package rich.modules.impl.combat.aura.target;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.util.timer.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static rich.IMinecraft.mc;

public class Vector {

    private static final Random random = new Random();
    private static final StopWatch pointTimer = new StopWatch();
    private static final StopWatch updateTimer = new StopWatch();
    private static List<Vec3d> cachedPoints = new ArrayList<>();
    private static int currentPointIndex = 0;

    public static Vec3d hitbox(Entity entity, float X, float Y, float Z, float WIDTH) {
        double wHalf = entity.getWidth() / WIDTH;
        double yExpand = MathHelper.clamp(entity.getEyeY() - entity.getY(), 0, entity.getHeight());
        double xExpand = MathHelper.clamp(mc.player.getX() - entity.getX(), -wHalf, wHalf);
        double zExpand = MathHelper.clamp(mc.player.getZ() - entity.getZ(), -wHalf, wHalf);

        return new Vec3d(
                entity.getX() + xExpand / X,
                entity.getY() + yExpand / Y,
                entity.getZ() + zExpand / Z
        );
    }

    public static Vec3d brain(Entity entity, float min, float max) {
        double distance = mc.player.getEntityPos().distanceTo(entity.getEntityPos());

        double normalizedDistance = MathHelper.clamp((distance - min) / (max - min), 0, 1);
        double heightFactor = normalizedDistance;

        double minHeight = 0.2;
        double maxHeight = 0.8;
        double targetHeight = minHeight + (maxHeight - minHeight) * heightFactor;

        double targetY = entity.getY() + (entity.getHeight() * targetHeight);

        return new Vec3d(
                entity.getX(),
                targetY,
                entity.getZ()
        );
    }

    public static Vec3d custom(Entity entity, int pointCount, float switchDelay) {
        if (updateTimer.every(1000) || cachedPoints.isEmpty()) {
            generateRandomPoints(entity, pointCount);
            currentPointIndex = 0;
            pointTimer.reset();
        }

        if (pointTimer.finished(switchDelay)) {
            currentPointIndex = (currentPointIndex + 1) % cachedPoints.size();
            pointTimer.reset();
        }

        if (cachedPoints.isEmpty()) {
            return entity.getEntityPos();
        }

        return cachedPoints.get(currentPointIndex);
    }

    private static void generateRandomPoints(Entity entity, int pointCount) {
        cachedPoints.clear();

        double width = entity.getWidth();
        double height = entity.getHeight();
        Vec3d entityPos = entity.getEntityPos();

        for (int i = 0; i < pointCount; i++) {
            double x = entityPos.x + (random.nextDouble() - 0.5) * width;
            double y = entityPos.y + random.nextDouble() * height;
            double z = entityPos.z + (random.nextDouble() - 0.5) * width;

            cachedPoints.add(new Vec3d(x, y, z));
        }
    }

    public static List<Vec3d> getAllCachedPoints() {
        return new ArrayList<>(cachedPoints);
    }

    public static int getCurrentPointIndex() {
        return currentPointIndex;
    }

    public static long getTimeSinceLastSwitch() {
        return pointTimer.elapsedTime();
    }

    public static void clearCache() {
        cachedPoints.clear();
        currentPointIndex = 0;
        pointTimer.reset();
        updateTimer.reset();
    }

    public static void forceUpdate(Entity entity, int pointCount) {
        generateRandomPoints(entity, pointCount);
        currentPointIndex = 0;
        pointTimer.reset();
        updateTimer.reset();
    }
}