package rich.modules.impl.combat.aura.target;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import rich.IMinecraft;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Поиск точки прицеливания по хитбоксу цели.
 *
 * <p>Два режима:
 * <ul>
 *   <li>{@link #computeVector(LivingEntity, float, Angle, Vec3d, boolean, int)} — классический,
 *       вертикальная сетка кандидатов, выбирается ближайший по ротации к текущему взгляду.</li>
 *   <li>{@link #computeVectorSmart(LivingEntity, float, Angle, Vec3d, boolean, int, List)} —
 *       упреждающий: к кандидатам добавляются точки по предсказанным позициям цели
 *       (симуляция движения противника), а выбор минимизирует ожидаемое угловое
 *       отклонение по всем сценариям. Камера заранее «тянется» в сторону, куда цель,
 *       скорее всего, уйдёт — отклонение цели компенсируется малой дельтой поворота.</li>
 * </ul>
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiPoint implements IMinecraft {
    private final Random random = new SecureRandom();
    private Vec3d offset = Vec3d.ZERO;

    /** Жесткая упреждающая лідота по скорости цели (тики). */
    private static final double OFFSET_LEAD_TICKS = 2.0;
    /** Сигма гауссова дребезга прицельной точки. */
    private static final double OFFSET_SIGMA = 0.12;
    /** Лимит смещения по оси, чтобы прицел не улетал с хитбокса. */
    private static final double OFFSET_MAX = 0.4;
    /** Дистанция, дальше которой упреждающие точки не рассматриваются. */
    private static final float FUTURE_RANGE_MARGIN = 3.0F;
    /** Дефолтная плотность сетки для дешёвых вызовов (TargetFinder/fov-чеки) — как в legacy. */
    private static final int CHEAP_POINTS = 11;

    public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3d velocity, boolean ignoreWalls) {
        return computeVector(entity, maxDistance, initialAngle, velocity, ignoreWalls, CHEAP_POINTS);
    }

    public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3d velocity, boolean ignoreWalls, int numPoints) {
        Pair<List<Vec3d>, Box> candidatePoints = generateCandidatePoints(entity, maxDistance, ignoreWalls, numPoints);
        Vec3d bestVector = findBestVector(candidatePoints.getLeft(), initialAngle);

        if (Aura.getInstance().options.isSelected("Интерполяция высоты") && entity != null) {
            bestVector = Vector.hitbox(entity, 1, 1.1F, 1, 6);
        }

        return new Pair<>((bestVector == null ? entity.getEyePos() : bestVector).add(offset), candidatePoints.getRight());
    }

    /**
     * Умный прицел: кандидаты текущего хитбокса + упреждающие точки по предсказанным
     * позициям цели. {@code predicted} — пары (центральная точку предсказанной позиции, вес).
     * Выбор: кандидат с минимальной взвешенной суммой квадратов угловых отклонений
     * от текущей ротации и от каждого сценария.
     */
    public Pair<Vec3d, Box> computeVectorSmart(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3d velocity, boolean ignoreWalls, int numPoints, List<Pair<Vec3d, Double>> predicted) {
        Box entityBox = entity.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();
        float futureRange = maxDistance + FUTURE_RANGE_MARGIN;

        List<Vec3d> candidates = new ArrayList<>(generateCandidatePoints(entity, maxDistance, ignoreWalls, numPoints).getLeft());

        if (predicted != null && !predicted.isEmpty()) {
            double hitboxY = entityBox.getLengthY();
            for (Pair<Vec3d, Double> entry : predicted) {
                Vec3d center = entry.getLeft();
                if (eye.distanceTo(center) > futureRange) continue;

                // три вертикальных точки внутри предсказанного хитбокса
                candidates.add(new Vec3d(center.x, center.y, center.z));
                candidates.add(new Vec3d(center.x, center.y - hitboxY * 0.18, center.z));
                candidates.add(new Vec3d(center.x, center.y + hitboxY * 0.18, center.z));
            }
        }

        if (candidates.isEmpty()) {
            return new Pair<>(entity.getEyePos(), entityBox);
        }

        Vec3d bestVector = predicted == null || predicted.isEmpty()
                ? findBestVector(candidates, initialAngle)
                : findBestVectorSmart(candidates, eye, initialAngle, predicted);

        return new Pair<>((bestVector == null ? entity.getEyePos() : bestVector).add(offset), entityBox);
    }

    public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
        return generateCandidatePoints(entity, maxDistance, ignoreWalls, CHEAP_POINTS);
    }

    public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, int numPoints) {
        Box entityBox = entity.getBoundingBox();
        int count = Math.max(2, numPoints);
        Vec3d centerXZ = entityBox.getCenter();
        double y0 = entityBox.minY;
        double y1 = entityBox.maxY;

        List<Vec3d> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double y = (count == 1) ? y0 : y0 + (y1 - y0) * i / (count - 1.0);
            Vec3d point = new Vec3d(centerXZ.x, y, centerXZ.z);
            if (isValidPoint(mc.player.getEyePos(), point, maxDistance, ignoreWalls)) {
                list.add(point);
            }
        }

        return new Pair<>(list, entityBox);
    }

    public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
        Box entityBox = entity.getBoundingBox();
        double stepY = entityBox.getLengthY() / 10.0F;

        return Stream.iterate(entityBox.minY, y -> y < entityBox.maxY, y -> y + stepY)
                .map(y -> new Vec3d(entityBox.getCenter().x, y, entityBox.getCenter().z))
                .anyMatch(point -> isValidPoint(mc.player.getEyePos(), point, maxDistance, ignoreWalls));
    }

    private boolean isValidPoint(Vec3d startPoint, Vec3d endPoint, float maxDistance, boolean ignoreWalls) {
        return startPoint.distanceTo(endPoint) <= maxDistance && (ignoreWalls || !RaycastAngle.raycast(startPoint, endPoint, RaycastContext.ShapeType.COLLIDER).getType().equals(HitResult.Type.BLOCK));
    }

    /** Классический выбор: ближайшая к текущей ротации точка. */
    private Vec3d findBestVector(List<Vec3d> candidatePoints, Angle initialAngle) {
        return candidatePoints.stream().min(Comparator.comparing(point -> calculateRotationDifference(mc.player.getEyePos(), point, initialAngle))).orElse(null);
    }

    /** Мудрый выбор: взвешенная сумма квадратов отклонений по всем сценариям. */
    private Vec3d findBestVectorSmart(List<Vec3d> candidatePoints, Vec3d eye, Angle initialAngle, List<Pair<Vec3d, Double>> predicted) {
        Vec3d best = null;
        double bestScore = Double.MAX_VALUE;

        for (Vec3d candidate : candidatePoints) {
            double score = calculateRotationDifference(eye, candidate, initialAngle);
            score = score * score;

            for (Pair<Vec3d, Double> ref : predicted) {
                Angle refAngle = MathAngle.fromVec3d(ref.getLeft().subtract(eye));
                Angle delta = MathAngle.calculateDelta(refAngle, MathAngle.fromVec3d(candidate.subtract(eye)));
                double d = Math.hypot(delta.getYaw(), delta.getPitch());
                score += ref.getRight() * d * d;
            }

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Angle initialAngle) {
        Angle targetAngle = MathAngle.fromVec3d(endPoint.subtract(startPoint));
        Angle delta = MathAngle.calculateDelta(initialAngle, targetAngle);
        return Math.hypot(delta.getYaw(), delta.getPitch());
    }

    /**
     * Обновление прицельного смещения: лёгкий лід по скорости цели (камера заранее
     * тянется туда, куда цель движется) + гауссов дребезг. Вызывается раз за тик,
     * чтобы камера «дребезжала» вокруг вероятного следующего положения.
     */
    public void updateOffset(Vec3d velocity) {
        offset = new Vec3d(
                MathHelper.clamp(velocity.x * OFFSET_LEAD_TICKS + random.nextGaussian() * OFFSET_SIGMA, -OFFSET_MAX, OFFSET_MAX),
                MathHelper.clamp(velocity.y * OFFSET_LEAD_TICKS * 0.5 + random.nextGaussian() * OFFSET_SIGMA * 0.4, -OFFSET_MAX, OFFSET_MAX),
                MathHelper.clamp(velocity.z * OFFSET_LEAD_TICKS + random.nextGaussian() * OFFSET_SIGMA, -OFFSET_MAX, OFFSET_MAX));
    }

}
