package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class SlothAC extends RotateConstructor {
    public SlothAC() {
        super("SlothAC");
    }

    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float distance = 0.0F;
        if (entity != null && mc.player != null)
            distance = mc.player.distanceTo(entity);
        boolean canAttack = (entity != null && attackHandler.canAttack(aura.getConfig(), 0));
        boolean hasTarget = (aura.isState() && aura.getTarget() != null);
        float len = (float) Math.hypot(yawDelta, pitchDelta);
        if (len < 0.001F)
            return currentAngle;
        float normYaw = yawDelta / len;
        float normPitch = pitchDelta / len;
        float baseSpeed = canAttack ? 0.85F : 0.75F;
        if (distance > 0.0F && distance < 1.8F) {
            float nearMul = MathHelper.lerp(distance / 1.8F, 0.65F, 1.0F);
            baseSpeed *= nearMul;
        }
        if ((!hasTarget || entity == null) &&
                attackTimer.finished(900.0D))
            baseSpeed = 0.65F;
        float desiredStep = baseSpeed * len;
        desiredStep = MathHelper.clamp(desiredStep, 3.0F, 60.0F);
        float stepYaw = normYaw * desiredStep;
        float stepPitch = normPitch * desiredStep;
        float maxYawPerTick = 40.0F;
        float maxPitchPerTick = 30.0F;
        stepYaw = MathHelper.clamp(stepYaw, -maxYawPerTick, maxYawPerTick);
        stepPitch = MathHelper.clamp(stepPitch, -maxPitchPerTick, maxPitchPerTick);
        float jitterYaw = 0.0F;
        float jitterPitch = 0.0F;
        if (hasTarget) {
            long t = System.nanoTime();
            float distFactor = MathHelper.clamp(distance / 8.0F, 0.0F, 1.0F);
            float angleFactor = MathHelper.clamp(len / 140.0F, 0.0F, 1.0F);

            // Увеличиваем базовую амплитуду джитера для более заметного движения
            float baseAmpYaw = 1.2F + 2.4F * distFactor;
            float baseAmpPitch = 1.0F + 2.0F * distFactor;

            float nearAimFactor = 1.0F - MathHelper.clamp(len / 40.0F, 0.0F, 1.0F);
            float ampReduce = 0.3F + 0.7F * (1.0F - nearAimFactor);
            baseAmpYaw *= ampReduce;
            baseAmpPitch *= ampReduce;

            // Добавляем больше частот для более сложного движения
            float body = (float) Math.sin(t / 4000000.0D);
            float shoulder = (float) Math.sin((t + 2000000L) / 6000000.0D);
            float wrist = (float) Math.sin((t + 4000000L) / 8000000.0D);
            float fingers = (float) Math.sin((t + 6000000L) / 10000000.0D);
            float tremor = (float) Math.sin((t + 1000000L) / 2000000.0D);

            // Увеличиваем вклад быстрых колебаний
            float slow = body * 0.4F + shoulder * 0.25F + wrist * 0.2F + fingers * 0.15F;
            float micro = (float) Math.sin((t + 7300000L) / 2500000.0D) * 0.4F;
            float fast = (float) Math.sin((t + 3300000L) / 1500000.0D) * 0.3F;
            float ultraFast = tremor * 0.2F;

            float yawNoise = slow + micro + fast + ultraFast;
            float pitchNoise = slow * 0.9F + micro * 0.7F + fast * 0.5F + ultraFast * 0.3F;

            // Увеличиваем общий масштаб джитера
            float commonScale = 0.4F + 0.6F * (1.0F - angleFactor);
            if (!canAttack) {
                commonScale *= 1.5F; // Увеличиваем джитер когда не атакуем
            }

            jitterYaw = yawNoise * baseAmpYaw * commonScale;
            jitterPitch = pitchNoise * baseAmpPitch * commonScale;

            if (len < 15.0F) {
                float closeReduce = len / 15.0F;
                jitterYaw *= closeReduce;
                jitterPitch *= closeReduce;
            }
        }
        float newYaw = currentAngle.getYaw() + stepYaw + jitterYaw;
        float newPitch = currentAngle.getPitch() + stepPitch + jitterPitch;
        float yawDiff = MathHelper.wrapDegrees(newYaw - currentAngle.getYaw());
        float pitchDiff = newPitch - currentAngle.getPitch();
        float hardMaxYaw = 65.0F;
        float hardMaxPitch = 45.0F;
        yawDiff = MathHelper.clamp(yawDiff, -hardMaxYaw, hardMaxYaw);
        pitchDiff = MathHelper.clamp(pitchDiff, -hardMaxPitch, hardMaxPitch);
        newYaw = currentAngle.getYaw() + yawDiff;
        newPitch = currentAngle.getPitch() + pitchDiff;
        newPitch = MathHelper.clamp(newPitch, -89.0F, 89.0F);
        Turns result = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        result.setYaw(newYaw);
        result.setPitch(newPitch);
        return result;
    }

    public Vec3d randomValue() {
        // Увеличиваем рандомизацию для более разнообразных ударов по хитбоксу
        double randomX = (Math.random() - 0.5) * 0.12;  // Увеличиваем разброс по X
        double randomY = (Math.random() - 0.5) * 0.18;  // Увеличиваем разброс по Y
        double randomZ = (Math.random() - 0.5) * 0.12;  // Увеличиваем разброс по Z
        
        // Добавляем смещение для атаки в разные части тела
        double bodyOffset = Math.random() * 0.08;
        double headOffset = Math.random() * 0.06;
        
        // Рандомно выбираем приоритетную часть для атаки
        int targetZone = (int) (Math.random() * 4);
        switch (targetZone) {
            case 0: // Голова
                randomY += headOffset;
                break;
            case 1: // Тело
                randomY += bodyOffset * 0.3;
                break;
            case 2: // Ноги
                randomY -= bodyOffset * 0.5;
                break;
            case 3: // Руки
                randomX += (Math.random() - 0.5) * 0.15;
                break;
        }
        
        return new Vec3d(randomX, randomY, randomZ);
    }
}
