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
import rich.util.move.MoveUtil;
import rich.util.timer.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class MatrixAngle extends RotateConstructor {
    public MatrixAngle() {
        super("Matrix");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        if (entity !=null && canAttack) {
            Vec3d aimPoint = Vector.hitbox(entity, 1, entity.isOnGround() ? 0.9F : 1.4F, 1, 2);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }
        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean lookingAtHitbox = false;
        if (entity != null && !canAttack && RaycastAngle.rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), 4.0, entity.getBoundingBox())) {
            lookingAtHitbox = true;
        }
        float preAttackSpeed = 1F;
        float postAttackSpeed = lookingAtHitbox ? 0.06F : randomLerp(0F, 0.5F);
        float speed = canAttack ? preAttackSpeed : postAttackSpeed;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 360);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
        float jitterYaw = canAttack ? 0 : MoveUtil.hasPlayerMovement() ? (float) (6 * Math.sin(System.currentTimeMillis() / 65D)) : 0;
        float jitterPitch = canAttack ? 0 : MoveUtil.hasPlayerMovement() ? (float) (2 * Math.cos(System.currentTimeMillis() / 65D)) : 0;

        float resolve1 = canAttack ? 0 : 13, resolve2 = canAttack ? 0 : 8;

        if (!aura.isState() || entity == null) {
            float speedFactor3 = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.1f, 1.0f);
            speed = !attackTimer.finished(550) ? 0.05F : 0.8F * speedFactor3;
            jitterYaw = 0;
            resolve2 = 0;
            resolve1 = 0;
            jitterPitch = 0;
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw) + resolve1;
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) + resolve2;
        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}