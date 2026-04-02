package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.InputEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class TargetStrafe extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public SelectSetting mode = (new SelectSetting("Режим", "Режим страфа"))
            .value(new String[]{"Matrix", "Grim", "Custom", "Vanilla"}).selected("Matrix");

    SelectSetting type = (new SelectSetting("Тип", "Тип страфа для Grim"))
            .value(new String[]{"Куб", "Центр", "Круг"}).selected("Куб").visible(() -> this.mode.isSelected("Grim"));

    SelectSetting typeMatrix = (new SelectSetting("Тип Matrix/Custom", "Тип страфа для Matrix/Custom"))
            .value(new String[]{"Куб", "Круг"}).selected("Круг")
            .visible(() -> this.mode.isSelected("Matrix") || this.mode.isSelected("Custom"));

    SliderSettings grimRadius = (new SliderSettings("Радиус Grim", "Радиус для Grim"))
            .setValue(0.87F).range(0.1F, 1.5F).visible(() -> this.mode.isSelected("Grim") && (this.type.isSelected("Куб") || this.type.isSelected("Круг")));

    MultiSelectSetting setting = (new MultiSelectSetting("Настройки", "Дополнительные настройки"))
            .value(new String[]{"Авто прыжок", "Только при нажатии клавиш", "Перед целью", "Режим направления", "Коррекция движения", "Передвижение"}).selected(new String[]{"Авто прыжок"});

    SelectSetting movementCorrectionMode = (new SelectSetting("Режим коррекции", "Режим коррекции движения"))
            .value(new String[]{"Стандартный", "Инвертированный"}).selected("Стандартный")
            .visible(() -> this.setting.isSelected("Коррекция движения"));

    BooleanSetting movementCorrectionOnlyForward = (new BooleanSetting("Только вперед", "Корректировать только при движении вперед"))
            .setValue(false)
            .visible(() -> this.setting.isSelected("Коррекция движения"));

    SelectSetting directionMode = (new SelectSetting("Направление", "Направление обхода цели"))
            .value(new String[]{"По часовой", "Против часовой", "Случайное"}).selected("По часовой")
            .visible(() -> this.setting.isSelected("Режим направления"));

    SliderSettings radius = (new SliderSettings("Радиус", "Радиус обхода"))
            .setValue(2.5F).range(0.1F, 7.0F).visible(() -> this.mode.isSelected("Matrix") || this.mode.isSelected("Custom") || this.mode.isSelected("Vanilla"));

    SliderSettings speed = (new SliderSettings("Скорость", "Скорость страфа"))
            .setValue(0.3F).range(0.1F, 1.0F).visible(() -> this.mode.isSelected("Matrix") || this.mode.isSelected("Custom"));

    BooleanSetting customAffectSpeed = (new BooleanSetting("Кастомная скорость", "Использовать кастомную скорость"))
            .setValue(true).visible(() -> this.mode.isSelected("Custom"));

    BooleanSetting hitRunWay = (new BooleanSetting("Режим Hit&Run", "Режим удар-бег"))
            .setValue(false);

    SliderSettings hitRunDistance = (new SliderSettings("Дистанция Hit&Run", "Дистанция для Hit&Run"))
            .setValue(4.0F).range(1.0F, 10.0F).visible(() -> this.hitRunWay.isValue());

    SliderSettings peragonDistance = (new SliderSettings("Дистанция перогона", "Дистанция передвижения"))
            .setValue(0.5F).range(0.1F, 3.0F)
            .visible(() -> this.setting.isSelected("Передвижение"));

    SelectSetting peragonMode = (new SelectSetting("Режим перогона", "Режим умного передвижения"))
            .value(new String[]{"Авто", "По клавише"}).selected("Авто")
            .visible(() -> this.setting.isSelected("Передвижение"));

    BindSetting peragonBind = (new BindSetting("Клавиша перогона", "Клавиша для перогона"))
            .setKey(0)
            .visible(() -> this.setting.isSelected("Передвижение") && this.peragonMode.isSelected("По клавише"));

    private int grimPointIndex = 0;
    private boolean peragonActive = false;
    private long peragonEndMs = 0L;

    public TargetStrafe() {
        super("TargetStrafe", "TargetStrafe", ModuleCategory.MOVEMENT);
        setup(new Setting[]{
                this.mode, this.type, this.typeMatrix, this.grimRadius, this.radius, this.speed,
                this.customAffectSpeed, this.hitRunWay, this.hitRunDistance, this.setting,
                this.directionMode, this.movementCorrectionMode, this.movementCorrectionOnlyForward,
                this.peragonDistance, this.peragonMode, this.peragonBind
        });
    }

    public static TargetStrafe getInstance() {
        return (TargetStrafe) Instance.get(TargetStrafe.class);
    }

    private boolean hasElytra() {
        if (mc.player == null || mc.world == null)
            return true;
        return mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA);
    }

    private boolean isPeragonActive() {
        if (!this.setting.isSelected("Передвижение"))
            return false;
        if (this.peragonMode.isSelected("По клавише")) {
            if (mc.getWindow() == null)
                return false;
            int key = this.peragonBind.getKey();
            if (key <= 0)
                return false;
            boolean pressed = (GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == 1);
            this.peragonActive = pressed;
            if (pressed)
                this.peragonEndMs = System.currentTimeMillis() + 100L;
            return this.peragonActive;
        }
        long now = System.currentTimeMillis();
        if (this.peragonActive && now > this.peragonEndMs)
            this.peragonActive = false;
        return this.peragonActive;
    }

    private void startSmartPeragon(LivingEntity target) {
        if (!this.setting.isSelected("Передвижение"))
            return;
        if (!this.peragonMode.isSelected("Авто"))
            return;
        if (isPeragonActive())
            return;
        double dist = mc.player.getPos().distanceTo(target.getPos());
        if (dist < 2.0D)
            return;
        Vec3d targetPos = target.getPos();
        Vec3d playerPos = mc.player.getPos();
        float yaw = target.bodyYaw;
        float yawRad = yaw * 0.017453292F;
        Vec3d look = new Vec3d(-MathHelper.sin(yawRad), 0.0D, MathHelper.cos(yawRad));
        Vec3d toPlayer = playerPos.subtract(targetPos).normalize();
        double dot = look.normalize().dotProduct(toPlayer);
        boolean backToMe = (dot < 0.0D);
        if (!backToMe)
            return;
        this.peragonActive = true;
        this.peragonEndMs = System.currentTimeMillis() + 400L;
    }

    private Vec3d getPeragonPoint(LivingEntity target) {
        if (!isPeragonActive())
            return null;
        float yaw = target.bodyYaw;
        float yawRad = yaw * 0.017453292F;
        Vec3d look = new Vec3d(-MathHelper.sin(yawRad), 0.0D, MathHelper.cos(yawRad));
        double dist = this.peragonDistance.getValue();
        return target.getPos().add(look.normalize().multiply(dist));
    }

    @EventHandler
    public void onInput(InputEvent event) {
        Vec3d nextPoint;
        if (mc.player == null || mc.world == null)
            return;
        if (hasElytra())
            return;
        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || !target.isAlive())
            return;
        if (!this.mode.isSelected("Grim") && !this.mode.isSelected("Vanilla"))
            return;
        if (this.setting.isSelected("Коррекция движения") && this.movementCorrectionOnlyForward.isValue() && !mc.options.forwardKey.isPressed())
            return;
        if (this.setting.isSelected("Только при нажатии клавиш") &&
                !mc.options.forwardKey.isPressed() &&
                !mc.options.backKey.isPressed() &&
                !mc.options.leftKey.isPressed() &&
                !mc.options.rightKey.isPressed())
            return;
        if (this.setting.isSelected("Передвижение") && this.peragonMode.isSelected("Авто"))
            startSmartPeragon(target);
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d peragonPoint = getPeragonPoint(target);
        if (peragonPoint != null) {
            nextPoint = new Vec3d(peragonPoint.x, playerPos.y, peragonPoint.z);
        } else {
            int directionMultiplier = 1;
            if (this.directionMode.isSelected("Против часовой")) {
                directionMultiplier = -1;
            } else if (this.directionMode.isSelected("Случайное")) {
                long time = System.currentTimeMillis() / 3000L;
                directionMultiplier = (time % 2L == 0L) ? 1 : -1;
            }
            if (this.mode.isSelected("Grim")) {
                double r = applyHitRunRadius(this.grimRadius.getValue());
                if (this.setting.isSelected("Перед целью")) {
                    float targetYaw = target.bodyYaw;
                    if (this.type.isSelected("Центр")) {
                        nextPoint = targetPos.add(
                                -Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier, 0.0D,
                                Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier);
                    } else {
                        double offset = Math.cos(System.currentTimeMillis() / 500.0D) * r * directionMultiplier;
                        nextPoint = targetPos.add(
                                -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset, 0.0D,
                                Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset);
                    }
                } else if (this.type.isSelected("Куб")) {
                    Vec3d[] points = {
                            new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                            new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                            new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                            new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
                    };
                    if (playerPos.distanceTo(points[this.grimPointIndex]) < 0.5D)
                        this.grimPointIndex = (this.grimPointIndex + directionMultiplier + points.length) % points.length;
                    nextPoint = points[this.grimPointIndex];
                } else if (this.type.isSelected("Круг")) {
                    double baseAngle = (System.currentTimeMillis() % 3600L) / 3600.0D * 4.0D * Math.PI;
                    double angle = (directionMultiplier > 0) ? baseAngle : (6.283185307179586D - baseAngle);
                    nextPoint = new Vec3d(targetPos.x + Math.cos(angle) * r, playerPos.y, targetPos.z + Math.sin(angle) * r);
                } else {
                    nextPoint = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
                }
                if (this.setting.isSelected("Коррекция движения") && !isPeragonActive()) {
                    double sign = this.movementCorrectionMode.isSelected("Стандартный") ? 1.0D : -1.0D;
                    double yawRad = Math.toRadians(target.bodyYaw);
                    double x = targetPos.x - Math.sin(yawRad) * r * sign;
                    double z = targetPos.z + Math.cos(yawRad) * r * sign;
                    nextPoint = new Vec3d(x, playerPos.y, z);
                }
            } else {
                double r = applyHitRunRadius(this.radius.getValue());
                if (this.setting.isSelected("Коррекция движения") && !isPeragonActive()) {
                    double sign = this.movementCorrectionMode.isSelected("Стандартный") ? 1.0D : -1.0D;
                    double yawRad = Math.toRadians(target.bodyYaw);
                    double x = targetPos.x - Math.sin(yawRad) * r * sign;
                    double z = targetPos.z + Math.cos(yawRad) * r * sign;
                    nextPoint = new Vec3d(x, playerPos.y, z);
                } else {
                    double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
                    double step = 0.8D;
                    angle += directionMultiplier * step / Math.max(playerPos.distanceTo(targetPos), r);
                    double x = targetPos.x + r * Math.cos(angle);
                    double z = targetPos.z + r * Math.sin(angle);
                    nextPoint = new Vec3d(x, playerPos.y, z);
                }
            }
        }
        Vec3d direction = nextPoint.subtract(playerPos).normalize();
        float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
        boolean forward = false, back = false, left = false, right = false;
        if (angleDiff >= -22.5D && angleDiff < 22.5D) {
            forward = true;
        } else if (angleDiff >= 22.5D && angleDiff < 67.5D) {
            forward = true;
            right = true;
        } else if (angleDiff >= 67.5D && angleDiff < 112.5D) {
            right = true;
        } else if (angleDiff >= 112.5D && angleDiff < 157.5D) {
            back = true;
            right = true;
        } else if (angleDiff >= -67.5D && angleDiff < -22.5D) {
            forward = true;
            left = true;
        } else if (angleDiff >= -112.5D && angleDiff < -67.5D) {
            left = true;
        } else if (angleDiff >= -157.5D && angleDiff < -112.5D) {
            back = true;
            left = true;
        } else {
            back = true;
        }
        event.setDirectional(forward, back, left, right);
        if (this.setting.isSelected("Авто прыжок") && mc.player.isOnGround())
            event.setJumping(true);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        double motionSpeed;
        if (mc.player == null || mc.world == null)
            return;
        if (hasElytra())
            return;
        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || !target.isAlive())
            return;
        if (!this.mode.isSelected("Matrix") && !this.mode.isSelected("Custom"))
            return;
        if (this.setting.isSelected("Коррекция движения") && this.movementCorrectionOnlyForward.isValue() && !mc.options.forwardKey.isPressed())
            return;
        if (this.setting.isSelected("Передвижение") && this.peragonMode.isSelected("Авто"))
            startSmartPeragon(target);
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = applyHitRunRadius(this.radius.getValue());
        if (this.setting.isSelected("Только при нажатии клавиш") &&
                !mc.options.forwardKey.isPressed() &&
                !mc.options.backKey.isPressed() &&
                !mc.options.leftKey.isPressed() &&
                !mc.options.rightKey.isPressed())
            return;
        if (this.setting.isSelected("Авто прыжок") && mc.player.isOnGround())
            mc.player.jump();
        int directionMultiplier = 1;
        if (this.directionMode.isSelected("Против часовой")) {
            directionMultiplier = -1;
        } else if (this.directionMode.isSelected("Случайное")) {
            long time = System.currentTimeMillis() / 3000L;
            directionMultiplier = (time % 2L == 0L) ? 1 : -1;
        }
        if (this.mode.isSelected("Matrix")) {
            motionSpeed = this.speed.getValue();
        } else if (this.customAffectSpeed.isValue()) {
            motionSpeed = this.speed.getValue();
        } else {
            double vx = mc.player.getVelocity().x;
            double vz = mc.player.getVelocity().z;
            motionSpeed = Math.hypot(vx, vz);
        }
        Vec3d peragonPoint = getPeragonPoint(target);
        if (peragonPoint != null) {
            Vec3d pp = new Vec3d(peragonPoint.x, playerPos.y, peragonPoint.z);
            float yaw = (float) Math.toDegrees(Math.atan2(pp.z - playerPos.z, pp.x - playerPos.x)) - 90.0F;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
            return;
        }
        if (this.setting.isSelected("Коррекция движения") && !isPeragonActive()) {
            double sign = this.movementCorrectionMode.isSelected("Стандартный") ? 1.0D : -1.0D;
            double yawRad = Math.toRadians(target.bodyYaw);
            double x = targetPos.x - Math.sin(yawRad) * r * sign;
            double z = targetPos.z + Math.cos(yawRad) * r * sign;
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
            return;
        }
        if (this.setting.isSelected("Перед целью")) {
            float targetYaw = target.bodyYaw;
            double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier;
            double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier;
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
            return;
        }
        if (this.typeMatrix.isSelected("Куб")) {
            Vec3d[] points = {
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
            };
            if (playerPos.distanceTo(points[this.grimPointIndex]) < 0.5D)
                this.grimPointIndex = (this.grimPointIndex + directionMultiplier + points.length) % points.length;
            Vec3d nextPoint = points[this.grimPointIndex];
            Vec3d dirVec = nextPoint.subtract(playerPos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(dirVec.z, dirVec.x)) - 90.0F;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
        } else if (this.typeMatrix.isSelected("Круг")) {
            double step, angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
            if (this.mode.isSelected("Matrix") || this.customAffectSpeed.isValue()) {
                step = this.speed.getValue();
            } else {
                step = motionSpeed;
            }
            angle += directionMultiplier * step / Math.max(playerPos.distanceTo(targetPos), r);
            double x = targetPos.x + r * Math.cos(angle);
            double z = targetPos.z + r * Math.sin(angle);
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
        }
    }

    public void activate() {
        super.activate();
        this.grimPointIndex = 0;
        this.peragonActive = false;
        this.peragonEndMs = 0L;
    }

    private double applyHitRunRadius(double baseRadius) {
        if (!this.hitRunWay.isValue() || mc.player == null)
            return baseRadius;
        float cooldown = mc.player.getAttackCooldownProgress(0.0F);
        if (cooldown >= 1.0F)
            return baseRadius;
        return Math.max(baseRadius, this.hitRunDistance.getValue());
    }
}
