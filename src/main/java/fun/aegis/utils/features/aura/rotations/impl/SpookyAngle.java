package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.utils.features.aura.point.Vector;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.math.calc.Calculate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.Aegis;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.utils.MathAngle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import fun.aegis.utils.math.time.StopWatch;

import java.security.SecureRandom;

import static java.lang.Math.random;

public class SpookyAngle extends RotateConstructor {
    private static final float ROTATION_SPEED = 25.5F;
    private static final float LIMIT_ROTATION_SPEED = 44.5F;
    private static final float SHAKE_INTENSITY = 2.2F;
    private static final float SHAKE_SPEED = 0.32F;
    private static final float EPSILON = 1.0E-3F;
    private static final SecureRandom RANDOM = new SecureRandom();
    public SpookyAngle() {
        super("Spooky");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float length = (float) Math.hypot(yawDelta, pitchDelta);
        final float ANGLE_LIMIT_YAW = (float) Math.min(Math.abs(yawDelta), 74 + (random() * 1.0329834f));
        final float ANGLE_LIMIT_PITCH = (float) Math.min(Math.abs(pitchDelta), 32.334);
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());

        if (length > EPSILON) {
            boolean limitReached =  Math.abs(pitchDelta) >= ANGLE_LIMIT_PITCH;
            float maxStep = limitReached ? LIMIT_ROTATION_SPEED : ROTATION_SPEED;
            float step = Math.min(length, maxStep);
            float scale = step / length;
            if (!limitReached) {
                scale = easeTowardsTarget(scale);
            }

            float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchDelta * scale, -89.0F, 90.0F);
            moveAngle.setPitch(newPitch);
        }

        if (length > EPSILON) {
            boolean limitReached =  Math.abs(yawDelta) >= ANGLE_LIMIT_YAW;
            float maxStep = limitReached ? LIMIT_ROTATION_SPEED : ROTATION_SPEED;
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

    private void applyBodyMotion(Turns angle) {
        if (mc.player == null) return;

        float time = (System.currentTimeMillis() % 12000L) / 1200.0F;
        float swayPhase = time * SHAKE_SPEED * MathHelper.TAU;
        float swayYaw = MathHelper.sin(swayPhase) * SHAKE_INTENSITY;

        angle.setYaw(angle.getYaw() + swayYaw);
    }

    private float easeTowardsTarget(float value) {
        return value * (0.5F + 0.5F * value);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}
