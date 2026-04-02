package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import java.security.SecureRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MatrixAngle extends RotateConstructor {
    private static final float ROTATION_SPEED = 25.5F;
    private static final float LIMIT_ROTATION_SPEED = 44.5F;
    private static final float SHAKE_INTENSITY = 2.2F;
    private static final float SHAKE_SPEED = 0.32F;
    private static final float EPSILON = 0.001F;
    private static final SecureRandom RANDOM = new SecureRandom();

    public MatrixAngle() {
        super("Matrix");
    }

    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        if (entity instanceof net.minecraft.entity.LivingEntity target) {
            Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);

            // Синергия с Grim Speed
            if (fun.aegis.features.impl.movement.Speed.currentGrimBoost > 0) {
                Vec3d velocity = target.getVelocity();
                double boostFactor = fun.aegis.features.impl.movement.Speed.currentGrimBoost * 4.0;
                targetPos = targetPos.add(velocity.multiply(boostFactor));
            }

            targetAngle = MathAngle.calculateAngle(targetPos);
        }

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float length = (float) Math.hypot((double) yawDelta, (double) pitchDelta);
        float ANGLE_LIMIT_YAW = (float) Math.min((double) Math.abs(yawDelta),
                74.0D + Math.random() * 1.032983422279358D);
        float ANGLE_LIMIT_PITCH = (float) Math.min((double) Math.abs(pitchDelta), 32.334D);
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        boolean limitReached;
        float maxStep;
        float step;
        float scale;
        float newYaw;
        if (length > 0.001F) {
            limitReached = Math.abs(pitchDelta) >= ANGLE_LIMIT_PITCH;
            maxStep = limitReached ? 44.5F : 25.5F;
            step = Math.min(length, maxStep);
            scale = step / length;
            if (!limitReached) {
                scale = this.easeTowardsTarget(scale);
            }

            newYaw = MathHelper.clamp(currentAngle.getPitch() + pitchDelta * scale, -89.0F, 90.0F);
            moveAngle.setPitch(newYaw);
        }

        if (length > 0.001F) {
            limitReached = Math.abs(yawDelta) >= ANGLE_LIMIT_YAW;
            maxStep = limitReached ? 44.5F : 25.5F;
            step = Math.min(length, maxStep);
            scale = step / length;
            if (!limitReached) {
                scale = this.easeTowardsTarget(scale);
            }

            newYaw = currentAngle.getYaw() + yawDelta * scale;
            moveAngle.setYaw(newYaw);
        }

        return moveAngle.adjustSensitivity();
    }

    private void applyBodyMotion(Turns angle) {
        if (mc.player != null) {
            float time = (float) (System.currentTimeMillis() % 12000L) / 1200.0F;
            float swayPhase = time * 0.32F * 6.2831855F;
            float swayYaw = MathHelper.sin(swayPhase) * 2.2F;
            angle.setYaw(angle.getYaw() + swayYaw);
        }
    }

    private float easeTowardsTarget(float value) {
        return value * (0.5F + 0.5F * value);
    }

    public Vec3d randomValue() {
        return new Vec3d(0.1D, 0.1D, 0.1D);
    }
}
