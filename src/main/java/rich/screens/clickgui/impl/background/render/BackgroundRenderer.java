package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);
        
        long currentTime = System.currentTimeMillis();
        float pulse = (float) Math.sin(currentTime * 0.0015) * 0.1f + 0.9f;
        
        int r1 = (int)(26 * pulse);
        int g1 = (int)(26 * pulse);
        int b1 = (int)(30 * pulse);
        
        int r2 = (int)(5 * pulse);
        int g2 = (int)(5 * pulse);
        int b2 = (int)(8 * pulse);
        
        int[] gradientColors = {
                new Color(r1 + 4, g1 + 4, b1 + 6, baseAlpha).getRGB(),
                new Color(r2, g2, b2, baseAlpha).getRGB(),
                new Color(r1, g1, b1, baseAlpha).getRGB(),
                new Color(r2 + 3, g2 + 3, b2 + 5, baseAlpha).getRGB(),
                new Color(r1 + 2, g1 + 2, b1 + 3, baseAlpha).getRGB()
        };

        Render2D.gradientRect(bgX, bgY, 400, 250, gradientColors, 15);
        
        float edgeGlowSize = 2f;
        int edgeAlpha = (int)(15 * alphaMultiplier);
        int edgeColor = new Color(60, 80, 120, edgeAlpha).getRGB();
        
        Render2D.gradientRect(bgX, bgY, 400, edgeGlowSize,
                new int[]{
                        new Color(60, 80, 120, 0).getRGB(),
                        edgeColor,
                        edgeColor,
                        new Color(60, 80, 120, 0).getRGB()
                }, 0);
        
        Render2D.gradientRect(bgX, bgY + 250 - edgeGlowSize, 400, edgeGlowSize,
                new int[]{
                        new Color(60, 80, 120, 0).getRGB(),
                        edgeColor,
                        edgeColor,
                        new Color(60, 80, 120, 0).getRGB()
                }, 0);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);
        int blurAlpha = (int) (155 * alphaMultiplier);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, new Color(128, 128, 128, panelAlpha).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 10);

        Render2D.outline(bgX + 12.5f, bgY + 220.5f, 70, 17, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 5);

        Fonts.GUI_ICONS.draw("X", bgX + 21.15f, bgY + 217.5f, 19, new Color(58, 58, 58, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Y", bgX + 40f, bgY + 217f, 20, new Color(58, 58, 58, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Z", bgX + 60f, bgY + 217f, 20, new Color(58, 58, 58, outlineAlpha).getRGB());

        Render2D.blur(bgX + 12.5f, bgY + 220.5f, 70, 17, 4, 5, new Color(25, 25, 25, blurAlpha).getRGB());

        float textSize = 6f;
        String soonText = "Soon...";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = bgX + 12.5f + (70 - textWidth) / 2f;
        float centerY = bgY + 220.5f + (17 - textHeight) / 2f;
        Fonts.BOLD.draw(soonText, centerX, centerY, textSize, new Color(150, 150, 150, (int) (200 * alphaMultiplier)).getRGB());
    }
}