package rich.modules.impl.combat.aura.impl;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;


public class LinearConstructor extends RotateConstructor {
    public LinearConstructor() {
        super("Linear");
    }
    public static final LinearConstructor INSTANCE = new LinearConstructor();

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);

        float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
        float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;

        float newYaw = currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw);
        float newPitch = currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch);

        return new Angle(newYaw, newPitch);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}
