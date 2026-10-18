package rich.util.move;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;
import rich.modules.impl.combat.aura.AngleConnection;

import java.util.Objects;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class MoveUtil implements IMinecraft {

    public static boolean hasPlayerMovement() {
        Input input = mc.player.input;
        if (input.hasForwardMovement()) {
            return true;
        }
        Vec2f vec = input.getMovementInput();
        return vec.x != 0f || vec.y != 0f;
    }

    public static double getDistanceToGround() {
        for (double y = mc.player.getY(); y > 0; y -= 0.1) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().down((int) ((mc.player.getY() - y) + 1))).isAir()) {
                return mc.player.getY() - y;
            }
        }
        return 256;
    }

    public static double getDegreesRelativeToView(
            Vec3d positionRelativeToPlayer,
            float yaw) {

        float optimalYaw =
                (float) Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(wrapDegrees(yaw));

        return Math.toDegrees(wrapDegrees((optimalYaw - currentYaw)));
    }

    public static void setVelocity(double velocity) {
        final double[] direction = calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
    }

    public static double[] forward(final double d) {
        Vec2f movement = mc.player.input.getMovementInput();
        float f = movement.y;
        float f2 = movement.x;
        float f3 = AngleConnection.INSTANCE.getRotation().getYaw();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }

    public static double[] calculateDirection(double distance) {
        Vec2f movement = mc.player.input.getMovementInput();
        float forward = movement.y;
        float sideways = movement.x;
        return calculateDirection(forward, sideways, distance);
    }

    public static double[] calculateDirection(float forward, float sideways, double distance) {
        float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += (forward > 0.0f) ? -45 : 45;
            } else if (sideways < 0.0f) {
                yaw += (forward > 0.0f) ? 45 : -45;
            }
            sideways = 0.0f;
            forward = (forward > 0.0f) ? 1.0f : -1.0f;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }

    public static final boolean moveKeyPressed(int keyNumber) {
        boolean w = mc.options.forwardKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();
        return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
    }

    public static final boolean w() {
        return moveKeyPressed(0);
    }

    public static final boolean a() {
        return moveKeyPressed(1);
    }

    public static final boolean s() {
        return moveKeyPressed(2);
    }

    public static final boolean d() {
        return moveKeyPressed(3);
    }

    public static final float moveYaw(float entityYaw) {
        return entityYaw + (float)(!a() || !d() || w() && s() || !w() && !s() ? (w() && s() && (!a() || !d()) && (a() || d()) ? (a() ? -90 : (d() ? 90 : 0)) : (a() && d() && (!w() || !s()) || w() && s() && (!a() || !d()) ? 0 : (!a() && !d() && !s() ? 0 : (w() && !s() ? 45 : (s() && !w() ? (!a() && !d() ? 180 : 135) : ((w() || s()) && (!w() || !s()) ? 0 : 90))) * (a() ? -1 : 1)))) : (w() ? 0 : (s() ? 180 : 0)));
    }

    public static float calculateBodyYaw(float yaw, float prevBodyYaw, double prevX, double prevZ, double currentX, double currentZ, float handSwingProgress) {
        double motionX = currentX - prevX;
        double motionZ = currentZ - prevZ;
        float motionSquared = (float)(motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;
        float swing = mc.player.handSwingProgress;

        if (motionSquared > 0.0025000002F) {
            float movementYaw = (float) MathHelper.atan2(motionZ, motionX) * (180F / (float)Math.PI) - 90.0F;
            float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(yaw) - movementYaw);
            if (95.0F < yawDiff && yawDiff < 265.0F) {
                bodyYaw = movementYaw - 180.0F;
            } else {
                bodyYaw = movementYaw;
            }
        }

        if (mc.player != null && mc.player.handSwingProgress - 0.2F > 0F) {
            bodyYaw = yaw;
        }

        float deltaYaw = MathHelper.wrapDegrees(bodyYaw - prevBodyYaw);
        bodyYaw = prevBodyYaw + deltaYaw * 0.3F;

        float yawOffsetDiff = MathHelper.wrapDegrees(yaw - bodyYaw);
        float maxHeadRotation = 52.0F;
        if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
            bodyYaw += yawOffsetDiff - (float)MathHelper.sign((double)yawOffsetDiff) * maxHeadRotation;
        }

        return bodyYaw;
    }

    public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
        boolean forwards = input.forward();
        boolean backwards = input.backward();
        boolean left = input.left();
        boolean right = input.right();

        if (dgs >= (-90.0F + deadAngle) && dgs <= (90.0F - deadAngle)) {
            forwards = true;
        } else if (dgs < (-90.0F - deadAngle) || dgs > (90.0F + deadAngle)) {
            backwards = true;
        }

        if (dgs >= (0.0F + deadAngle) && dgs <= (180.0F - deadAngle)) {
            right = true;
        } else if (dgs >= (-180.0F + deadAngle) && dgs <= (0.0F - deadAngle)) {
            left = true;
        }

        return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
        return getDirectionalInputForDegrees(input, dgs, 20.0F);
    }
}
