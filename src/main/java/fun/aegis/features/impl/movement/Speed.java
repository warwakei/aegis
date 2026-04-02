package fun.aegis.features.impl.movement;

import fun.aegis.events.player.TickEvent;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.interactions.simulate.Simulations;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Speed extends Module {

    public static Speed getInstance() {
        return Instance.get(Speed.class);
    }

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим скорости")
            .value("Grim", "Grim V2", "Grim V2X", "Entity", "Normal")
            .selected("Grim");

    SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения")
            .range(1.0f, 20.0f)
            .setValue(8.0f);

    SliderSettings range = new SliderSettings("Дальность", "Дальность поиска ближайшей цели")
            .range(0.5f, 10.0f)
            .setValue(3.0f)
            .visible(() -> mode.isSelected("Entity"));

    SliderSettings expand = new SliderSettings("Расширение", "Расширение хитбокса для проверки коллизий")
            .range(0.1f, 2.0f)
            .setValue(0.5f);

    BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", "Учитывать только игроков")
            .setValue(true);

    BooleanSetting requireMoving = new BooleanSetting("Требуется движение", "Работает только при движении")
            .setValue(true);

    BooleanSetting auraBoost = new BooleanSetting("Усиление Aura", "Увеличивает дистанцию ускорения до цели в Aura")
            .setValue(true)
            .visible(() -> mode.getSelected().startsWith("Grim"));

    SliderSettings auraStrength = new SliderSettings("Сила усиления", "Фактор умножения до цели")
            .range(1.0f, 6.0f)
            .setValue(1.5f)
            .visible(() -> mode.getSelected().startsWith("Grim") && auraBoost.isValue());

    private int collisionCounter = 0;
    private long lastCollisionTime = 0;
    private double staticChaosX = 0.5; 
    public static double currentGrimBoost = 0;

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(mode, speed, range, expand, onlyPlayers, requireMoving, auraBoost, auraStrength);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null)
            return;

        if (requireMoving.isValue() && !Simulations.hasPlayerMovement())
            return;

        switch (mode.getSelected()) {
            case "Grim" -> handleGrimMode();
            case "Grim V2" -> handleGrimV2Mode();
            case "Grim V2X" -> handleGrimV2XMode();
            case "Entity" -> handleEntityMode();
            case "Normal" -> handleNormalMode();
        }
    }

    private void handleGrimMode() {
        int collisions = calculateCollisions();
        if (collisions <= 0)
            return;

        updateCollisionCounter();

        double phi = 1.618033988749895;
        double seed = (System.currentTimeMillis() % 1000) / 1000.0;
        double chaoticFactor = (seed * phi) % 0.02;

        if (mc.player.hurtTime > 0) {
            collisionCounter = 0;
            return;
        }

        double motion = calculateMotion(chaoticFactor);
        motion = Math.min(motion, 0.08) * collisions;

        float moveYaw = calculateOrbitalYaw(3.5f, 2.3f, 55.0f, 1.6f);

        applyBoost(moveYaw, motion);
    }

    private void handleGrimV2Mode() {
        int collisions = calculateCollisions();
        if (collisions <= 0)
            return;

        updateCollisionCounter();

        // Aperiodic
        double phi = 1.618033988749895;
        double seed = (System.currentTimeMillis() % 10000) / 10000.0;
        double phiFactor = (seed * phi) % 0.025;

        if (mc.player.hurtTime > 0) {
            collisionCounter = 0;
            return;
        }

        double motion;
        if (collisionCounter <= 1)
            motion = 0.022 + phiFactor;
        else if (collisionCounter <= 3)
            motion = 0.045 + phiFactor;
        else if (collisionCounter <= 5)
            motion = 0.055 + phiFactor * 1.5;
        else
            motion = 0.075 + phiFactor * 0.8;

        motion = Math.min(motion, 0.085) * collisions;

        
        float moveYaw = calculateOrbitalYaw(3.8f, 2.4f, 60.0f, 1.8f);

        applyBoost(moveYaw, motion);
    }

    private void handleGrimV2XMode() {
        int collisions = calculateCollisions();
        if (collisions <= 0)
            return;

        updateCollisionCounter();

        if (mc.player.hurtTime > 0) {
            collisionCounter = 0;
            return;
        }

        // Теория Хаоса: Логистическое отображение (r = 3.99 для макс хаоса)
        double r = 3.99;
        staticChaosX = r * staticChaosX * (1 - staticChaosX);
        double chaosFactor = staticChaosX * 0.035;

        double motion;
        if (collisionCounter <= 1)
            motion = 0.02 + chaosFactor;
        else if (collisionCounter <= 3)
            motion = 0.042 + chaosFactor;
        else
            motion = 0.072 + chaosFactor * 0.6;

        motion = Math.min(motion, 0.09) * collisions;

        // V2X Orbital: "Тянущая" орбита с динамическим радиусом
        float moveYaw = calculateOrbitalYaw(4.0f, 2.2f + (float) staticChaosX * 0.4f, 65.0f, 2.0f);

        applyBoost(moveYaw, motion);
    }

    private int calculateCollisions() {
        int collisions = 0;
        float boxExpand = expand.getValue();

        if (auraBoost.isValue() && Aura.getInstance().isState() && Aura.getInstance().getTarget() != null) {
            if (Aura.getInstance().getTarget().isSprinting() && mc.player.isSprinting()) {
                boxExpand = auraStrength.getValue();
            }
        }

        Box expandedBox = mc.player.getBoundingBox().expand(boxExpand);

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player || ent instanceof ArmorStandEntity)
                continue;
            if (onlyPlayers.isValue() && !(ent instanceof PlayerEntity))
                continue;

            if ((ent instanceof LivingEntity || ent instanceof BoatEntity)
                    && expandedBox.intersects(ent.getBoundingBox())) {
                collisions++;
            }
        }
        return collisions;
    }

    private void updateCollisionCounter() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCollisionTime > 500)
            collisionCounter = 0;
        lastCollisionTime = currentTime;
        collisionCounter++;
    }

    private double calculateMotion(double chaoticFactor) {
        if (collisionCounter <= 1)
            return 0.02 + chaoticFactor;
        if (collisionCounter <= 3)
            return 0.04 + chaoticFactor * 0.8;
        if (collisionCounter <= 5)
            return 0.05 + chaoticFactor * 1.2;
        return 0.07 + chaoticFactor * 0.5;
    }

    private float calculateOrbitalYaw(float catchRange, float targetDist, float force, float jumpFactor) {
        float moveYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        if (Aura.getInstance().isState() && Aura.getInstance().getTarget() != null) {
            LivingEntity target = Aura.getInstance().getTarget();
            double distance = mc.player.distanceTo(target);

            if (distance < catchRange) {
                Vec3d targetPos = target.getPos();
                Vec3d playerPos = mc.player.getPos();
                double dx = playerPos.x - targetPos.x;
                double dz = playerPos.z - targetPos.z;
                float yawToPlayer = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

                float orbitYaw = yawToPlayer + 90.0F;
                float finalJumpFactor = mc.player.isOnGround() ? 1.0f : jumpFactor;
                float radiusCorrection = (float) MathHelper.clamp((targetDist - distance) * force * finalJumpFactor,
                        -70.0f, 70.0f);
                moveYaw = orbitYaw + radiusCorrection;
            }
        }
        return moveYaw;
    }

    private void applyBoost(float moveYaw, double motion) {
        currentGrimBoost = motion;
        double rad = Math.toRadians(moveYaw + 90.0f);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        mc.player.addVelocity(cos * motion, 0, sin * motion);
    }

    private void handleEntityMode() {
        int collisions = 0;
        Box expandedBox = mc.player.getBoundingBox().expand(expand.getValue());

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player)
                continue;
            if (onlyPlayers.isValue() && !(ent instanceof PlayerEntity))
                continue;

            if ((ent instanceof LivingEntity || ent instanceof BoatEntity)
                    && expandedBox.intersects(ent.getBoundingBox())) {
                collisions++;
            }
        }

        double finalSpeed = speed.getValue() * 0.01 * collisions;
        if (finalSpeed <= 0.0)
            return;

        // Поиск ближайшей цели
        Entity nearest = null;
        double bestSq = Double.MAX_VALUE;
        double maxRangeSq = range.getValue() * range.getValue();

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player)
                continue;
            if (onlyPlayers.isValue() && !(ent instanceof PlayerEntity))
                continue;
            if (!(ent instanceof LivingEntity) && !(ent instanceof BoatEntity))
                continue;

            double dx = ent.getX() - mc.player.getX();
            double dz = ent.getZ() - mc.player.getZ();
            double sq = dx * dx + dz * dz;

            if (sq <= maxRangeSq && sq < bestSq) {
                bestSq = sq;
                nearest = ent;
            }
        }

        if (nearest != null) {
            double[] dir = getDirectionToPoint(mc.player.getPos(), nearest.getPos(), finalSpeed);
            mc.player.addVelocity(dir[0], 0.0, dir[1]);
        }
    }

    private void handleNormalMode() {
        Simulations.setVelocity(speed.getValue() / 3);
    }

    private double[] getDirectionToPoint(Vec3d from, Vec3d to, double spd) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len == 0)
            return new double[] { 0.0, 0.0 };
        return new double[] { dx / len * spd, dz / len * spd };
    }
}
