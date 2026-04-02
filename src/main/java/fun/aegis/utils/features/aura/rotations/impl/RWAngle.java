package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.client.chat.ChatMessage;
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

public class RWAngle extends RotateConstructor {
    public RWAngle() {
        super("ReallyWorld");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        if (entity != null) {
            Vec3d aimPoint = Vector.brain(entity, 1, 3.5F);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }
        int count = attackHandler.getCount();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float preAttackSpeed = 1.5f;
        float postAttackSpeed = 0.8f;
        float speed = canAttack ? preAttackSpeed : postAttackSpeed;
        float lineYaw = (Math.abs(yawDelta / Math.max(rotationDifference, 0.1f)) * 180);
        float linePitch = (Math.abs(pitchDelta / Math.max(rotationDifference, 0.1f)) * 180);
        float jitterYaw = canAttack ? 0 : (float) (-6 * Math.cos(System.currentTimeMillis() / 90D));
        float jitterPitch = canAttack ? 0 : (float) (6 * Math.sin(System.currentTimeMillis() / 90D));

        if (!aura.isState() || entity == null) {
            speed = 1F;
            jitterYaw = 0;
            jitterPitch = 0;
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.clamp(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch, -90F, 90F));

        if (count > 0 && count % 50 == 0 && !attackTimer.finished(200)) {
            moveAngle.setPitch(MathHelper.clamp(MathHelper.lerp(0.55F, currentAngle.getPitch(), currentAngle.getPitch() - 90) + jitterPitch, -90F, 90F));
        }

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
