package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class MatrixAdvancedPredictor extends RotateConstructor {
    private static final float ROTATION_SPEED = 25.5F;
    private static final float LIMIT_ROTATION_SPEED = 44.5F;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final float[] VELOCITY_WEIGHTS = { 0.35f, 0.25f, 0.2f, 0.12f, 0.08f };
    private static final float[] ACCELERATION_WEIGHTS = { 0.4f, 0.3f, 0.2f, 0.1f };

    private Vec3d[] velocityHistory = new Vec3d[5];
    private Vec3d[] accelerationHistory = new Vec3d[4];
    private int velocityIndex = 0;
    private int accelerationIndex = 0;
    private Vec3d lastVelocity = Vec3d.ZERO;

    public MatrixAdvancedPredictor() {
        super("MatrixAdvancedPredictor");
        for (int i = 0; i < velocityHistory.length; i++) {
            velocityHistory[i] = Vec3d.ZERO;
        }
        for (int i = 0; i < accelerationHistory.length; i++) {
            accelerationHistory[i] = Vec3d.ZERO;
        }
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();

        Vec3d predictedPos = targetAngle.toVector();
        if (entity instanceof LivingEntity living) {
            predictedPos = predictAdvancedPosition(living, aura);
            targetAngle = MathAngle.fromVec3d(predictedPos.subtract(mc.player.getEyePos()));
        }

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float length = (float) Math.hypot((double) yawDelta, (double) pitchDelta);

        float ANGLE_LIMIT_YAW = (float) Math.min((double) Math.abs(yawDelta),
                74.0D + Math.random() * 1.032983422279358D);
        float ANGLE_LIMIT_PITCH = (float) Math.min((double) Math.abs(pitchDelta), 32.334D);

        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());

        if (length > 0.001F) {
            boolean limitReached = Math.abs(pitchDelta) >= ANGLE_LIMIT_PITCH;
            float maxStep = limitReached ? 44.5F : 25.5F;
            float step = Math.min(length, maxStep);
            float scale = step / length;
            if (!limitReached) {
                scale = easeTowardsTarget(scale);
            }

            float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchDelta * scale, -89.0F, 90.0F);
            moveAngle.setPitch(newPitch);
        }

        if (length > 0.001F) {
            boolean limitReached = Math.abs(yawDelta) >= ANGLE_LIMIT_YAW;
            float maxStep = limitReached ? 44.5F : 25.5F;
            float step = Math.min(length, maxStep);
            float scale = step / length;
            if (!limitReached) {
                scale = easeTowardsTarget(scale);
            }

            float newYaw = currentAngle.getYaw() + yawDelta * scale;
            moveAngle.setYaw(newYaw);
        }

        return moveAngle.adjustSensitivity();
    }

    private Vec3d predictAdvancedPosition(LivingEntity target, Aura aura) {
        Vec3d currentPos = target.getPos();
        Vec3d currentVelocity = target.getVelocity();

        updateVelocityHistory(currentVelocity);
        updateAccelerationHistory(currentVelocity);

        Vec3d smoothedVelocity = calculateWeightedVelocity();
        Vec3d smoothedAcceleration = calculateWeightedAcceleration();

        float predictTicks = calculateAdaptivePredictTicks(target, currentVelocity);

        Vec3d predictedPos = currentPos
                .add(smoothedVelocity.multiply(predictTicks))
                .add(smoothedAcceleration.multiply(predictTicks * predictTicks * 0.5f));

        return predictedPos.add(0, target.getHeight() / 2, 0);
    }

    private void updateVelocityHistory(Vec3d velocity) {
        velocityHistory[velocityIndex] = velocity;
        velocityIndex = (velocityIndex + 1) % velocityHistory.length;
    }

    private void updateAccelerationHistory(Vec3d currentVelocity) {
        Vec3d acceleration = currentVelocity.subtract(lastVelocity);
        accelerationHistory[accelerationIndex] = acceleration;
        accelerationIndex = (accelerationIndex + 1) % accelerationHistory.length;
        lastVelocity = currentVelocity;
    }

    private Vec3d calculateWeightedVelocity() {
        Vec3d weighted = Vec3d.ZERO;
        for (int i = 0; i < velocityHistory.length; i++) {
            weighted = weighted.add(velocityHistory[i].multiply(VELOCITY_WEIGHTS[i]));
        }
        return weighted;
    }

    private Vec3d calculateWeightedAcceleration() {
        Vec3d weighted = Vec3d.ZERO;
        for (int i = 0; i < accelerationHistory.length; i++) {
            weighted = weighted.add(accelerationHistory[i].multiply(ACCELERATION_WEIGHTS[i]));
        }
        return weighted;
    }

    private float calculateAdaptivePredictTicks(LivingEntity target, Vec3d velocity) {
        if (mc.player == null)
            return 0;

        float distance = mc.player.distanceTo(target);
        float speed = (float) velocity.length();

        float baseTicks = 2.0f;

        if (distance < 3) {
            baseTicks = 1.0f;
        } else if (distance < 5) {
            baseTicks = 2.0f;
        } else if (distance < 8) {
            baseTicks = 3.0f;
        } else {
            baseTicks = 4.0f;
        }

        if (speed > 0.3f) {
            baseTicks += speed * 0.8f;
        }

        if (target.isOnGround()) {
            baseTicks *= 0.9f;
        } else {
            baseTicks *= 1.1f;
        }

        return MathHelper.clamp(baseTicks, 0.5f, 6.0f);
    }

    private float easeTowardsTarget(float value) {
        return value * (0.5F + 0.5F * value);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1D, 0.1D, 0.1D);
    }
}
