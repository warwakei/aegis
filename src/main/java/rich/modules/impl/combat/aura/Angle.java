package rich.modules.impl.combat.aura;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.util.math.MathUtils;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

@Getter
@Setter
@ToString
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Angle {
    public static Angle DEFAULT = new Angle(0, 0);
    float yaw, pitch;


    public static Angle fromTargetHead(Vec3d playerPos, Vec3d targetPos, double targetHeight) {
        double headY = targetPos.y + targetHeight * 0.9;

        double deltaX = targetPos.x - playerPos.x;
        double deltaY = headY - (playerPos.y + 1.5);
        double deltaZ = targetPos.z - playerPos.z;

        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        yaw = wrapDegrees(yaw);

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float pitch = (float) Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

        return new Angle(yaw, pitch);
    }

    public Angle adjustSensitivity() {
        double gcd = MathUtils.computeGcd();

        Angle previousAngle = AngleConnection.INSTANCE.getServerAngle();

        float adjustedYaw = adjustAxis(yaw, previousAngle.yaw, gcd);
        float adjustedPitch = adjustAxis(pitch, previousAngle.pitch, gcd);

        return new Angle(adjustedYaw, MathHelper.clamp(adjustedPitch, -90f, 90f));
    }

    public Angle random(float f) {
        return new Angle(yaw + MathUtils.getRandom(-f, f), pitch + MathUtils.getRandom(-f, f));
    }

    private float adjustAxis(float axisValue, float previousValue, double gcd) {
        float delta = axisValue - previousValue;
        return previousValue + Math.round(delta / gcd) * (float) gcd;
    }

    public final Vec3d toVector() {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public Angle addYaw(float yaw) {
        return new Angle(this.yaw + yaw, this.pitch);
    }

    public Angle addPitch(float pitch) {
        this.pitch = MathHelper.clamp(this.pitch + pitch, -90, 90);
        return this;
    }

    public Angle of(Angle angle) {
        return new Angle(angle.getYaw(), angle.getPitch());
    }

    @ToString
    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class VecRotation {
        final Angle angle;
        final Vec3d vec;
    }
}