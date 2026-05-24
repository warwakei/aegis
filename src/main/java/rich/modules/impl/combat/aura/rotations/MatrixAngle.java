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

import java.util.Random;

public class MatrixAngle extends RotateConstructor {
    private static final Random RANDOM = new Random();
    private static final float RAGE_MULTIPLIER = 1.7f;
    
    private long lastRotationTime = 0;
    private float lastYawDelta = 0;
    private float lastPitchDelta = 0;
    private int consistentRotations = 0;
    private boolean isInCombat = false;
    private long combatStartTime = 0;
    private float accumulatedError = 0;
    private float lastSpeed = 0;
    private int framesSinceLastAttack = 0;
    private boolean wasLookingAtTarget = false;
    private float targetSwitchCooldown = 0;
    
    public MatrixAngle() {
        super("Matrix");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        
        boolean isTargetFar = entity != null && entity.distanceTo(mc.player) > 3.5f;
        
        if (entity != null) {
            // Целимся в центр хитбокса с небольшим смещением вниз
            float distance = entity.distanceTo(mc.player);
            
            // Центр хитбокса = Y + половина высоты
            double centerY = entity.getY() + (entity.getHeight() / 2.0);
            
            // Небольшое смещение вниз для более стабильного попадания
            double offset;
            if (distance < 2.0f) {
                offset = -0.1; // Близко - чуть ниже центра
            } else if (distance < 4.0f) {
                offset = 0.0; // Средняя дистанция - точно в центр
            } else {
                offset = 0.05; // Далеко - чуть выше центра
            }
            
            Vec3d aimPoint = new Vec3d(
                entity.getX(),
                centerY + offset,
                entity.getZ()
            );
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }
        
        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        
        long currentTime = System.currentTimeMillis();
        boolean isRotationSuspicious = detectSuspiciousRotation(yawDelta, pitchDelta, currentTime);
        
        updateCombatState(canAttack, currentTime);
        
        boolean lookingAtHitbox = false;
        if (entity != null && !canAttack && RaycastAngle.rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), 4.0, entity.getBoundingBox())) {
            lookingAtHitbox = true;
        }
        
        // Детект смены цели для burst режима
        boolean targetChanged = !wasLookingAtTarget && lookingAtHitbox;
        if (targetChanged) {
            targetSwitchCooldown = 1.0f;
        }
        wasLookingAtTarget = lookingAtHitbox;
        
        // Сброс счетчика при атаке
        if (canAttack) {
            framesSinceLastAttack = 0;
        }
        
        float baseSpeed = calculateAdaptiveSpeed(canAttack, lookingAtHitbox, isTargetFar, isRotationSuspicious);
        float speed = applyAntiCheatBypass(baseSpeed, rotationDifference, currentTime);
        
        // Защита от деления на ноль
        if (rotationDifference < 0.01f) {
            return currentAngle;
        }
        
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 360);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
        
        float[] jitters = calculateSmartJitters(canAttack, currentTime, isRotationSuspicious);
        float jitterYaw = jitters[0];
        float jitterPitch = jitters[1];

        float resolve1 = canAttack ? fastRandom(-1.0f, 1.0f) : fastRandom(4.0f, 8.5f);
        float resolve2 = canAttack ? fastRandom(-0.8f, 0.8f) : fastRandom(2.2f, 6.0f);

        if (!aura.isState() || entity == null) {
            float speedFactor3 = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.15f, 1.0f);
            speed = !attackTimer.finished(650) ? 0.08F : 0.6F * speedFactor3;
            jitterYaw = 0;
            resolve2 = 0;
            resolve1 = 0;
            jitterPitch = 0;
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw) + resolve1;
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) + resolve2;
        
        // Нормализация yaw ПЕРЕД добавлением ошибки
        while (moveYaw > 180) moveYaw -= 360;
        while (moveYaw < -180) moveYaw += 360;
        
        // Накопленная ошибка с затуханием
        moveYaw += accumulatedError * 0.2f;
        accumulatedError += fastRandom(-0.15f, 0.15f);
        accumulatedError *= 0.95f;
        accumulatedError = MathHelper.clamp(accumulatedError, -1.5f, 1.5f);
        
        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

        lastYawDelta = yawDelta;
        lastPitchDelta = pitchDelta;
        lastRotationTime = currentTime;

        return moveAngle;
    }
    
    private boolean detectSuspiciousRotation(float yawDelta, float pitchDelta, long currentTime) {
        float deltaThreshold = 0.5f;
        if (Math.abs(yawDelta - lastYawDelta) < deltaThreshold && Math.abs(pitchDelta - lastPitchDelta) < deltaThreshold) {
            consistentRotations++;
        } else {
            consistentRotations = 0;
        }
        
        return consistentRotations > 8 || (currentTime - lastRotationTime) < 16; // ~60fps = 16ms
    }
    
    private void updateCombatState(boolean canAttack, long currentTime) {
        if (canAttack && !isInCombat) {
            isInCombat = true;
            combatStartTime = currentTime;
        } else if (!canAttack && isInCombat && (currentTime - combatStartTime) > 2000) {
            isInCombat = false;
        }
    }
    
    private float calculateAdaptiveSpeed(boolean canAttack, boolean lookingAtHitbox, boolean isTargetFar, boolean suspicious) {
        float preAttackSpeed = fastRandom(2.0F, 3.0F) * RAGE_MULTIPLIER;
        float postAttackSpeed = lookingAtHitbox ? fastRandom(1.2F, 1.6F) : fastRandom(1.5F, 2.0F);
        
        // Burst speed при смене цели
        if (targetSwitchCooldown > 0) {
            preAttackSpeed *= 1.9f;
            targetSwitchCooldown -= 0.1f;
        }
        
        return canAttack ? preAttackSpeed : postAttackSpeed;
    }
    
    private float applyAntiCheatBypass(float baseSpeed, float rotationDifference, long currentTime) {
        // Упрощаем логику для более быстрого наведения
        float humanFactor = (float) (0.98f + 0.05f * Math.sin(currentTime / 1000.0));
        
        // Убираем имитацию усталости мыши - всегда быстро
        float mouseFatigue = 1.0f;
        
        // Убираем замедление для больших углов - быстро поворачиваемся
        // if (rotationDifference > 90) {
        //     humanFactor *= 0.82f;
        // } else if (rotationDifference > 45) {
        //     humanFactor *= 0.92f;
        // }
        
        // Убираем случайные микро-лаги
        // if (RANDOM.nextFloat() < 0.08f) {
        //     humanFactor *= 0.65f;
        // }
        
        // Более быстрое изменение скорости
        float targetSpeed = baseSpeed * humanFactor * mouseFatigue;
        lastSpeed = MathHelper.lerp(0.7f, lastSpeed, targetSpeed);
        
        framesSinceLastAttack++;
        
        return lastSpeed;
    }
    
    private float[] calculateSmartJitters(boolean canAttack, long currentTime, boolean suspicious) {
        float jitterMultiplier = suspicious ? 0.4f : 0.6f;
        
        float baseJitterYaw = (float)(1.4 * Math.sin(currentTime / 55D)) * jitterMultiplier;
        float microJitterYaw = (float)(0.4 * Math.sin(currentTime / 23D + Math.PI/6));
        float humanJitterYaw = (float)(0.6 * Math.cos(currentTime / 87D));
        
        float baseJitterPitch = (float)(0.9 * Math.cos(currentTime / 63D)) * jitterMultiplier;
        float microJitterPitch = (float)(0.3 * Math.cos(currentTime / 31D + Math.PI/4));
        float humanJitterPitch = (float)(0.5 * Math.sin(currentTime / 94D));
        
        float jitterYaw = canAttack ? 
            fastRandom(-0.9f, 0.9f) + baseJitterYaw + microJitterYaw : 
            fastRandom(-1.5f, 1.5f) + baseJitterYaw + humanJitterYaw;
            
        float jitterPitch = canAttack ? 
            fastRandom(-0.6f, 0.6f) + baseJitterPitch + microJitterPitch : 
            fastRandom(-1.0f, 1.0f) + baseJitterPitch + humanJitterPitch;
            
        // Стресс и адреналин в бою
        if (isInCombat) {
            long combatDuration = currentTime - combatStartTime;
            float combatStress = Math.min(combatDuration / 5000.0f, 0.6f);
            float adrenaline = (float)(0.2 * Math.sin(combatDuration / 200.0));
            
            jitterYaw += (fastRandom(-0.6f, 0.6f) + adrenaline) * combatStress;
            jitterPitch += (fastRandom(-0.4f, 0.4f) + adrenaline * 0.5f) * combatStress;
        }
        
        // Имитация дрожания рук при долгой игре
        if (framesSinceLastAttack > 300) {
            float handShake = Math.min(framesSinceLastAttack / 1500.0f, 0.4f);
            jitterYaw += fastRandom(-handShake, handShake);
            jitterPitch += fastRandom(-handShake * 0.5f, handShake * 0.5f);
        }
        
        return new float[]{jitterYaw, jitterPitch};
    }

    private float fastRandom(float min, float max) {
        return min + RANDOM.nextFloat() * (max - min);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}