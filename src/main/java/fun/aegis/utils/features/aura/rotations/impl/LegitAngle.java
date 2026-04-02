package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.point.Vector;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class LegitAngle extends RotateConstructor {
    public LegitAngle() {
        super("Matrix");
    }

    private boolean isPullingBack = false;
    private long pullBackStartTime = 0;
    private final SecureRandom random = new SecureRandom();

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        if (entity !=null) {
            Vec3d aimPoint = Vector.hitbox(entity, 1, 1.2f, 1, 4);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float preAttackSpeed = 0.75F;
        float speed = !attackTimer.finished(250) ? 0.1F : 0.7F;
        if (!attackTimer.finished(350)) speed = 0.15F;
        if (!attackTimer.finished(500)) speed = 0.25F;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * (canAttack ? 360 : 100));
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
        float jitterYaw = canAttack ? 0 : (float) (3 * Math.sin(System.currentTimeMillis() / 85D));
        float jitterPitch = canAttack ? 0 : (float) (2 * Math.cos(System.currentTimeMillis() / 85D));

        if (!aura.isState() || entity == null) {
            jitterYaw = 0;
            jitterPitch = 0;
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) ;
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) +  jitterPitch);

        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}
