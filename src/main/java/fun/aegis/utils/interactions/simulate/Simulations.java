package fun.aegis.utils.interactions.simulate;

import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.impl.misc.SelfDestruct;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

import java.util.Objects;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class Simulations implements QuickImports {

    public boolean hasPlayerMovement() {
        return mc.player.input.movementForward != 0f || mc.player.input.movementSideways != 0f;
    }

    public double[] calculateDirection(double distance) {
        return calculateDirection(mc.player.input.movementForward, mc.player.input.movementSideways, distance);
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
    public static double[] forward(final double d) {
        float f = mc.player.input.movementForward;
        float f2 = mc.player.input.movementSideways;
        float f3 = TurnsConnection.INSTANCE.getRotation().getYaw();
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

    public static float calculateBodyYaw(
            float yaw,
            float prevBodyYaw,
            double prevX,
            double prevZ,
            double currentX,
            double currentZ,
            float handSwingProgress
    ) {

        if (Aura.fakeRotate && Aura.getInstance().getTarget() != null) {
            yaw = TurnsConnection.INSTANCE.getFakeAngle().getYaw();
        } else {
            yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        }

        double motionX = currentX - prevX;
        double motionZ = currentZ - prevZ;
        float motionSquared = (float)(motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;
        float swing = mc.player.handSwingProgress;

        if (motionSquared > 0.0025000002F) {
            float movementYaw = (float)MathHelper.atan2(motionZ, motionX) * (180F / (float)Math.PI) - 90.0F;
            float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(yaw) - movementYaw);
            if (95.0F < yawDiff && yawDiff < 265.0F) {
                bodyYaw = movementYaw - 180.0F;
            } else {
                bodyYaw = movementYaw;
            }
        }

        if (mc.player != null && mc.player.handSwingProgress - 0.2F > 0F && !Aura.getInstance().getAimMode().isSelected("SlothAC")) {
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

    public static double kizdamati() {
        return 1488;
    }

    public static String fpsADDS() {
        if (SelfDestruct.unhooked) {
            return "fabric";
        } else {
            return "lunarclient:v2.21.5-2540";
        }
    }

    public double[] calculateDirection(float forward, float sideways, double distance) {
        float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
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

    public double getSpeedSqrt(Entity entity) {
        return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.prevX, entity.prevY, entity.prevZ)));
    }

    public void setVelocity(double velocity) {
        final double[] direction = Simulations.calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
    }

    public void setVelocity(double velocity, double y) {
        final double[] direction = Simulations.calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], y, direction[1]);
    }

    public double getDegreesRelativeToView(
            Vec3d positionRelativeToPlayer,
            float yaw) {

        float optimalYaw =
                (float) Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(wrapDegrees(yaw));

        return Math.toDegrees(wrapDegrees((optimalYaw - currentYaw)));
    }

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
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

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
        return getDirectionalInputForDegrees(input, dgs, 20.0F);
    }
}
