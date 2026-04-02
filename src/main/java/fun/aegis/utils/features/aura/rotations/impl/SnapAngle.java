package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class SnapAngle extends RotateConstructor {
    private float tick = 0;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private long lastSnapTime = 0;
    private final SecureRandom secureRandom = new SecureRandom();
    private Vec3d lastTargetVelocity = Vec3d.ZERO;
    private long reactionDelayEnd = 0;

    public SnapAngle() {
        super("Snap");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d targetPos = entity != null ? entity.getPos() : vec3d;

        if (targetPos == null) {
            return currentAngle;
        }

        // Предсказание движения цели
        Turns predictedAngle = targetAngle;
        if (entity instanceof LivingEntity livingEntity) {
            Vec3d targetVelocity = livingEntity.getVelocity();
            lastTargetVelocity = targetVelocity;
            
            // Предсказываем позицию на 2-3 тика вперёд
            float predictTicks = 2.5f;
            Vec3d predictedPos = targetPos.add(targetVelocity.multiply(predictTicks));
            predictedAngle = new Turns(
                targetAngle.getYaw() + (float)(targetVelocity.x * 5),
                targetAngle.getPitch() + (float)(targetVelocity.y * 3)
            );
        }

        // Легитные смещения для имитации живого игрока
        float addyVacY = 0.4F * (float) Math.cos(System.currentTimeMillis() / 1500D);
        float addyVacZ = 0.25F * (float) Math.cos(System.currentTimeMillis() / 700D);
        float addyVacX = 0.36F * (float) Math.cos(System.currentTimeMillis() / 900D);

        Vec3d vec = targetPos.add(addyVacX,
                MathHelper.clamp((float)(playerEyePos.y - targetPos.y), 0.0F, 0.8F) - addyVacY,
                addyVacZ)
                .subtract(playerEyePos).normalize();

        // Базовая скорость вращения
        float baseYawSpeed = 35.0f;
        float basePitchSpeed = 25.0f;

        boolean attackF = false;
        if (canAttack) {
            tick = 4;
            // Человеческая задержка реакции (50-150ms)
            if (reactionDelayEnd == 0) {
                reactionDelayEnd = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(50, 150);
            }
        }
        
        if (tick > 0) {
            attackF = true;
            tick--;
        }

        // Вычисляем разницу углов
        float yawDiff = MathHelper.wrapDegrees(predictedAngle.getYaw() - currentAngle.getYaw());
        float pitchDiff = MathHelper.clamp(predictedAngle.getPitch() - currentAngle.getPitch(), -90F, 90F);
        float angleDifference = (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff));

        // При атаке - более быстрый snap с плавным ускорением
        if (attackF && System.currentTimeMillis() >= reactionDelayEnd) {
            // Ease-in-out для плавного ускорения
            float progress = 1.0f - (tick / 4.0f);
            float easeProgress = progress < 0.5f ? 2 * progress * progress : -1 + (4 - 2 * progress) * progress;
            
            baseYawSpeed = 45.0f + (easeProgress * 30.0f) + ThreadLocalRandom.current().nextFloat(-3, 3);
            basePitchSpeed = 35.0f + (easeProgress * 20.0f) + ThreadLocalRandom.current().nextFloat(-3, 3);
        } else if (attackF) {
            // Во время задержки реакции - медленное движение
            baseYawSpeed = 15.0f;
            basePitchSpeed = 10.0f;
        } else {
            // Адаптивная скорость в зависимости от расстояния до цели
            if (angleDifference > 30) {
                baseYawSpeed = 40.0f;
                basePitchSpeed = 30.0f;
            } else if (angleDifference > 15) {
                baseYawSpeed = 32.0f;
                basePitchSpeed = 24.0f;
            } else {
                baseYawSpeed = 25.0f;
                basePitchSpeed = 18.0f;
            }
        }

        // Микро-движения для имитации дрожания руки
        float microMovementYaw = (float) (Math.sin(System.currentTimeMillis() / 120D) * 0.3f + Math.cos(System.currentTimeMillis() / 180D) * 0.2f);
        float microMovementPitch = (float) (Math.cos(System.currentTimeMillis() / 150D) * 0.2f + Math.sin(System.currentTimeMillis() / 200D) * 0.15f);

        Turns result = smoothRotation(currentAngle, predictedAngle, baseYawSpeed, basePitchSpeed, microMovementYaw, microMovementPitch);
        
        lastYaw = result.getYaw();
        lastPitch = result.getPitch();
        lastSnapTime = System.currentTimeMillis();
        
        return result;
    }

    private Turns smoothRotation(Turns current, Turns target, float yawSpeed, float pitchSpeed, float microYaw, float microPitch) {
        float yawDiff = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();

        // Адаптивный множитель скорости в зависимости от расстояния
        float distanceFactor = Math.min(1.0F, Math.max(0.2F, (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff)) / 60.0F));
        
        float adaptiveYawSpeed = yawSpeed * distanceFactor;
        float adaptivePitchSpeed = pitchSpeed * distanceFactor;

        float yawStep = Math.min(Math.abs(yawDiff), adaptiveYawSpeed);
        float pitchStep = Math.min(Math.abs(pitchDiff), adaptivePitchSpeed);

        float yawChange = MathHelper.clamp(yawDiff, -yawStep, yawStep);
        float pitchChange = MathHelper.clamp(pitchDiff, -pitchStep, pitchStep);

        return new Turns(
                current.getYaw() + yawChange + microYaw,
                MathHelper.clamp(current.getPitch() + pitchChange + microPitch, -90F, 90F)
        );
    }

    private int randomInt(int min, int max) {
        return min + secureRandom.nextInt(max - min + 1);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.36D, 0.4D, 0.25D);
    }
}

