package rich.modules.impl.combat.macetarget.prediction;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;

@Getter
public class TargetPredictor {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Позиция и скорость
    private Vec3d lastPosition = null;
    private Vec3d velocity = Vec3d.ZERO;
    private Vec3d smoothedVelocity = Vec3d.ZERO;
    
    // Ускорение для предсказания изменений скорости
    private Vec3d acceleration = Vec3d.ZERO;
    private Vec3d smoothedAcceleration = Vec3d.ZERO;
    
    // История позиций для определения паттернов
    private Vec3d[] positionHistory = new Vec3d[10];
    private Vec3d[] velocityHistory = new Vec3d[10];
    private int historyIndex = 0;
    private int historySize = 0;
    
    private long lastUpdateTime = 0;
    private int sampleCount = 0;

    private static final int MIN_SAMPLES = 2;
    private static final double VELOCITY_SMOOTHING = 0.65;
    private static final double ACCELERATION_SMOOTHING = 0.5;
    private static final int MAX_HISTORY = 10;

    // Параметры для определения паттернов движения
    private boolean isStrafing = false;
    private boolean isJumping = false;
    private boolean isSprinting = false;
    private double strafeAngle = 0;
    private double movementConsistency = 1.0; // 0 = хаотичное, 1 = стабильное

    // Jitter параметры для режима предугадывания
    private double jitterOffsetX = 0;
    private double jitterOffsetZ = 0;
    private double jitterOffsetY = 0;
    private long lastJitterUpdate = 0;
    private static final double JITTER_AMPLITUDE = 0.15; // Амплитуда джиттера
    private static final long JITTER_UPDATE_INTERVAL = 75; // Обновление каждые 75ms

    public void update(LivingEntity target) {
        if (target == null) {
            reset();
            return;
        }

        Vec3d currentPos = target.getEntityPos();
        long currentTime = System.currentTimeMillis();

        if (lastPosition != null && lastUpdateTime > 0) {
            long deltaTime = currentTime - lastUpdateTime;
            if (deltaTime > 0 && deltaTime < 500) {
                double deltaSeconds = deltaTime / 1000.0;
                
                // Вычисляем скорость
                Vec3d newVelocity = currentPos.subtract(lastPosition);
                velocity = newVelocity;
                
                // Вычисляем ускорение
                if (sampleCount > 0) {
                    acceleration = newVelocity.subtract(smoothedVelocity).multiply(1.0 / deltaSeconds);
                    smoothedAcceleration = smoothedAcceleration.multiply(ACCELERATION_SMOOTHING)
                            .add(acceleration.multiply(1 - ACCELERATION_SMOOTHING));
                }
                
                // Сглаживание скорости
                smoothedVelocity = smoothedVelocity.multiply(VELOCITY_SMOOTHING)
                        .add(newVelocity.multiply(1 - VELOCITY_SMOOTHING));
                
                // Обновляем историю
                addToHistory(currentPos, newVelocity);
                
                // Определяем паттерны движения
                analyzeMovementPattern();
                
                sampleCount++;
            }
        }

        lastPosition = currentPos;
        lastUpdateTime = currentTime;
        
        // Обновляем jitter
        updateJitter();
    }

    private void addToHistory(Vec3d position, Vec3d velocity) {
        positionHistory[historyIndex] = position;
        velocityHistory[historyIndex] = velocity;
        historyIndex = (historyIndex + 1) % MAX_HISTORY;
        if (historySize < MAX_HISTORY) {
            historySize++;
        }
    }

    private void analyzeMovementPattern() {
        if (historySize < 3) return;

        // Определяем стрейфы по изменению направления горизонтальной скорости
        double recentHorizontalAngle = Math.atan2(velocity.z, velocity.x);
        double prevHorizontalAngle = Math.atan2(velocityHistory[(historyIndex - 2 + MAX_HISTORY) % MAX_HISTORY].z,
                                                 velocityHistory[(historyIndex - 2 + MAX_HISTORY) % MAX_HISTORY].x);
        
        double angleDiff = Math.abs(recentHorizontalAngle - prevHorizontalAngle);
        isStrafing = angleDiff > Math.PI / 4 && angleDiff < Math.PI * 0.75;
        strafeAngle = angleDiff;

        // Определяем прыжки по вертикальной скорости
        isJumping = velocity.y > 0.2;

        // Определяем стабильность движения
        if (historySize >= 5) {
            double variance = 0;
            Vec3d avgVelocity = Vec3d.ZERO;
            
            for (int i = 0; i < Math.min(historySize, 5); i++) {
                avgVelocity = avgVelocity.add(velocityHistory[(historyIndex - 1 - i + MAX_HISTORY) % MAX_HISTORY]);
            }
            avgVelocity = avgVelocity.multiply(1.0 / Math.min(historySize, 5));
            
            for (int i = 0; i < Math.min(historySize, 5); i++) {
                Vec3d diff = velocityHistory[(historyIndex - 1 - i + MAX_HISTORY) % MAX_HISTORY].subtract(avgVelocity);
                variance += diff.horizontalLengthSquared();
            }
            variance /= Math.min(historySize, 5);
            
            movementConsistency = Math.max(0, 1.0 - variance * 10);
        }
    }

