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

public class MatrixHybridPredictor extends RotateConstructor {
    private static final float ROTATION_SPEED = 25.5F;
    private static final float LIMIT_ROTATION_SPEED = 44.5F;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private static final float[] LINEAR_WEIGHTS = {0.4f, 0.3f, 0.2f, 0.1f};
    private static final float[] QUADRATIC_WEIGHTS = {0.5f, 0.3f, 0.15f, 0.05f};
    
    private Vec3d[] positionHistory = new Vec3d[4];
    private int positionIndex = 0;
    private long lastUpdateTime = 0;

    public MatrixHybridPredictor() {
        super("MatrixHybridPredictor");
        for (int i = 0; i < positionHistory.length; i++) {
            positionHistory[i] = Vec3d.ZERO;
        }
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        
        Vec3d predictedPos = targetAngle.toVector();
        if (entity instanceof LivingEntity living) {
            predictedPos = predictHybridPosition(living, aura);
            targetAngle = MathAngle.fromVec3d(predictedPos.subtract(mc.player.getEyePos()));
        }
        
        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float length = (float) Math.hypot((double) yawDelta, (double) pitchDelta);
        
        float ANGLE_LIMIT_YAW = (float) Math.min((double) Math.abs(yawDelta), 74.0D + Math.random() * 1.032983422279358D);
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

    private Vec3d predictHybridPosition(LivingEntity target, Aura aura) {
        Vec3d currentPos = target.getPos();
        updatePositionHistory(currentPos);
        
        Vec3d linearPrediction = predictLinear(target);
        Vec3d quadraticPrediction = predictQuadratic(target);
        
        float blendFactor = calculateBlendFactor(target);
        Vec3d blendedPrediction = linearPrediction.multiply(blendFactor)
                .add(quadraticPrediction.multiply(1.0f - blendFactor));
        
        return blendedPrediction.add(0, target.getHeight() / 2, 0);
    }

    private void updatePositionHistory(Vec3d position) {
        positionHistory[positionIndex] = position;
        positionIndex = (positionIndex + 1) % positionHistory.length;
    }

    private Vec3d predictLinear(LivingEntity target) {
        Vec3d velocity = target.getVelocity();
        Vec3d smoothedVelocity = Vec3d.ZERO;
        
        for (int i = 0; i < positionHistory.length - 1; i++) {
            Vec3d diff = positionHistory[(positionIndex + i + 1) % positionHistory.length]
                    .subtract(positionHistory[(positionIndex + i) % positionHistory.length]);
            smoothedVelocity = smoothedVelocity.add(diff.multiply(LINEAR_WEIGHTS[i]));
        }
        
        float predictTicks = calculatePredictTicks(target);
        return target.getPos().add(smoothedVelocity.multiply(predictTicks));
    }

    private Vec3d predictQuadratic(LivingEntity target) {
        Vec3d pos0 = positionHistory[0];
        Vec3d pos1 = positionHistory[1];
        Vec3d pos2 = positionHistory[2];
        Vec3d pos3 = positionHistory[3];
        
        Vec3d vel1 = pos1.subtract(pos0);
        Vec3d vel2 = pos2.subtract(pos1);
        Vec3d vel3 = pos3.subtract(pos2);
        
        Vec3d acc1 = vel2.subtract(vel1);
        Vec3d acc2 = vel3.subtract(vel2);
        
        Vec3d smoothedAcc = acc1.multiply(QUADRATIC_WEIGHTS[0])
                .add(acc2.multiply(QUADRATIC_WEIGHTS[1]));
        
        float predictTicks = calculatePredictTicks(target);
        Vec3d velocity = target.getVelocity();
        
        return target.getPos()
                .add(velocity.multiply(predictTicks))
                .add(smoothedAcc.multiply(predictTicks * predictTicks * 0.5f));
    }

    private float calculateBlendFactor(LivingEntity target) {
        if (mc.player == null) return 0.5f;
        
        float distance = mc.player.distanceTo(target);
        float speed = (float) target.getVelocity().length();
        
        if (distance < 4 && speed < 0.3f) {
            return 0.7f;
        } else if (distance < 6 && speed < 0.5f) {
            return 0.6f;
        } else if (speed > 0.8f) {
            return 0.4f;
        }
        
        return 0.5f;
    }

    private float calculatePredictTicks(LivingEntity target) {
        if (mc.player == null) return 0;
        
        float distance = mc.player.distanceTo(target);
        float speed = (float) target.getVelocity().length();
        
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
            baseTicks += speed * 0.6f;
        }
        
        return MathHelper.clamp(baseTicks, 0.5f, 5.5f);
    }

    private float easeTowardsTarget(float value) {
        return value * (0.5F + 0.5F * value);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1D, 0.1D, 0.1D);
    }
}
