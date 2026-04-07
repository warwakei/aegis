package rich.modules.impl.render.particles;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Система трейлов для частиц - хранит историю позиций для визуального следа
 */
public class ParticleTrail {
    
    private final List<TrailPoint> trailPoints = new ArrayList<>();
    private final int maxTrailLength;
    private final int color;
    private final float trailLifetime;
    
    public ParticleTrail(int maxTrailLength, int color, float trailLifetime) {
        this.maxTrailLength = maxTrailLength;
        this.color = color;
        this.trailLifetime = trailLifetime;
    }
    
    public void addPoint(double x, double y, double z, float alpha) {
        trailPoints.add(new TrailPoint(x, y, z, alpha, System.currentTimeMillis()));
        
        // Ограничиваем длину трейла
        while (trailPoints.size() > maxTrailLength) {
            trailPoints.remove(0);
        }
    }
    
    /**
     * Обновляет трейл - удаляет старые точки
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        long maxAge = (long) (trailLifetime * 1000f);
        
        trailPoints.removeIf(p -> (currentTime - p.addedTime) > maxAge);
    }
    
    /**
     * Возвращает точки трейла для рендера
     */
    public List<TrailPoint> getTrailPoints() {
        return trailPoints;
    }
    
    public void clear() {
        trailPoints.clear();
    }
    
    public boolean isEmpty() {
        return trailPoints.isEmpty();
    }
    
    public int getColor() {
        return color;
    }
    
    public static class TrailPoint {
        public final double x, y, z;
        public final float alpha;
        public final long addedTime;
        
        public TrailPoint(double x, double y, double z, float alpha, long addedTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.alpha = alpha;
            this.addedTime = addedTime;
        }
    }
}
