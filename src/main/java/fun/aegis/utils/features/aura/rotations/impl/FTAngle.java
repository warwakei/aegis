package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class FTAngle extends RotateConstructor {
    private float tick = 0;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // FunTime специфичные переменные
    private long lastLookUpTime = 0;
    private long nextLookUpDelay = 6500 + new SecureRandom().nextInt(700);
    private boolean isLookingUp = false;
    private long lookUpStartTime = 0;
    private int lookUpDuration = 0;

    public FTAngle() {
        super("FunTime");
    }

    @Override
    public Turns limitAngleChange(Turns currentTurns, Turns targetTurns, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        long currentTime = System.currentTimeMillis();

        // Логика "взгляда вверх" как в UFuntime
        if (!isLookingUp && currentTime - lastLookUpTime >= nextLookUpDelay) {
            isLookingUp = true;
            lookUpStartTime = currentTime;
            lookUpDuration = 270 + secureRandom.nextInt(120);
            lastLookUpTime = currentTime;
            nextLookUpDelay = 6500 + secureRandom.nextInt(700);
        }

        boolean fastspeed = false;
        if (isLookingUp && currentTime - lookUpStartTime >= lookUpDuration) {
            isLookingUp = false;
        }
        if (currentTime - lookUpStartTime >= lookUpDuration + 40L) {
            fastspeed = true;
        }

        float baseYaw = currentTurns.getYaw();

        if (canAttack) {
            tick = randomLerp(6, 7);
        }

        float yawChangeSpeed = randomLerp(22, 29);
        float randomAttackShift = 0;
        float waveA = (float) Math.cos(currentTime / 40D);
        float waveB = (float) Math.sin(currentTime / 70D);

        if (tick > 0) {
            yawChangeSpeed = randomLerp(50, 90);
            baseYaw = targetTurns.getYaw(); // Используем targetTurns
            randomAttackShift = (waveA + waveB) * randomLerp(1, 2);
            tick--;
        }

        float yawJitter = waveA * randomLerp(13, 15) + randomAttackShift;
        float pitchJitter = waveB * randomLerp(5, 7) + randomAttackShift;

        float finalPitch = isLookingUp ? -randomLerp(80, 90) : targetTurns.getPitch();

        Turns newRotation = new Turns(baseYaw + yawJitter, finalPitch + pitchJitter);

        float pitchSpeed = isLookingUp ? randomLerp(120, 170) : fastspeed ? randomLerp(120, 170) : randomLerp(25, 35);

        return smoothRotation(currentTurns, newRotation, yawChangeSpeed, pitchSpeed);
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

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(secureRandom.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.06D, 0.06D, 0.5D);
    }
}
