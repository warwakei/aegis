package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);

        int c0 = new Color(16, 17, 22, baseAlpha).getRGB();
        int c1 = new Color(12, 13, 18, baseAlpha).getRGB();
        int c2 = new Color(10, 11, 16, baseAlpha).getRGB();
        int c3 = new Color(14, 15, 21, baseAlpha).getRGB();

        Render2D.gradientRect(bgX, bgY, 400, 250,
                new int[]{c0, c1, c2, c3}, 12);

        Render2D.outline(bgX, bgY, 400, 250, 1f, ClickGuiPalette.border(alphaMultiplier), 12);

        float lineY = bgY + 1.5f;
        Render2D.rect(bgX + 14, lineY, 400 - 28, 1f, ClickGuiPalette.accentLine(alphaMultiplier), 0);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int outlineAlpha = (int) (255 * alphaMultiplier);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, ClickGuiPalette.panelInset(alphaMultiplier), 8);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f,
                new Color(34, 38, 48, outlineAlpha).getRGB(), 8);

        Render2D.rect(bgX + 87.5f, bgY + 7.5f, 0.5f, bgHeight - 15,
                new Color(28, 32, 40, (int) (180 * alphaMultiplier)).getRGB(), 0);

        Render2D.outline(bgX + 12.5f, bgY + 220.5f, 70, 17, 0.5f,
                new Color(40, 44, 54, outlineAlpha).getRGB(), 5);

        Fonts.GUI_ICONS.draw("X", bgX + 21.15f, bgY + 217.5f, 19, new Color(55, 58, 68, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Y", bgX + 40f, bgY + 217f, 20, new Color(55, 58, 68, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Z", bgX + 60f, bgY + 217f, 20, new Color(55, 58, 68, outlineAlpha).getRGB());

        Render2D.blur(bgX + 12.5f, bgY + 220.5f, 70, 17, 3.5f, 5,
                new Color(16, 18, 24, (int) (120 * alphaMultiplier)).getRGB());

        float textSize = 6f;
        String soonText = "Soon...";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = bgX + 12.5f + (70 - textWidth) / 2f;
        float centerY = bgY + 220.5f + (17 - textHeight) / 2f;
        Fonts.BOLD.draw(soonText, centerX, centerY, textSize,
                new Color(95, 98, 110, (int) (200 * alphaMultiplier)).getRGB());
    }
}
