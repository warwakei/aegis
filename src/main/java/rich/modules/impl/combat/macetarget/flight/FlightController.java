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

    // кеш чтоб не считать дважды
    private Vec3d cachedTargetPosition = null;
    private LivingEntity cachedTarget = null;
    private Stage cachedStage = null;
    private long cacheTime = 0;
    private static final long CACHE_DURATION_MS = 25; // кеш 25мс

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
     * блок тут есть ли
     */
    private boolean hasBlockAt(Vec3d pos) {
        if (mc.world == null) return false;
        BlockPos blockPos = BlockPos.ofFloored(pos);
        var blockState = mc.world.getBlockState(blockPos);
        return !blockState.isAir() && blockState.getCollisionShape(mc.world, blockPos).isEmpty() == false;
    }

    /**
     * влезет ли чел 2 блока
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
     * видно ли цель рейкаст
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
     * ищем точку обходя стены
     */
    private Vec3d findSmartPathPosition(LivingEntity target, Stage stage) {
        Vec3d basePos = getTargetPosition(target, stage);

        if (!smartPathEnabled) {
            // без умняка просто вверх вниз
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
     * точка вверх обходя крышу
     */
    private Vec3d findFlyingUpPosition(Vec3d basePos, LivingEntity target) {
        Vec3d flyTarget = basePos.add(0, height, 0);

        // план 1 чек верх вниз
        for (int offset = 0; offset <= 10; offset++) {
            Vec3d checkPos = basePos.add(0, height - offset, 0);
            if (hasSpaceForPlayer(checkPos)) {
                return checkPos;
            }
        }

        // план 2 спираль от центра
        Vec3d spiralResult = findPositionSpiral(basePos, height, 6, 2);
        if (spiralResult != null) {
            return spiralResult;
        }

        // план 3 радиус ближние точки
        Vec3d radiusResult = findPositionInRadius(basePos, height, 8);
        if (radiusResult != null) {
            return radiusResult;
        }

        return flyTarget;
    }

    /**
     * точка для удара обходя стены
     */
    private Vec3d findAttackPosition(Vec3d basePos, LivingEntity target, Stage stage) {
        if (mc.world == null || mc.player == null) {
            return basePos;
        }

        Vec3d playerPos = mc.player.getEyePos();

        // чек прямой путь
        if (hasClearPath(playerPos, basePos)) {
            return basePos;
        }

        // план 1 вверх обходим
        for (int yOffset = 1; yOffset <= 4; yOffset++) {
            Vec3d elevatedPos = basePos.add(0, yOffset, 0);
            if (hasClearPath(playerPos, elevatedPos)) {
                return elevatedPos;
            }
        }

        // план 2 откуда видно
        Vec3d visibleResult = findVisiblePosition(playerPos, basePos, 5, 3);
        if (visibleResult != null) {
            return visibleResult;
        }

        // план 3 спираль вокруг
        Vec3d spiralResult = findPositionSpiral(basePos, 2, 4, 2);
        if (spiralResult != null && hasClearPath(playerPos, spiralResult)) {
            return spiralResult;
        }

        // ниче не зашло берем базу
        return basePos;
    }

    /**
     * спираль ищем свободную
     */
    private Vec3d findPositionSpiral(Vec3d center, double baseHeight, int maxRadius, int maxIterations) {
        List<Vec3d> candidates = new ArrayList<>();

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                double x = center.x + radius * Math.cos(rad);
                double z = center.z + radius * Math.sin(rad);

                // высоты разные чекаем
                for (int yOff = -2; yOff <= 2; yOff++) {
                    Vec3d pos = new Vec3d(x, baseHeight + yOff, z);
                    if (hasSpaceForPlayer(pos)) {
                        // оценка по дистанции
                        double distance = pos.distanceTo(center);
                        candidates.add(pos);
                    }
                }
            }
        }

        // ближнюю берем
        candidates.sort(Comparator.comparingDouble(p -> p.distanceTo(center)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * в радиусе ищем
     */
    private Vec3d findPositionInRadius(Vec3d center, double height, int maxRadius) {
        List<Vec3d> candidates = new ArrayList<>();

        for (int x = -maxRadius; x <= maxRadius; x++) {
            for (int z = -maxRadius; z <= maxRadius; z++) {
                if (x == 0 && z == 0) continue;

                // высоты разные чекаем
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
     * откуда видно цель
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
     * угол с учетом стен
     */
    public Angle calculateAngle(LivingEntity target, Stage stage) {
        if (target == null || mc.player == null) {
            return MathAngle.cameraAngle();
        }

        // кеш чекаем
        long currentTime = System.currentTimeMillis();
        if (cachedTarget == target && cachedStage == stage && 
            (currentTime - cacheTime) < CACHE_DURATION_MS && cachedTargetPosition != null) {
            Vec3d cachedPos = cachedTargetPosition;
            return MathAngle.fromVec3d(cachedPos.subtract(mc.player.getEyePos()));
        }

        Vec3d targetPos = findSmartPathPosition(target, stage);

        // кеш обновили
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