    private void updateJitter() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastJitterUpdate < JITTER_UPDATE_INTERVAL) return;
        
        lastJitterUpdate = currentTime;
        
        // Используем текущие параметры движения для адаптивного джиттера
        double baseAmplitude = JITTER_AMPLITUDE;
        
        // Увеличиваем джиттер при стрейфах
        if (isStrafing) {
            baseAmplitude *= 1.5;
        }
        
        // Уменьшаем при стабильном движении
        baseAmplitude *= (2.0 - movementConsistency);
        
        // Создаём псевдослучайный джиттер на основе времени и позиции
        double time = currentTime / 1000.0;
        double posX = positionHistory[0] != null ? positionHistory[0].x : 0;
        double posZ = positionHistory[0] != null ? positionHistory[0].z : 0;
        jitterOffsetX = Math.sin(time * 13.7 + posX) * baseAmplitude;
        jitterOffsetZ = Math.cos(time * 17.3 + posZ) * baseAmplitude;
        jitterOffsetY = Math.sin(time * 9.1) * baseAmplitude * 0.3; // Меньше по Y
    }

    public Vec3d getPredictedPosition(LivingEntity target, Stage stage) {
        if (target == null || mc.player == null) {
            return Vec3d.ZERO;
        }

        Vec3d currentPos = target.getEntityPos();

        if (sampleCount < MIN_SAMPLES || smoothedVelocity.horizontalLengthSquared() < 0.0001) {
            return currentPos;
        }

        double distance = mc.player.distanceTo(target);
        double playerSpeed = mc.player.getVelocity().length();
        double targetSpeed = smoothedVelocity.horizontalLength();

        // Рассчитываем время достижения цели более точно
        double ticksToReach = calculateTicksToReach(stage, distance, playerSpeed, targetSpeed);

        // Применяем ускорение для более точного предсказания
        Vec3d prediction = calculatePredictionWithAcceleration(ticksToReach);

        // Добавляем джиттер для рандомизации точки реакции
        Vec3d jitteredPrediction = prediction.add(jitterOffsetX, jitterOffsetY, jitterOffsetZ);

        return jitteredPrediction;
    }

    private double calculateTicksToReach(Stage stage, double distance, double playerSpeed, double targetSpeed) {
        double ticksToReach;
        double targetY = lastPosition != null ? lastPosition.y : 0;

        switch (stage) {
            case FLYING_UP -> {
                double heightDiff = Math.abs(mc.player.getY() - targetY);
                ticksToReach = (heightDiff + distance) / Math.max(playerSpeed * 20, 1.0) * 1.2;
            }
            case TARGETTING -> {
                ticksToReach = distance / Math.max(playerSpeed * 20, 0.8);
            }
            case ATTACKING -> {
                ticksToReach = distance / Math.max(playerSpeed * 20, 1.5) * 0.8;
            }
            default -> {
                ticksToReach = distance / 2.0;
            }
        }

        // Корректируем на основе ускорения цели
        if (smoothedAcceleration.horizontalLengthSquared() > 0.001) {
            double accelFactor = 1.0 + smoothedAcceleration.horizontalLength() * 0.1;
            ticksToReach *= accelFactor;
        }

        // Корректируем на основе стабильности движения
        ticksToReach *= (2.0 - movementConsistency) * 0.5 + 0.5;

        ticksToReach = Math.min(ticksToReach, 40);
        ticksToReach = Math.max(ticksToReach, 5);

        return ticksToReach;
    }

    private Vec3d calculatePredictionWithAcceleration(double ticksToReach) {
        Vec3d currentPos = lastPosition != null ? lastPosition : Vec3d.ZERO;
        
        // Базовое предсказание по скорости
        Vec3d velocityPrediction = smoothedVelocity.multiply(ticksToReach);
        
        // Добавляем предсказание по ускорению (квадратичное)
        Vec3d accelerationPrediction = smoothedAcceleration.multiply(0.5 * ticksToReach * ticksToReach / 400.0);
        
        Vec3d prediction = velocityPrediction.add(accelerationPrediction);

        // Применяем адаптивный lead multiplier
        double targetSpeed = smoothedVelocity.horizontalLength();
        double leadMultiplier = calculateLeadMultiplier(targetSpeed);

        // Для стрейфов увеличиваем предсказание
        if (isStrafing) {
            leadMultiplier *= 1.2;
        }

        return currentPos.add(prediction.multiply(leadMultiplier));
    }

    private double calculateLeadMultiplier(double targetSpeed) {
        if (targetSpeed < 0.1) {
            return 1.0;
        } else if (targetSpeed < 0.2) {
            return 1.15;
        } else if (targetSpeed < 0.3) {
            return 1.3;
        } else if (targetSpeed < 0.4) {
            return 1.4;
        } else {
            return 1.5;
        }
    }

    public boolean isMoving() {
        return smoothedVelocity.horizontalLengthSquared() > 0.001;
    }

    public boolean isStrafing() {
        return isStrafing;
    }

    public double getMovementConsistency() {
        return movementConsistency;
    }

    public void reset() {
        lastPosition = null;
        velocity = Vec3d.ZERO;
        smoothedVelocity = Vec3d.ZERO;
        acceleration = Vec3d.ZERO;
        smoothedAcceleration = Vec3d.ZERO;
        lastUpdateTime = 0;
        sampleCount = 0;
        historyIndex = 0;
        historySize = 0;
        
        isStrafing = false;
        isJumping = false;
        movementConsistency = 1.0;
        
        jitterOffsetX = 0;
        jitterOffsetZ = 0;
        jitterOffsetY = 0;
    }
}
