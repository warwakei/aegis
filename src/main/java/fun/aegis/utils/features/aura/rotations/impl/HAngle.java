package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HAngle extends RotateConstructor {
    private static long tickCounter = 0;
    private float resetProgress = 0.0f;
    private final SecureRandom secureRandom = new SecureRandom();
    private boolean jitterApplied = false;

    public HAngle() {
        super("HvH");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();

        if (entity instanceof net.minecraft.entity.LivingEntity target) {
            // Целимся в грудь/голову для максимальной легитности
            Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.75, 0);
            targetAngle = MathAngle.calculateAngle(targetPos);
        }

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        // Динамическая скорость вращения
        float speedYaw = 40.0f + secureRandom.nextFloat() * 20.0f;
        float speedPitch = 25.0f + secureRandom.nextFloat() * 15.0f;

        float moveYaw = MathHelper.clamp(yawDelta, -speedYaw, speedYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -speedPitch, speedPitch);

        float finalYaw = currentAngle.getYaw() + moveYaw;
        float finalPitch = currentAngle.getPitch() + movePitch;

        // Микро-джиттер для имитации живой руки
        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
        if (rotationDifference > 0.5f) {
            finalYaw += (secureRandom.nextFloat() - 0.5f) * 0.12f;
            finalPitch += (secureRandom.nextFloat() - 0.5f) * 0.12f;
        }

        Turns result = new Turns(finalYaw, MathHelper.clamp(finalPitch, -90F, 90F));
        return result.adjustSensitivity();
    }

    private float applyGaussianJitter(float rotation, float strength) {
        return rotation + (float) (secureRandom.nextGaussian() * strength);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}
