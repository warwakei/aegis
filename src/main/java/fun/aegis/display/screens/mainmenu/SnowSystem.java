package fun.aegis.display.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnowSystem {
    private final List<SnowParticle> particles;
    private final Random random = new Random();
    private final int maxParticles;
    private final float spawnRate;
    private float spawnTimer;
    private boolean enabled;
    private int snowColor;
    
    public SnowSystem(int maxParticles, float spawnRate) {
        this.maxParticles = maxParticles;
        this.spawnRate = spawnRate;
        this.particles = new ArrayList<>();
        this.spawnTimer = 0f;
        this.enabled = true;
        this.snowColor = 0xFFFFFF;
    }
    
    public void update(float screenWidth, float screenHeight, float deltaTime) {
        if (!enabled) return;
        
        spawnTimer += deltaTime;
        if (spawnTimer >= spawnRate && particles.size() < maxParticles) {
            particles.add(new SnowParticle(screenWidth, screenHeight));
            spawnTimer = 0f;
        }
        
        particles.removeIf(particle -> particle.getY() > screenHeight + 20);
        
        for (SnowParticle particle : particles) {
            particle.update(screenWidth, screenHeight, deltaTime);
        }
    }
    
    public void render(DrawContext context, float alpha) {
        if (!enabled) return;
        
        int color = ((int) (alpha * 255) << 24) | (snowColor & 0xFFFFFF);
        
        for (SnowParticle particle : particles) {
            particle.render(context, color);
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            particles.clear();
        }
    }
    
    public void setSnowColor(int color) {
        this.snowColor = color;
    }
    
    public void setIntensity(float intensity) {
        int targetParticles = (int) (maxParticles * intensity);
        while (particles.size() > targetParticles) {
            particles.remove(particles.size() - 1);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getParticleCount() {
        return particles.size();
    }
}
