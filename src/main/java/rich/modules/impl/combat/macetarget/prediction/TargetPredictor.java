package rich.modules.impl.combat.macetarget.prediction;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;

@Getter
public class TargetPredictor {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Vec3d lastPosition = null;
    private Vec3d velocity = Vec3d.ZERO;
    private Vec3d smoothedVelocity = Vec3d.ZERO;
    private long lastUpdateTime = 0;
    private int sampleCount = 0;

    private static final int MIN_SAMPLES = 2;
    private static final double SMOOTHING = 0.6;

    public void update(LivingEntity target) {
        if (target == null) {
            reset();
            return;
        }

        Vec3d currentPos = target.getEntityPos();
        long currentTime = System.currentTimeMillis();

        if (lastPosition != null && lastUpdateTime > 0) {
            long deltaTime = currentTime - lastUpdateTime;
            if (deltaTime > 0 && deltaTime < 500) {
                velocity = currentPos.subtract(lastPosition);
                smoothedVelocity = smoothedVelocity.multiply(SMOOTHING).add(velocity.multiply(1 - SMOOTHING));
                sampleCount++;
            }
        }

        lastPosition = currentPos;
        lastUpdateTime = currentTime;
    }

    public Vec3d getPredictedPosition(LivingEntity target, Stage stage) {
        if (target == null || mc.player == null) {
            return Vec3d.ZERO;
        }

        Vec3d currentPos = target.getEntityPos();

        if (sampleCount < MIN_SAMPLES || smoothedVelocity.horizontalLengthSquared() < 0.0001) {
            return currentPos;
        }

        double distance = mc.player.distanceTo(target);
        double playerSpeed = mc.player.getVelocity().length();
        double targetSpeed = smoothedVelocity.horizontalLength();

        double ticksToReach;

        switch (stage) {
            case FLYING_UP -> {
                double heightDiff = Math.abs(mc.player.getY() - target.getY());
                ticksToReach = (heightDiff + distance) / Math.max(playerSpeed * 20, 1.0) * 1.2;
            }
            case TARGETTING -> {
                ticksToReach = distance / Math.max(playerSpeed * 20, 0.8);
            }
            case ATTACKING -> {
                ticksToReach = distance / Math.max(playerSpeed * 20, 1.5) * 0.8;
            }
            default -> {
                ticksToReach = distance / 2.0;
            }
        }

        ticksToReach = Math.min(ticksToReach, 40);

        double leadMultiplier = 1.0;
        if (targetSpeed > 0.2) {
            leadMultiplier = 1.3;
        }
        if (targetSpeed > 0.4) {
            leadMultiplier = 1.5;
        }

        Vec3d prediction = smoothedVelocity.multiply(ticksToReach * leadMultiplier);

        return currentPos.add(prediction);
    }

    public boolean isMoving() {
        return smoothedVelocity.horizontalLengthSquared() > 0.001;
    }

    public void reset() {
        lastPosition = null;
        velocity = Vec3d.ZERO;
        smoothedVelocity = Vec3d.ZERO;
        lastUpdateTime = 0;
        sampleCount = 0;
    }
}