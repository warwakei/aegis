package rich.modules.impl.combat.aura.rotations;

import rich.IMinecraft;
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

/**
 * Jenro — обход Matrix v7.19.2 (FunnyGame, LuckyWorld, SunnyWorld, BarsMine и др.)
 * На базе SpookyTime, но без "пьяного" эффекта после выключения
 */
public class JenroAngle extends RotateConstructor implements IMinecraft {

    private final SecureRandom random = new SecureRandom();

    private float currentJitterYaw = 0;
    private float currentJitterPitch = 0;
    private float targetJitterYaw = 0;
    private float targetJitterPitch = 0;

    private float circlePhase = 0;
    private float circleRadius = 0;
    private float targetCircleRadius = 0;

    private float currentSpeed = 0;

    // Состояние для отслеживания выключения
    private boolean wasActive = false;

    public JenroAngle() {
        super("Jenro");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        int count = attackHandler.getCount();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        boolean isActive = aura.isState();

        // При выключении ауры — РЕЗКО обнуляем джиттер
        if (!isActive) {
            if (wasActive) {
                // Только что выключили — резко сбрасываем
                currentJitterYaw = 0;
                currentJitterPitch = 0;
                targetJitterYaw = 0;
                targetJitterPitch = 0;
                circleRadius = 0;
                targetCircleRadius = 0;
                currentSpeed = 0;
                wasActive = false;
            }
            return currentAngle; // Аура выключена - не поворачиваемся
        }

        // Если нет цели - тоже не поворачиваемся
        if (entity == null) {
            return currentAngle;
        }

        wasActive = true;

        if (entity != null) {
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

        // Для ElytraTarget/MaceTarget уменьшаем джиттер чтобы не мешал таргетингу
        boolean isElytraFlying = mc.player.isGliding();
        boolean elytraTargetMode = isElytraFlying && entity instanceof net.minecraft.entity.LivingEntity le && le.isGliding();

        float deltaTime = 0.75f;
        circlePhase += deltaTime * randomLerp(11f, 16f); // Ещё быстрее фаза для быстрой реакции
        if (circlePhase > Math.PI * 2) circlePhase -= Math.PI * 2;

        if (canAttack) {
            targetCircleRadius = (elytraTargetMode || isElytraFlying) ? randomLerp(0.2f, 1f) : randomLerp(0.5f, 3.5f);
        } else if (lookingAtHitbox) {
            targetCircleRadius = (elytraTargetMode || isElytraFlying) ? randomLerp(1.5f, 3f) : randomLerp(10f, 14f);
        } else {
            targetCircleRadius = (elytraTargetMode || isElytraFlying) ? randomLerp(1.5f, 4f) : randomLerp(10f, 15f);
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

        // Для ElytraTarget/MaceTarget ещё сильнее уменьшаем джиттер
        if (elytraTargetMode || isElytraFlying) {
            jitterMultiplier = 0.25f;
        }

        targetJitterYaw = randomLerp(38f, 35f) * randomAngle.getYaw() * jitterMultiplier;
        targetJitterPitch = randomLerp(8f, 3f) * randomAngle.getPitch() * jitterMultiplier;

        float jitterSmoothSpeed = 0.22f; // Быстрее джиттер (было 0.15)
        currentJitterYaw += (targetJitterYaw - currentJitterYaw) * jitterSmoothSpeed;
        currentJitterPitch += (targetJitterPitch - currentJitterPitch) * jitterSmoothSpeed;

        float targetSpeed;
        if (canAttack) {
            targetSpeed = (elytraTargetMode || isElytraFlying) ? randomLerp(1.3f, 1.5f) : randomLerp(1.2f, 1.4f);
        } else if (lookingAtHitbox) {
            targetSpeed = (elytraTargetMode || isElytraFlying) ? randomLerp(0.95f, 1.05f) : randomLerp(0.5f, 0.3f);
        } else if (entity != null) {
            // Для ElytraTarget/MaceTarget максимальная скорость — мы летим за целью
            if (elytraTargetMode || isElytraFlying) {
                targetSpeed = randomLerp(1.0f, 1.1f); // Максимальная скорость для элитр
            } else {
                float distanceFactor = MathHelper.clamp(rotationDifference / 30f, 0.1f, 1f);
                targetSpeed = randomLerp(0.6f, 0.4f) * distanceFactor;
            }
        } else {
            targetSpeed = !attackTimer.finished(600) ? 0.7f : randomLerp(0.35f, 0.5f);
        }

        currentSpeed += (targetSpeed - currentSpeed) * 0.9f; // Моментальная интерполяция

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 90);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float totalJitterYaw = currentJitterYaw + circleYaw;
        float totalJitterPitch = currentJitterPitch + circlePitch;

        // При атаке — без плавности, сразу в цель
        float newYaw, newPitch;
        if (canAttack) {
            // Рейдж: моментально в цель + джиттер
            if (elytraTargetMode || isElytraFlying) {
                // На элитрах — быстрая ротация почти без сглаживания
                newYaw = MathHelper.lerp(0.9f, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + totalJitterYaw;
                newPitch = MathHelper.lerp(0.9f, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + totalJitterPitch;
            } else {
                // Обычная атака — мгновенно
                newYaw = currentAngle.getYaw() + moveYaw + totalJitterYaw;
                newPitch = currentAngle.getPitch() + movePitch + totalJitterPitch;
            }
        } else {
            // Не может атаковать — быстрая ротация для преследования
            if (elytraTargetMode) {
                // Для ElytraTarget очень быстрое следование за целью
                newYaw = MathHelper.lerp(0.9f, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + totalJitterYaw;
                newPitch = MathHelper.lerp(0.9f, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + totalJitterPitch;
            } else {
                // Обычная плавность
                newYaw = MathHelper.lerp(currentSpeed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + totalJitterYaw;
                newPitch = MathHelper.lerp(currentSpeed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + totalJitterPitch;
            }
        }

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
