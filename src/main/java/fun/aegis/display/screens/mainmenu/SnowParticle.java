package fun.aegis.display.screens.mainmenu;

import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Random;

public class SnowParticle {
    private float x, y;
    private float size;
    private float speed;
    private float drift;
    private float opacity;
    private final Random random = new Random();
    
    public SnowParticle(float screenWidth, float screenHeight) {
        reset(screenWidth, screenHeight);
    }
    
    public void reset(float screenWidth, float screenHeight) {
        this.x = random.nextFloat() * screenWidth;
        this.y = -10f;
        this.size = random.nextFloat() * 2.5f + 1.0f;
        this.speed = random.nextFloat() * 1.5f + 0.5f;
        this.drift = random.nextFloat() * 0.8f - 0.4f;
        this.opacity = random.nextFloat() * 0.4f + 0.3f;
    }
    
    public void update(float screenWidth, float screenHeight, float deltaTime) {
        y += speed * deltaTime * 60f;
        x += drift * deltaTime * 60f;
        
        if (y > screenHeight + 10) {
            reset(screenWidth, screenHeight);
        }
        
        if (x < -10) {
            x = screenWidth + 10;
        } else if (x > screenWidth + 10) {
            x = -10;
        }
    }
    
    public void render(DrawContext context, int color) {
        int alpha = (int) (opacity * 255);
        int particleColor = ((alpha & 0xFF) << 24) | ((color >> 16) & 0xFF) << 16 | ((color >> 8) & 0xFF) << 8 | (color & 0xFF);
        
        context.fill((int)(x - size/2), (int)(y - size/2), (int)(x + size/2), (int)(y + size/2), particleColor);
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getSize() { return size; }
    public float getOpacity() { return opacity; }
}
