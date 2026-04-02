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

public class SPAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();

    private float currentJitterYaw = 0;
    private float currentJitterPitch = 0;
    private float targetJitterYaw = 0;
    private float targetJitterPitch = 0;

    private float circlePhase = 0;
    private float circleRadius = 0;
    private float targetCircleRadius = 0;

    private float currentSpeed = 0;

    public SPAngle() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        int count = attackHandler.getCount();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        if (entity != null && canAttack) {
            Vec3d aimPoint = Vector.hitbox(entity, 1, entity.isOnGround() ? 1F : 1.256F, 1, 2);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.01f) rotationDifference = 1;

        boolean lookingAtHitbox = false;
        if (entity != null && !canAttack) {
            lookingAtHitbox = RaycastAngle.rayTrace(
                    AngleConnection.INSTANCE.getRotation().toVector(),
                    4.0,
                    entity.getBoundingBox()
            );
        }

        float deltaTime = 0.75f;
        circlePhase += deltaTime * randomLerp(7.5f, 12.5f);
        if (circlePhase > Math.PI * 2) circlePhase -= Math.PI * 2;

        if (canAttack) {
            targetCircleRadius = randomLerp(0.5f, 4.5f);
        } else if (lookingAtHitbox) {
            targetCircleRadius = randomLerp(12f, 12f);
        } else {
            targetCircleRadius = randomLerp(8f, 12f);
        }

        circleRadius += (targetCircleRadius - circleRadius) * 0.18f;

        float circleYaw = (float) (Math.cos(circlePhase) * circleRadius);
        float circlePitch = (float) (Math.sin(circlePhase * 11.3f) * circleRadius * 0.4f);

        float timeRandom = attackTimer.elapsedTime() / 100F + (count % 5);
        int pattern = count % 4;

        Angle randomAngle = switch (pattern) {
            case 0 -> new Angle((float) Math.cos(timeRandom), (float) Math.sin(timeRandom));
            case 1 -> new Angle((float) Math.sin(timeRandom * 2.2f), (float) Math.cos(timeRandom * 0.6f));
            case 2 -> new Angle((float) Math.sin(timeRandom), (float) -Math.cos(timeRandom));
            default -> new Angle((float) -Math.cos(timeRandom * 0.5f), (float) Math.sin(timeRandom * 2.1f));
        };

        float jitterMultiplier = canAttack ? 0.5f : (lookingAtHitbox ? 0.6f : 1f);

        targetJitterYaw = randomLerp(35f, 32f) * randomAngle.getYaw() * jitterMultiplier;
        targetJitterPitch = randomLerp(5f, 2f) * randomAngle.getPitch() * jitterMultiplier;

        float jitterSmoothSpeed = 0.15f;
        currentJitterYaw += (targetJitterYaw - currentJitterYaw) * jitterSmoothSpeed;
        currentJitterPitch += (targetJitterPitch - currentJitterPitch) * jitterSmoothSpeed;

        float targetSpeed;
        if (canAttack) {
            targetSpeed = randomLerp(1f, 1f);
        } else if (lookingAtHitbox) {
            targetSpeed = randomLerp(0.35f, 0.15f);
        } else if (entity != null) {
            float distanceFactor = MathHelper.clamp(rotationDifference / 30f, 0.1f, 1f);
            targetSpeed = randomLerp(0.45f, 0.25f) * distanceFactor;
        } else {
            targetSpeed = !attackTimer.finished(600) ? 0.53f : randomLerp(0.2f, 0.35f);
        }

        currentSpeed += (targetSpeed - currentSpeed) * 0.65f;

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 90);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float totalJitterYaw = currentJitterYaw + circleYaw;
        float totalJitterPitch = currentJitterPitch + circlePitch;

        if (!aura.isState() || entity == null) {
            if (attackTimer.finished(800)) {
                totalJitterYaw *= 0.3f;
                totalJitterPitch *= 0.3f;
            }
        }

        float newYaw = MathHelper.lerp(currentSpeed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + totalJitterYaw;
        float newPitch = MathHelper.lerp(currentSpeed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + totalJitterPitch;

        return new Angle(newYaw, MathHelper.clamp(newPitch, -90, 90));
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}