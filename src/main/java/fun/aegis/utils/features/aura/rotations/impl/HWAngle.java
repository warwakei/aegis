package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.math.time.TimerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HWAngle extends RotateConstructor {
    private float tick = 0;
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil timer2 = new TimerUtil();
    private final SecureRandom secureRandom = new SecureRandom();

    public HWAngle() {
        super("HolyWorld");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 10);

        float time = System.currentTimeMillis();

        boolean attackF = false;
        if (canAttack) tick = 2;
        if (tick > 0) {
            attackF = true;
            tick--;
        }

        // Плавные синусоидальные колебания
        float randomX = (float) (Math.sin(time / 80D) * 2) + (float) (Math.cos(time / 50D) * 4);
        float randomY = (float) (Math.sin(time / 120D) * 3) + (float) (Math.cos(time / 60D) * 2);

        // При атаке - усиленная рандомизация
        if (attackF) {
            randomX += (float) (Math.sin(time / 30D) * 2) * (float) (Math.cos(time / 40D) * 3);
            randomY += (float) (Math.sin(time / 35D) * 2) * (float) (Math.cos(time / 45D) * 2);
        }

        // Используем targetAngle как базу
        Turns newRotation = new Turns(
                targetAngle.getYaw() + randomX, 
                MathHelper.clamp(targetAngle.getPitch() + randomY, -90F, 90F)
        );

        float yawSpeed = attackF ? randomWithUpdate(50, 70, 35, timer) : randomWithUpdate(30, 40, 35, timer);
        float pitchSpeed = attackF ? randomWithUpdate(40, 60, 100, timer2) : randomWithUpdate(20, 30, 100, timer2);
        
        return smoothRotation(currentAngle, newRotation, yawSpeed, pitchSpeed);
    }

    private Turns smoothRotation(Turns current, Turns target, float yawSpeed, float pitchSpeed) {
        float yawDiff = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();

        float yawStep = Math.min(Math.abs(yawDiff), yawSpeed);
        float pitchStep = Math.min(Math.abs(pitchDiff), pitchSpeed);

        float yawChange = MathHelper.clamp(yawDiff, -yawStep, yawStep);
        float pitchChange = MathHelper.clamp(pitchDiff, -pitchStep, pitchStep);

        return new Turns(
                current.getYaw() + yawChange,
                MathHelper.clamp(current.getPitch() + pitchChange, -90F, 90F)
        );
    }

    private float randomWithUpdate(float min, float max, long updateInterval, TimerUtil timer) {
        if (timer.hasTimeElapsed(updateInterval)) {
            timer.resetCounter();
        }
        return MathHelper.lerp(secureRandom.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.16D, 0.2D, 0.15D);
    }
}
