package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);

        long currentTime = System.currentTimeMillis();
        // Более плавная и естественная пульсация
        float pulse = (float) Math.sin(currentTime * 0.0012) * 0.08f + 0.92f;

        // Улучшенные цвета фона - более глубокие и насыщенные
        int r1 = (int)(24 * pulse);
        int g1 = (int)(27 * pulse);
        int b1 = (int)(35 * pulse);

        int r2 = (int)(8 * pulse);
        int g2 = (int)(8 * pulse);
        int b2 = (int)(14 * pulse);

        // 5-цветный градиент для более плавного перехода
        int[] gradientColors = {
                new Color(r1 + 6, g1 + 6, b1 + 10, baseAlpha).getRGB(),
                new Color(r1 + 2, g1 + 3, b1 + 6, baseAlpha).getRGB(),
                new Color(r2 + 4, g2 + 4, b2 + 8, baseAlpha).getRGB(),
                new Color(r2, g2, b2 + 4, baseAlpha).getRGB(),
                new Color(r1, g1, b1 + 4, baseAlpha).getRGB()
        };

        Render2D.gradientRect(bgX, bgY, 400, 250, gradientColors, 15);

        // Анимированная градиентная полоса сверху
        renderAnimatedTopBar(bgX, bgY, alphaMultiplier, currentTime);

        // Улучшенный edge glow - более мягкий и плавный
        float edgeGlowSize = 3f;
        int edgeAlpha = (int)(12 * alphaMultiplier);

        // Верхний edge glow
        Render2D.gradientRect(bgX, bgY, 400, edgeGlowSize,
                new int[]{
                        new Color(50, 70, 110, 0).getRGB(),
                        new Color(50, 70, 110, edgeAlpha).getRGB(),
                        new Color(60, 85, 130, edgeAlpha).getRGB(),
                        new Color(50, 70, 110, 0).getRGB()
                }, 0);

        // Нижний edge glow
        int bottomEdgeAlpha = (int)(8 * alphaMultiplier);
        Render2D.gradientRect(bgX, bgY + 250 - edgeGlowSize, 400, edgeGlowSize,
                new int[]{
                        new Color(40, 55, 80, 0).getRGB(),
                        new Color(40, 55, 80, bottomEdgeAlpha).getRGB(),
                        new Color(50, 65, 95, bottomEdgeAlpha).getRGB(),
                        new Color(40, 55, 80, 0).getRGB()
                }, 0);

        // Левый edge glow (тонкий)
        float sideGlowSize = 1.5f;
        int leftEdgeAlpha = (int)(10 * alphaMultiplier);
        Render2D.gradientRect(bgX, bgY, sideGlowSize, 250,
                new int[]{
                        new Color(50, 70, 110, 0).getRGB(),
                        new Color(50, 70, 110, leftEdgeAlpha).getRGB(),
                        new Color(50, 70, 110, leftEdgeAlpha).getRGB(),
                        new Color(50, 70, 110, 0).getRGB()
                }, 0);

        // Правый edge glow
        Render2D.gradientRect(bgX + 400 - sideGlowSize, bgY, sideGlowSize, 250,
                new int[]{
                        new Color(50, 70, 110, 0).getRGB(),
                        new Color(50, 70, 110, leftEdgeAlpha).getRGB(),
                        new Color(50, 70, 110, leftEdgeAlpha).getRGB(),
                        new Color(50, 70, 110, 0).getRGB()
                }, 0);
    }

    private void renderAnimatedTopBar(float bgX, float bgY, float alphaMultiplier, long currentTime) {
        // Градиентная полоса сверху с анимацией
        int barHeight = 1;
        float barY = bgY + 2.5f;

        // Анимированный градиент
        float animOffset = (currentTime % 3000) / 3000f;

        int startColor = new Color(80, 120, 200, (int)(40 * alphaMultiplier)).getRGB();
        int midColor = new Color(140, 180, 255, (int)(60 * alphaMultiplier)).getRGB();
        int endColor = new Color(80, 120, 200, (int)(40 * alphaMultiplier)).getRGB();

        // Разделяем полосу на сегменты для анимации
        int segmentCount = 4;
        float segmentWidth = 400f / segmentCount;

        for (int i = 0; i < segmentCount; i++) {
            float segX = bgX + i * segmentWidth;
            float localT = (i / (float)segmentCount + animOffset) % 1f;

            int segColor;
            if (localT < 0.5f) {
                segColor = blendColors(startColor, midColor, localT * 2f);
            } else {
                segColor = blendColors(midColor, endColor, (localT - 0.5f) * 2f);
            }

            Render2D.rect(segX, barY, segmentWidth, barHeight, segColor, 0);
        }
    }

    private int blendColors(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        int a = (int)(a1 + (a2 - a1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int panelAlpha = (int) (22 * alphaMultiplier); // Чуть меньше
        int outlineAlpha = (int) (255 * alphaMultiplier);
        int blurAlpha = (int) (140 * alphaMultiplier); // Меньше blur

        // Улучшенный фон панели - чуть темнее и с более мягким оттенком
        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, new Color(20, 20, 24, panelAlpha).getRGB(), 10);

        // Более аккуратный аутлайн
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f, new Color(45, 45, 52, outlineAlpha).getRGB(), 10);

        // Нижний блок XYZ
        Render2D.outline(bgX + 12.5f, bgY + 220.5f, 70, 17, 0.5f, new Color(45, 45, 52, outlineAlpha).getRGB(), 5);

        Fonts.GUI_ICONS.draw("X", bgX + 21.15f, bgY + 217.5f, 19, new Color(50, 50, 56, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Y", bgX + 40f, bgY + 217f, 20, new Color(50, 50, 56, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Z", bgX + 60f, bgY + 217f, 20, new Color(50, 50, 56, outlineAlpha).getRGB());

        // Улучшенный blur - чуть меньше и мягче
        Render2D.blur(bgX + 12.5f, bgY + 220.5f, 70, 17, 3.5f, 5, new Color(22, 22, 26, blurAlpha).getRGB());

        float textSize = 6f;
        String soonText = "Soon...";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = bgX + 12.5f + (70 - textWidth) / 2f;
        float centerY = bgY + 220.5f + (17 - textHeight) / 2f;
        Fonts.BOLD.draw(soonText, centerX, centerY, textSize, new Color(140, 140, 148, (int) (190 * alphaMultiplier)).getRGB());
    }
}
