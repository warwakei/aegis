package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);

        // монолитный чёрный фон (без градиента)
        int bg = new Color(6, 7, 12, baseAlpha).getRGB();
        Render2D.rect(bgX, bgY, 420, 260, bg, 12);

        // едва заметный блик для глубины
        Render2D.holoSheen(
                bgX, bgY, 420, 260,
                12,
                new Color(80, 100, 140, Math.max(0, Math.min(255, (int) (60 * alphaMultiplier)))).getRGB(),
                0.03f * alphaMultiplier,
                0.08f,
                1.00f,
                0.01f
        );

        // серый контур
        Render2D.outline(bgX, bgY, 420, 260, 0.8f,
                new Color(40, 44, 58, (int)(160 * alphaMultiplier)).getRGB(), 12);

        // мягкое свечение снаружи
        Render2D.outline(bgX - 1, bgY - 1, 422, 262, 2f,
                new Color(30, 35, 50, (int)(20 * alphaMultiplier)).getRGB(), 13);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int outlineAlpha = (int) (255 * alphaMultiplier);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15,
                new Color(8, 9, 14, (int)(200 * alphaMultiplier)).getRGB(), 8);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f,
                new Color(35, 40, 55, outlineAlpha).getRGB(), 8);

        Render2D.rect(bgX + 87.5f, bgY + 7.5f, 0.5f, bgHeight - 15,
                new Color(20, 24, 36, (int) (180 * alphaMultiplier)).getRGB(), 0);
    }
}
