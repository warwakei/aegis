package rich.modules.impl.combat.aura.rotations;

import rich.Initialization;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.combat.aura.target.Vector;
import rich.util.timer.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class SnapAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();
    private static long lastSnapTime = 0;
    private static Angle snappedAngle = null;
    private static boolean holdingSnap = false;

    public SnapAngle() {
        super("Snap");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float preAttackSpeed = 1F;
        float postAttackSpeed = 1F;
        float speed = canAttack ? preAttackSpeed : postAttackSpeed;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
        moveAngle.setPitch(MathHelper.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch));

        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}