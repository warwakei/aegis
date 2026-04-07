package rich.modules.impl.combat.macetarget.flight;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.macetarget.prediction.TargetPredictor;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public class FlightController {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final TargetPredictor predictor;
    private boolean predictionEnabled = false;
    private float height = 30.0f;
    private boolean smartPathEnabled = false;

    // Кэш для избежания повторных вычислений
    private Vec3d cachedTargetPosition = null;
    private LivingEntity cachedTarget = null;
    private Stage cachedStage = null;
    private long cacheTime = 0;
    private static final long CACHE_DURATION_MS = 25; // Кэш на 25ms

    public FlightController(TargetPredictor predictor) {
        this.predictor = predictor;
    }

    public void setPredictionEnabled(boolean enabled) {
        this.predictionEnabled = enabled;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setSmartPathEnabled(boolean enabled) {
        this.smartPathEnabled = enabled;
    }

    public Vec3d getTargetPosition(LivingEntity target, Stage stage) {
        if (target == null) {
            return Vec3d.ZERO;
        }

        if (predictionEnabled && predictor.isMoving()) {
            return predictor.getPredictedPosition(target, stage);
        }

        return target.getEyePos();
    }

    /**
     * Проверяет есть ли блок в указанной позиции
     */
    private boolean hasBlockAt(Vec3d pos) {
        if (mc.world == null) return false;
        BlockPos blockPos = BlockPos.ofFloored(pos);
        var blockState = mc.world.getBlockState(blockPos);
        return !blockState.isAir() && blockState.getCollisionShape(mc.world, blockPos).isEmpty() == false;
    }

    /**
     * Проверяет есть ли место для игрока в данной позиции (2 блока высоты)
     */
    private boolean hasSpaceForPlayer(Vec3d pos) {
        for (int h = 0; h < 2; h++) {
            if (hasBlockAt(pos.add(0, h, 0))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверяет видимость цели (raycast)
     */
    private boolean hasClearPath(Vec3d from, Vec3d to) {
        if (mc.world == null) return false;
        
        var hitResult = mc.world.raycast(
            new net.minecraft.world.RaycastContext(
                from, to,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
            )
        );

        return hitResult.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK;
    }

    /**
     * Ищет безопасную позицию для атаки с полным обходом препятствий по X/Y/Z
     */
    private Vec3d findSmartPathPosition(LivingEntity target, Stage stage) {
        Vec3d basePos = getTargetPosition(target, stage);

        if (!smartPathEnabled) {
            // Без умного путя - просто летим вверх/вниз по Y
            switch (stage) {
                case FLYING_UP -> {
                    return basePos.add(0, height, 0);
                }
                case TARGETTING, ATTACKING -> {
                    return basePos;
                }
                default -> {
                    return basePos;
                }
            }
        }

        switch (stage) {
            case FLYING_UP -> {
                return findFlyingUpPosition(basePos, target);
            }
            case TARGETTING, ATTACKING -> {
                return findAttackPosition(basePos, target, stage);
            }
            default -> {
                return basePos;
            }
        }
    }

    /**
     * Поиск позиции для полёта вверх с обходом препятствий
     */
    private Vec3d findFlyingUpPosition(Vec3d basePos, LivingEntity target) {
        Vec3d flyTarget = basePos.add(0, height, 0);

        // Стратегия 1: Проверяем основную позицию сверху вниз
        for (int offset = 0; offset <= 10; offset++) {
            Vec3d checkPos = basePos.add(0, height - offset, 0);
            if (hasSpaceForPlayer(checkPos)) {
                return checkPos;
            }
        }

        // Стратегия 2: Поиск по спирали от центра
        Vec3d spiralResult = findPositionSpiral(basePos, height, 6, 2);
        if (spiralResult != null) {
            return spiralResult;
        }

        // Стратегия 3: Поиск в радиусе с приоритетом ближайших точек
        Vec3d radiusResult = findPositionInRadius(basePos, height, 8);
        if (radiusResult != null) {
            return radiusResult;
        }

        return flyTarget;
    }

    /**
     * Поиск позиции для атаки с обходом препятствий
     */
    private Vec3d findAttackPosition(Vec3d basePos, LivingEntity target, Stage stage) {
        if (mc.world == null || mc.player == null) {
            return basePos;
        }

        Vec3d playerPos = mc.player.getEyePos();

        // Проверяем прямой путь к цели
        if (hasClearPath(playerPos, basePos)) {
            return basePos;
        }

        // Стратегия 1: Смещение вверх для обхода препятствий
        for (int yOffset = 1; yOffset <= 4; yOffset++) {
            Vec3d elevatedPos = basePos.add(0, yOffset, 0);
            if (hasClearPath(playerPos, elevatedPos)) {
                return elevatedPos;
            }
        }

        // Стратегия 2: Поиск позиции с которой видна цель
        Vec3d visibleResult = findVisiblePosition(playerPos, basePos, 5, 3);
        if (visibleResult != null) {
            return visibleResult;
        }

        // Стратегия 3: Спиральный поиск вокруг цели
        Vec3d spiralResult = findPositionSpiral(basePos, 2, 4, 2);
        if (spiralResult != null && hasClearPath(playerPos, spiralResult)) {
            return spiralResult;
        }

        // Если ничего не помогло, возвращаем базовую позицию
        return basePos;
    }

    /**
     * Спиральный поиск свободной позиции
     */
    private Vec3d findPositionSpiral(Vec3d center, double baseHeight, int maxRadius, int maxIterations) {
        List<Vec3d> candidates = new ArrayList<>();

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                double x = center.x + radius * Math.cos(rad);
                double z = center.z + radius * Math.sin(rad);

                // Проверяем несколько высот
                for (int yOff = -2; yOff <= 2; yOff++) {
                    Vec3d pos = new Vec3d(x, baseHeight + yOff, z);
                    if (hasSpaceForPlayer(pos)) {
                        // Оцениваем позицию по расстоянию до центра
                        double distance = pos.distanceTo(center);
                        candidates.add(pos);
                    }
                }
            }
        }

        // Сортируем по расстоянию и возвращаем ближайшую
        candidates.sort(Comparator.comparingDouble(p -> p.distanceTo(center)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Поиск позиции в радиусе
     */
    private Vec3d findPositionInRadius(Vec3d center, double height, int maxRadius) {
        List<Vec3d> candidates = new ArrayList<>();

        for (int x = -maxRadius; x <= maxRadius; x++) {
            for (int z = -maxRadius; z <= maxRadius; z++) {
                if (x == 0 && z == 0) continue;

                // Проверяем несколько высот
                for (int yOff = -3; yOff <= 3; yOff++) {
                    Vec3d pos = center.add(x, height + yOff, z);
                    if (hasSpaceForPlayer(pos)) {
                        double distance = Math.sqrt(x * x + z * z);
                        candidates.add(pos);
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(p -> p.distanceTo(center)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Поиск позиции с видимостью до цели
     */
    private Vec3d findVisiblePosition(Vec3d from, Vec3d targetPos, int searchRadius, int yOffset) {
        List<Vec3d> candidates = new ArrayList<>();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = 0; y <= yOffset; y++) {
                    Vec3d pos = targetPos.add(x, y, z);
                    if (hasSpaceForPlayer(pos) && hasClearPath(from, pos)) {
                        double distance = pos.distanceTo(from);
                        candidates.add(pos);
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(p -> p.distanceTo(from)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Рассчитывает угол до цели с учётом препятствий
     */
    public Angle calculateAngle(LivingEntity target, Stage stage) {
        if (target == null || mc.player == null) {
            return MathAngle.cameraAngle();
        }

        // Проверяем кэш
        long currentTime = System.currentTimeMillis();
        if (cachedTarget == target && cachedStage == stage && 
            (currentTime - cacheTime) < CACHE_DURATION_MS && cachedTargetPosition != null) {
            Vec3d cachedPos = cachedTargetPosition;
            return MathAngle.fromVec3d(cachedPos.subtract(mc.player.getEyePos()));
        }

        Vec3d targetPos = findSmartPathPosition(target, stage);

        // Обновляем кэш
        cachedTargetPosition = targetPos;
        cachedTarget = target;
        cachedStage = stage;
        cacheTime = currentTime;

        switch (stage) {
            case FLYING_UP, TARGETTING, ATTACKING -> {
                return MathAngle.fromVec3d(targetPos.subtract(mc.player.getEyePos()));
            }
            default -> {
                return MathAngle.cameraAngle();
            }
        }
    }
}
