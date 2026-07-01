package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);

        // Pure AMOLED black canvas with subtle cool gradient at edges
        int c0 = new Color(6, 7, 14, baseAlpha).getRGB();
        int c1 = new Color(4, 5, 11, baseAlpha).getRGB();
        int c2 = new Color(3, 4, 10, baseAlpha).getRGB();
        int c3 = new Color(5, 6, 13, baseAlpha).getRGB();

        Render2D.gradientRect(bgX, bgY, 400, 250,
                new int[]{c0, c1, c2, c3}, 12);

        // Минимальный sheen для глубины
        Render2D.holoSheen(
                bgX, bgY, 400, 250,
                12,
                new Color(100, 156, 255, Math.max(0, Math.min(255, (int) (180 * alphaMultiplier)))).getRGB(),
                0.06f * alphaMultiplier,
                0.12f,
                1.00f,
                0.02f
        );

        // Тонкий акцентный аутлайн вместо чёрного
        Render2D.outline(bgX, bgY, 400, 250, 0.8f,
                new Color(28, 38, 60, (int)(180 * alphaMultiplier)).getRGB(), 12);

        // Внешнее свечение — очень тонкое
        Render2D.outline(bgX - 1, bgY - 1, 402, 252, 2f,
                new Color(40, 70, 120, (int)(25 * alphaMultiplier)).getRGB(), 13);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int outlineAlpha = (int) (255 * alphaMultiplier);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, ClickGuiPalette.panelInset(alphaMultiplier), 8);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f,
                new Color(22, 28, 44, outlineAlpha).getRGB(), 8);

        Render2D.rect(bgX + 87.5f, bgY + 7.5f, 0.5f, bgHeight - 15,
                new Color(16, 20, 34, (int) (180 * alphaMultiplier)).getRGB(), 0);

        Render2D.outline(bgX + 12.5f, bgY + 220.5f, 70, 17, 0.5f,
                new Color(28, 34, 50, outlineAlpha).getRGB(), 5);

        int iconAccent = new Color(106, 156, 255, (int) (80 * alphaMultiplier)).getRGB();
        Fonts.GUI_ICONS.draw("X", bgX + 21.15f, bgY + 217.5f, 19, iconAccent);
        Fonts.GUI_ICONS.draw("Y", bgX + 40f, bgY + 217f, 20, iconAccent);
        Fonts.GUI_ICONS.draw("Z", bgX + 60f, bgY + 217f, 20, iconAccent);

        Render2D.blur(bgX + 12.5f, bgY + 220.5f, 70, 17, 3.5f, 5,
                new Color(6, 8, 14, (int) (150 * alphaMultiplier)).getRGB());

        float textSize = 6f;
        String soonText = "Soon...";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = bgX + 12.5f + (70 - textWidth) / 2f;
        float centerY = bgY + 220.5f + (17 - textHeight) / 2f;
        Fonts.BOLD.draw(soonText, centerX, centerY, textSize,
                new Color(106, 156, 255, (int) (180 * alphaMultiplier)).getRGB());
    }
}
