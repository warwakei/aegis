package fun.aegis.utils.display.glow;

import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.features.impl.render.Hud;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Utility class for applying glow effects to HUD elements
 */
public class GlowEffect {
    
    /**
     * Applies animated glow effect to a HUD element
     * @param matrix MatrixStack for rendering
     * @param x X position of the element
     * @param y Y position of the element  
     * @param width Width of the element
     * @param height Height of the element
     * @param blur Blur renderer instance
     */
    public static void applyGlow(MatrixStack matrix, float x, float y, float width, float height, Object blur) {
        applyGlow(matrix, x, y, width, height, blur, 4.0f);
    }

    /**
     * Applies animated glow effect to a HUD element with custom rounding
     * @param matrix MatrixStack for rendering
     * @param x X position of the element
     * @param y Y position of the element  
     * @param width Width of the element
     * @param height Height of the element
     * @param blur Blur renderer instance
     * @param rounding Corner rounding value
     */
    public static void applyGlow(MatrixStack matrix, float x, float y, float width, float height, Object blur, float rounding) {
        if (!isGlowEnabled()) {
            return;
        }
        
        // Animated glow effect with white and client theme colors
        long time = System.currentTimeMillis();
        float pulse = (float) Math.sin(time * 0.002) * 0.5f + 0.5f; // Smooth pulse effect
        
        int clientColor = ColorAssist.getClientColor();
        int whiteColor = java.awt.Color.WHITE.getRGB();
        
        // Create pulsing blend of white and client color
        int blendColor1 = ColorAssist.interpolateColor(whiteColor, clientColor, 0.3f + pulse * 0.2f);
        int blendColor2 = ColorAssist.interpolateColor(clientColor, whiteColor, 0.4f + pulse * 0.3f);
        
        try {
            // Use reflection to call blur.render method
            blur.getClass().getMethod("render", ShapeProperties.class)
                .invoke(blur, ShapeProperties.create(matrix, x - 2, y - 2, width + 4, height + 4)
                    .round(rounding).softness(2).thickness(3).outlineColor(ColorAssist.multAlpha(blendColor1, 0.4f)).color(ColorAssist.multAlpha(blendColor1, 0.4f)).build());
            
            blur.getClass().getMethod("render", ShapeProperties.class)
                .invoke(blur, ShapeProperties.create(matrix, x - 1, y - 1, width + 2, height + 2)
                    .round(rounding - 0.5f).softness(1.5F).thickness(2).outlineColor(ColorAssist.multAlpha(blendColor2, 0.6f)).color(ColorAssist.multAlpha(blendColor2, 0.6f)).build());
        } catch (Exception e) {
            // Fallback - do nothing if reflection fails
        }
    }
    
    /**
     * Checks if glow effect is enabled
     * @return false since glow effect was removed
     */
    public static boolean isGlowEnabled() {
        return false;
    }
}
