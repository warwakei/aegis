package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.RotationUpdateEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.target.TargetFinder;
import rich.modules.impl.render.Particles;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.math.MathUtils;
import rich.util.math.TaskPriority;
import rich.util.repository.friend.FriendUtils;
import rich.util.Instance;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ProjectileHelper extends ModuleStructure {

    public static ProjectileHelper getInstance() {
        return Instance.get(ProjectileHelper.class);
    }

    private static final double ARROW_GRAVITY = 0.05;
    private static final double ARROW_DRAG = 0.99;
    private static final double MAX_SIMULATION_TICKS = 40;
    private static final double HITBOX_EXPAND = 0.18;

    private final SliderSettings searchDistance = new SliderSettings("Дистанция поиска", "Радиус поиска цели вокруг игрока")
            .setValue(16).range(5F, 64F);

    private final SliderSettings fov = new SliderSettings("FOV", "Угол поиска цели вокруг прицела")
            .setValue(35).range(3F, 110F);

    private final BooleanSetting autoShoot = new BooleanSetting("Авто-выстрел", "Выстреливает, когда лук полностью заряжен и аим завершен")
            .setValue(false);

    private final MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует цели по типу")
            .value("Players", "Mobs", "Animals", "Armor Stand")
            .selected("Players", "Mobs", "Animals");

    private final TargetFinder targetFinder = new TargetFinder();
    private LivingEntity currentTarget;
    private Angle lastSolution;

    public ProjectileHelper() {
        super("ProjectileHelper", "Projectile Helper", ModuleCategory.COMBAT);
        settings(searchDistance, fov, autoShoot, targetType);
    }

    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public LivingEntity getTarget(World world, Iterable<Entity> entities) {
        List<Entity> entityList = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());

        List<LivingEntity> validTargets = entityList.stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(this::isValidTarget)
                .filter(this::hasLineOfSight)
                .filter(this::isWithinFov)
                .collect(Collectors.toList());

        LivingEntity nearestTarget = null;
        double nearestAngle = Double.MAX_VALUE;

        for (LivingEntity target : validTargets) {
            double angleDistance = getCrosshairAngleDistance(target);
            if (angleDistance < nearestAngle) {
                nearestAngle = angleDistance;
                nearestTarget = target;
            }
        }

        currentTarget = nearestTarget;
        return currentTarget;
    }

    private double getCrosshairAngleDistance(LivingEntity target) {
        Vec3d from = getShooterPos();
        Vec3d to = getAimPoint(target);
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        float desiredYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float desiredPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        float yawDelta = Math.abs(MathHelper.wrapDegrees(desiredYaw - mc.player.getYaw()));
        float pitchDelta = Math.abs(desiredPitch - mc.player.getPitch());

        return Math.hypot(yawDelta, pitchDelta);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null) return false;
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;

        if (!targetType.isSelected("Players") && entity instanceof PlayerEntity) return false;
        if (!targetType.isSelected("Mobs") && entity instanceof MobEntity) return false;
        if (!targetType.isSelected("Animals") && entity instanceof AnimalEntity) return false;
        if (!targetType.isSelected("Armor Stand") && entity instanceof ArmorStandEntity) return false;
        return true;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public Vec3d getPredictedPosition(LivingEntity target, Vec3d shooterPos, float projectileSpeed, float gravity) {
        Vec3d targetPos = target.getEntityPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d targetVelocity = target.getVelocity();
        Vec3d delta = targetPos.subtract(shooterPos);

        double a = projectileSpeed * projectileSpeed - targetVelocity.lengthSquared();
        double b = -2 * delta.dotProduct(targetVelocity);
        double c = -delta.lengthSquared();

        double t;
        double discriminant = b * b - 4 * a * c;
        if (discriminant > 0) {
            double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
            double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);
            t = Math.max(t1, t2);
        } else {
            t = delta.length() / projectileSpeed;
        }

        Vec3d predicted = targetPos.add(targetVelocity.multiply(t));
        predicted = predicted.add(0, 0.5 * gravity * t * t, 0);

        return predicted;
    }

    private float getBowSpeed(ItemStack stack) {
        if (!(stack.getItem() instanceof BowItem)) {
            return 3.0f;
        }

        return Math.max(0.1f, BowItem.getPullProgress(mc.player.getItemUseTime()) * 3.0f);
    }

    private boolean isHoldingProjectile() {
        ItemStack main = mc.player.getMainHandStack();
        return main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem || main.getItem() instanceof TridentItem;
    }

    private Vec3d getShooterPos() {
        return mc.player.getCameraPosVec(1.0f);
    }

    private Vec3d getAimPoint(LivingEntity entity) {
        return entity.getEntityPos().add(0, entity.getHeight() * 0.5, 0);
    }

    private double getYawDeltaTo(LivingEntity entity) {
        Vec3d from = getShooterPos();
        Vec3d to = getAimPoint(entity);
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        float desiredYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        return Math.abs(MathHelper.wrapDegrees(desiredYaw - mc.player.getYaw()));
    }

    private boolean isWithinFov(LivingEntity entity) {
        return getYawDeltaTo(entity) <= fov.getValue();
    }

    private boolean hasLineOfSight(LivingEntity entity) {
        Vec3d from = getShooterPos();
        Vec3d to = getAimPoint(entity);
        HitResult hit = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || (hit instanceof BlockHitResult bhr && bhr.getPos().squaredDistanceTo(to) < 0.25);
    }

    private boolean isAimedAtTarget(LivingEntity entity) {
        if (lastSolution == null) return false;
        double yawDelta = Math.abs(MathHelper.wrapDegrees(lastSolution.getYaw() - mc.player.getYaw()));
        double pitchDelta = Math.abs(MathHelper.wrapDegrees(lastSolution.getPitch() - mc.player.getPitch()));
        return yawDelta <= 1.2 && pitchDelta <= 1.2;
    }

    private boolean isBowFullyDrawn(ItemStack stack) {
        return mc.player.getActiveItem() == stack && mc.player.getItemUseTime() >= 20;
    }

    private void tryAutoShoot(ItemStack stack) {
        if (!autoShoot.isValue()) return;
        if (!(stack.getItem() instanceof BowItem) || !isBowFullyDrawn(stack)) return;
        if (currentTarget == null || !currentTarget.isAlive()) return;
        if (!hasLineOfSight(currentTarget) || !isWithinFov(currentTarget) || !isAimedAtTarget(currentTarget)) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() == HitResult.Type.MISS) return;
        if (mc.player.getItemUseTime() <= 0) return;
        mc.player.stopUsingItem();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != EventType.PRE) return;

        ItemStack stack = mc.player.getMainHandStack();

        if (!isValidWeaponState(stack)) {
            currentTarget = null;
            lastSolution = null;
            return;
        }

        updateTarget();

        if (currentTarget != null) {
            performAim();
            tryAutoShoot(stack);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isValidWeaponState(ItemStack stack) {
        boolean holdingBow = stack.getItem() instanceof BowItem;
        boolean holdingCrossbow = stack.getItem() instanceof CrossbowItem && ((CrossbowItem) stack.getItem()).isCharged(stack);
        boolean holdingTrident = stack.getItem() instanceof TridentItem;

        if (!holdingBow && !holdingCrossbow && !holdingTrident) {
            return false;
        }

        if (holdingBow && mc.player.getActiveItem() != stack) {
            return false;
        }

        return true;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void updateTarget() {
        if (currentTarget != null && !currentTarget.isAlive()) {
            currentTarget = null;
        }

        if (currentTarget == null) {
            currentTarget = getTarget(mc.world, mc.world.getEntities());
            if (currentTarget == mc.player) currentTarget = null;
        }

        if (FriendUtils.isFriend(currentTarget)) currentTarget = null;
        if (currentTarget == null) {
            lastSolution = null;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performAim() {
        ItemStack stack = mc.player.getMainHandStack();
        Vec3d shooterPos = getShooterPos().add(mc.player.getVelocity().multiply(0.35));
        Angle solution = solveBallisticAngle(currentTarget, shooterPos, getBowSpeed(stack));

        if (solution == null) {
            lastSolution = null;
            return;
        }

        float yaw = solution.getYaw() + (float) MathUtils.getRandom(-0.099, 0.099);
        float pitch = solution.getPitch() + (float) MathUtils.getRandom(-0.066, 0.066);

        float yawDiff = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        float pitchDiff = MathHelper.clamp(pitch - mc.player.getPitch(), -55f, 55f);
        yaw = mc.player.getYaw() + yawDiff * 0.81f;
        pitch = mc.player.getPitch() + pitchDiff * 0.81f;
        lastSolution = new Angle(solution.getYaw(), solution.getPitch());

        AngleConnection.INSTANCE.rotateTo(
                new Angle(yaw, pitch),
                AngleConfig.DEFAULT,
                TaskPriority.HIGH_IMPORTANCE_1,
                this
        );
    }

    private Angle solveBallisticAngle(LivingEntity target, Vec3d shooterPos, double speed) {
        if (speed <= 0.1) return null;

        Vec3d targetVelocity = target.getVelocity();
        Angle bestAngle = null;
        double bestMiss = Double.MAX_VALUE;

        float baseYaw = (float) Math.toDegrees(Math.atan2(
                (getAimPoint(target).z - shooterPos.z),
                (getAimPoint(target).x - shooterPos.x))) - 90f;

        for (int ticks = 2; ticks <= MAX_SIMULATION_TICKS; ticks++) {
            Vec3d predicted = getAimPoint(target).add(targetVelocity.multiply(ticks));
            Vec3d delta = predicted.subtract(shooterPos);
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (horizontal < 0.001) continue;

            float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
            float pitch = solvePitchBySimulation(shooterPos, predicted, speed, ticks);
            if (Float.isNaN(pitch)) continue;

            double miss = simulateArrowMiss(shooterPos, yaw, pitch, speed, target, ticks);
            if (miss < bestMiss) {
                bestMiss = miss;
                bestAngle = new Angle(yaw, pitch);
                if (miss <= 0.01) break;
            }
        }

        return bestMiss <= 0.75 ? bestAngle : null;
    }

    private float solvePitchBySimulation(Vec3d shooterPos, Vec3d targetPos, double speed, int maxTicks) {
        float low = -89f;
        float high = 89f;
        float bestPitch = Float.NaN;
        double bestMiss = Double.MAX_VALUE;

        for (int i = 0; i < 14; i++) {
            float mid = (low + high) * 0.5f;
            double miss = simulateArrowMiss(shooterPos, targetPos, speed, mid, maxTicks);
            if (miss < bestMiss) {
                bestMiss = miss;
                bestPitch = mid;
            }

            float upPitch = mid + 1.5f;
            double upMiss = simulateArrowMiss(shooterPos, targetPos, speed, upPitch, maxTicks);

            if (upMiss < miss) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return bestMiss <= 0.4 ? bestPitch : Float.NaN;
    }

    private double simulateArrowMiss(Vec3d shooterPos, Vec3d targetPos, double speed, float pitch, int maxTicks) {
        float yaw = (float) Math.toDegrees(Math.atan2(targetPos.z - shooterPos.z, targetPos.x - shooterPos.x)) - 90f;
        return simulateArrowMiss(shooterPos, yaw, pitch, speed, new Box(targetPos.x, targetPos.y, targetPos.z, targetPos.x, targetPos.y, targetPos.z), maxTicks);
    }

    private double simulateArrowMiss(Vec3d shooterPos, float yaw, float pitch, double speed, LivingEntity target, int maxTicks) {
        return simulateArrowMiss(shooterPos, yaw, pitch, speed, predictedBox(target, maxTicks), maxTicks);
    }

    private double simulateArrowMiss(Vec3d shooterPos, float yaw, float pitch, double speed, Box targetBox, int maxTicks) {
        Vec3d position = shooterPos;
        Vec3d velocity = angleToVelocity(yaw, pitch).multiply(speed).add(mc.player.getVelocity());
        double closest = Double.MAX_VALUE;

        for (int tick = 0; tick <= maxTicks; tick++) {
            Box box = targetBox.expand(HITBOX_EXPAND);
            if (box.contains(position)) return 0;

            Vec3d nextPosition = position.add(velocity);
            if (box.raycast(position, nextPosition).isPresent()) return 0;

            closest = Math.min(closest, distanceToBox(nextPosition, box));
            position = nextPosition;
            velocity = velocity.multiply(ARROW_DRAG).add(0, -ARROW_GRAVITY, 0);
        }

        return closest;
    }

    private Vec3d angleToVelocity(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3d(x, y, z).normalize();
    }

    private Box predictedBox(LivingEntity target, int ticks) {
        return target.getBoundingBox().offset(target.getVelocity().multiply(ticks));
    }

    private double distanceToBox(Vec3d point, Box box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
