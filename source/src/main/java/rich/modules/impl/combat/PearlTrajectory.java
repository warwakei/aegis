package rich.modules.impl.combat;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.BiFunction;
import java.util.function.Predicate;

final class PearlTrajectory {
    private static final double GRAVITY = 0.03;
    private static final double AIR_DRAG = 0.99f;
    private static final double WATER_DRAG = 0.8f;
    private static final double SPEED = 1.5;
    private static final int MAX_TICKS = 120;

    record Landing(Vec3d position, BlockPos block) {}
    record Aim(float yaw, float pitch) {}

    private PearlTrajectory() {}

    static Landing predict(Vec3d position, Vec3d velocity, Predicate<Vec3d> water,
                           BiFunction<Vec3d, Vec3d, Landing> collision) {
        for (int i = 0; i < MAX_TICKS; i++) {
            velocity = velocity.add(0, -GRAVITY, 0).multiply(water.test(position) ? WATER_DRAG : AIR_DRAG);
            Vec3d next = position.add(velocity);
            Landing landing = collision.apply(position, next);
            if (landing != null) return landing;
            position = next;
        }
        return null;
    }

    static Aim solve(Vec3d origin, Vec3d inherited, Landing target, Predicate<Vec3d> water,
                     BiFunction<Vec3d, Vec3d, Landing> collision) {
        // Aim slightly into the collision shape so rounding cannot stop the ray on its surface.
        Vec3d displacement = target.position.lerp(Vec3d.ofCenter(target.block), 0.10).subtract(origin);
        double velocityScale = 1, gravityVelocity = 0, distanceScale = 0, drop = 0;
        double previousScale = 0, previousDrop = 0, previousError = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            velocityScale *= AIR_DRAG;
            gravityVelocity = (gravityVelocity - GRAVITY) * AIR_DRAG;
            distanceScale += velocityScale;
            drop += gravityVelocity;
            Vec3d required = requiredVelocity(displacement, inherited, distanceScale, drop);
            double error = required.length() - SPEED;
            if (Math.signum(error) != Math.signum(previousError) || Math.abs(error) < 0.002) {
                double lower = 0, upper = 1;
                boolean lowerPositive = previousError > 0;
                for (int i = 0; i < 14; i++) {
                    double fraction = (lower + upper) / 2;
                    required = requiredVelocity(displacement, inherited,
                            previousScale + (distanceScale - previousScale) * fraction,
                            previousDrop + (drop - previousDrop) * fraction);
                    if ((required.length() > SPEED) == lowerPositive) lower = fraction;
                    else upper = fraction;
                }
                Vec3d launch = required.normalize().multiply(SPEED);
                Landing landed = predict(origin, launch.add(inherited), water, collision);
                if (landed != null && landed.position.squaredDistanceTo(target.position) <= 0.81) {
                    float yaw = (float) Math.toDegrees(Math.atan2(-launch.x, launch.z));
                    float pitch = (float) -Math.toDegrees(Math.atan2(launch.y, launch.horizontalLength()));
                    return new Aim(yaw, pitch);
                }
            }
            previousScale = distanceScale;
            previousDrop = drop;
            previousError = error;
        }
        return null;
    }

    private static Vec3d requiredVelocity(Vec3d displacement, Vec3d inherited, double scale, double drop) {
        return displacement.add(0, -drop, 0).multiply(1 / scale).subtract(inherited);
    }
}
