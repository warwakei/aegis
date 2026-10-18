package rich.util.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class MovementUtil {

    public static Vec3d applySmoothVelocity(Vec3d currentVelocity, Vec3d targetVelocity, float smoothFactor) {
        return new Vec3d(
                lerp(currentVelocity.x, targetVelocity.x, smoothFactor),
                lerp(currentVelocity.y, targetVelocity.y, smoothFactor),
                lerp(currentVelocity.z, targetVelocity.z, smoothFactor)
        );
    }

    public static double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }
}