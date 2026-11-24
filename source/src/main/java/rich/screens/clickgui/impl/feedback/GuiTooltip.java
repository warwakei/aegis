package rich.screens.clickgui.impl.feedback;

import net.minecraft.client.gui.DrawContext;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Ховер-тултип для настроек: показывается после задержки, пока курсор
 * стоит на строке настройки. Рисуется последним, поверх тостов и панелей.
 */
public class GuiTooltip {

    private static final float TEXT_SIZE = 5f;
    private static final float PADDING_X = 6f;
    private static final float PADDING_Y = 4f;
    private static final float LINE_HEIGHT = 7f;
    private static final float MAX_WIDTH = 160f;
    private static final long SHOW_DELAY_MS = 550;
    private static final long FADE_MS = 140;

    private String currentText = null;
    private long hoverStartTime = 0;
    private float alpha = 0f;
    private long lastUpdateTime = System.currentTimeMillis();

    /**
     * @param text описание под курсором или null, если ховера нет
     * @param anchorX правый край панели настроек — тултип рисуется слева от него
     * @param rowY верх строки настройки
     */
    public void render(DrawContext context, String text, float anchorX, float rowY,
                       float mouseX, float mouseY, float alphaMultiplier) {
        long now = System.currentTimeMillis();
        float deltaTime = Math.min((now - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = now;

        if (text == null || text.isEmpty()) {
            currentText = null;
            alpha = 0f;
            return;
        }

        if (!text.equals(currentText)) {
            currentText = text;
            hoverStartTime = now;
            alpha = 0f;
        }

        boolean shouldShow = now - hoverStartTime >= SHOW_DELAY_MS;
        float target = shouldShow ? 1f : 0f;
        alpha += (target - alpha) * Math.min(1f, deltaTime * 14f);

        float finalAlpha = alpha * alphaMultiplier;
        if (finalAlpha <= 0.02f) return;

        List<String> lines = wrap(text, MAX_WIDTH - PADDING_X * 2);
        float width = 0;
        for (String line : lines) {
            width = Math.max(width, Fonts.BOLD.getWidth(line, TEXT_SIZE));
        }
        width += PADDING_X * 2;
        float height = lines.size() * LINE_HEIGHT + PADDING_Y * 2 - 2;

        // слева от правого края панели, над курсором; не вылезаем за экран
        float x = anchorX - width - 4;
        float y = rowY - height - 3;
        if (y < 2) y = rowY + 14;

        float slideY = (1f - alpha) * 3f;
        y += slideY;

        Render2D.rect(x, y, width, height, new Color(12, 14, 24, clamp((int) (240 * finalAlpha))).getRGB(), 4);
        Render2D.outline(x, y, width, height, 0.5f, new Color(82, 128, 184, clamp((int) (120 * finalAlpha))).getRGB(), 4);

        float textY = y + PADDING_Y;
        for (String line : lines) {
            Fonts.BOLD.draw(line, x + PADDING_X, textY, TEXT_SIZE,
                    new Color(200, 208, 226, clamp((int) (230 * finalAlpha))).getRGB());
            textY += LINE_HEIGHT;
        }
    }

    public void reset() {
        currentText = null;
        alpha = 0f;
    }

    private List<String> wrap(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (Fonts.BOLD.getWidth(candidate, TEXT_SIZE) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}