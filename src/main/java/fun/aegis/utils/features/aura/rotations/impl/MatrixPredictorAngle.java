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

public class MatrixPredictorAngle extends RotateConstructor {
    private static final float ROTATION_SPEED = 25.5F;
    private static final float LIMIT_ROTATION_SPEED = 44.5F;
    private static final float SHAKE_INTENSITY = 2.2F;
    private static final float SHAKE_SPEED = 0.32F;
    private static final float EPSILON = 0.01F;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private static final float[] PREDICTOR_WEIGHTS = {0.3f, 0.25f, 0.2f, 0.15f, 0.1f};
    private Vec3d[] velocityHistory = new Vec3d[5];
    private int historyIndex = 0;

    public MatrixPredictorAngle() {
        super("MatrixPredictor");
        for (int i = 0; i < velocityHistory.length; i++) {
            velocityHistory[i] = Vec3d.ZERO;
        }
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        
        Vec3d predictedPos = targetAngle.toVector();
        if (entity instanceof LivingEntity living) {
            predictedPos = predictTargetPosition(living, aura);
            targetAngle = MathAngle.fromVec3d(predictedPos.subtract(mc.player.getEyePos()));
        }
        
        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float length = (float) Math.hypot((double) yawDelta, (double) pitchDelta);
        
        float ANGLE_LIMIT_YAW = (float) Math.min((double) Math.abs(yawDelta), 74.0D + Math.random() * 1.032983422279358D);
        float ANGLE_LIMIT_PITCH = (float) Math.min((double) Math.abs(pitchDelta), 32.334D);
        
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        
        if (length > EPSILON) {
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
        
        if (length > EPSILON) {
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

    private Vec3d predictTargetPosition(LivingEntity target, Aura aura) {
        Vec3d currentPos = target.getPos();
        Vec3d velocity = target.getVelocity();
        
        updateVelocityHistory(velocity);
        Vec3d smoothedVelocity = calculateSmoothedVelocity();
        
        float predictTicks = calculatePredictTicks(target);
        Vec3d predictedPos = currentPos.add(smoothedVelocity.multiply(predictTicks));
        
        return predictedPos.add(0, target.getHeight() / 2, 0);
    }

    private void updateVelocityHistory(Vec3d velocity) {
        velocityHistory[historyIndex] = velocity;
        historyIndex = (historyIndex + 1) % velocityHistory.length;
    }

    private Vec3d calculateSmoothedVelocity() {
        Vec3d weighted = Vec3d.ZERO;
        float totalWeight = 0;
        for (int i = 0; i < velocityHistory.length; i++) {
            weighted = weighted.add(velocityHistory[i].multiply(PREDICTOR_WEIGHTS[i]));
            totalWeight += PREDICTOR_WEIGHTS[i];
        }
        if (totalWeight > 0) {
            weighted = weighted.multiply(1.0 / totalWeight);
        }
        return weighted;
    }

    private float calculatePredictTicks(LivingEntity target) {
        if (mc.player == null) return 0;
        
        float distance = mc.player.distanceTo(target);
        float basePredict = 2.0f;
        
        if (distance < 3) {
            basePredict = 1.5f;
        } else if (distance < 5) {
            basePredict = 2.5f;
        } else if (distance < 8) {
            basePredict = 3.5f;
        }
        
        float speedFactor = (float) target.getVelocity().length();
        if (speedFactor > 0.5f) {
            basePredict += speedFactor * 0.5f;
        }
        
        return basePredict;
    }

    private float easeTowardsTarget(float value) {
        return value * (0.5F + 0.5F * value);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1D, 0.1D, 0.1D);
    }
}
