package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.modules.impl.render.Hud;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.tps.TPSCalculate;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends AbstractHudElement {

    private String lastFps = "";
    private String oldFps = "";
    private long fpsAnimationStart = 0;

    private String lastTime = "";
    private String oldTime = "";
    private long timeAnimationStart = 0;

    private String lastTps = "";
    private String oldTps = "";
    private long tpsAnimationStart = 0;

    private static final long ANIMATION_DURATION = 200;
    private static final float ANIMATION_OFFSET = 8.0f;

    public Watermark() {
        super("Watermark", 10, 10, 200, 24, false);
        startAnimation();
    }

    @Override
    public void tick() {
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * 255)));
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float x = 20;
        float y = 5;

        String username = mc.getSession().getUsername();
        String fpsNumber = String.valueOf(mc.getCurrentFps());
        String fpsText = "fps";
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        boolean showTps = Hud.getInstance() != null && Hud.getInstance().showTps.isValue();

        float tpsValue = 20.0f;
        if (TPSCalculate.getInstance() != null) {
            tpsValue = TPSCalculate.getInstance().getTpsRounded();
        }
        String tpsNumber = String.format("%.1f", tpsValue);
        String tpsText = "tps";

        long currentTime = System.currentTimeMillis();

        if (!fpsNumber.equals(lastFps)) {
            oldFps = lastFps;
            lastFps = fpsNumber;
            fpsAnimationStart = currentTime;
        }

        if (!time.equals(lastTime)) {
            oldTime = lastTime;
            lastTime = time;
            timeAnimationStart = currentTime;
        }

        if (!tpsNumber.equals(lastTps)) {
            oldTps = lastTps;
            lastTps = tpsNumber;
            tpsAnimationStart = currentTime;
        }

        float fpsAnimation = Math.min(1.0f, (currentTime - fpsAnimationStart) / (float) ANIMATION_DURATION);
        float timeAnimation = Math.min(1.0f, (currentTime - timeAnimationStart) / (float) ANIMATION_DURATION);
        float tpsAnimation = Math.min(1.0f, (currentTime - tpsAnimationStart) / (float) ANIMATION_DURATION);

        float usernameWidth = Fonts.BOLD.getWidth(username, 6);
        float fpsNumberWidth = Fonts.BOLD.getWidth(fpsNumber, 6);
        float fpsTextWidth = Fonts.BOLD.getWidth(fpsText, 6);
        float timeWidth = Fonts.BOLD.getWidth(time, 6);
        float tpsNumberWidth = Fonts.BOLD.getWidth(tpsNumber, 6);
        float tpsTextWidth = Fonts.BOLD.getWidth(tpsText, 6);

        float totalWidth = usernameWidth + fpsNumberWidth + fpsTextWidth + timeWidth + 97;
        float tpsBoxWidth = 10 + 12 + 8 + tpsNumberWidth + 2 + tpsTextWidth + 10;

        if (showTps) {
            setWidth((int) (totalWidth + tpsBoxWidth + 10));
        } else {
            setWidth((int) (totalWidth + 10));
        }
        setHeight(22);

        Render2D.gradientRect(x, y + 3, totalWidth, 20,
                new int[]{
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(22, 22, 22, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(22, 22, 22, 255).getRGB()
                },
                5);

        Render2D.outline(x, y + 3, totalWidth, 20, 0.35f, new Color(90, 90, 90, 255).getRGB(), 5);

        float tpsBoxX = x + totalWidth;

        if (showTps) {
            Render2D.gradientRect(tpsBoxX, y + 3, tpsBoxWidth, 20,
                    new int[]{
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(22, 22, 22, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(22, 22, 22, 255).getRGB()
                    },
                    5);

            Render2D.outline(tpsBoxX, y + 3, tpsBoxWidth, 20, 0.35f, new Color(90, 90, 90, 255).getRGB(), 5);
        }

        float textY = y + 7;
        float textX = x;

        float offsetX = textX + 5;

        // Иконка пользователя - центрируем по вертикали
        Fonts.CATEGORY_ICONS.draw("d", offsetX, textY + 1.5f, 10, new Color(225, 225, 225, 255).getRGB());
        offsetX += 12;

        Fonts.BOLD.draw(username, offsetX, textY + 3, 6, new Color(255, 255, 255, 255).getRGB());
        offsetX += usernameWidth + 5;

        Fonts.TEST.draw("»", offsetX, textY + 1.5f, 8, new Color(155, 155, 155, 255).getRGB());
        offsetX += 12;

        // Иконка FPS
        Fonts.CATEGORY_ICONS.draw("b", offsetX, textY + 2.5f, 9, new Color(225, 225, 225, 255).getRGB());
        offsetX += 12;

        float fpsOffsetX = offsetX;
        drawAnimatedTextPerChar(fpsNumber, oldFps, fpsOffsetX, textY + 3, 6, fpsAnimation);
        offsetX += fpsNumberWidth + 2;

        Fonts.BOLD.draw(fpsText, offsetX, textY + 3, 6, new Color(155, 155, 155, 255).getRGB());
        offsetX += fpsTextWidth + 5;

        Fonts.TEST.draw("»", offsetX, textY + 1.5f, 8, new Color(155, 155, 155, 255).getRGB());
        offsetX += 12;

        // Иконка времени
        Fonts.CATEGORY_ICONS.draw("n", offsetX, textY + 2.5f, 9, new Color(225, 225, 225, 255).getRGB());
        offsetX += 12;

        float timeOffsetX = offsetX;
        drawAnimatedTextPerChar(time, oldTime, timeOffsetX, textY + 3, 6, timeAnimation);

        if (showTps) {
            // Иконка TPS - центрируем
            Fonts.ICONSTYPETHO.draw("t", tpsBoxX + 5, textY + 0.5f, 12, new Color(225, 225, 225, 255).getRGB());

            float tpsOffsetX = tpsBoxX + 19;

            Fonts.TEST.draw("»", tpsOffsetX, textY + 1.5f, 8, new Color(155, 155, 155, 255).getRGB());
            tpsOffsetX += 8;

            drawAnimatedTextPerChar(tpsNumber, oldTps, tpsOffsetX, textY + 3, 6, tpsAnimation);
            tpsOffsetX += tpsNumberWidth + 2;

            Fonts.BOLD.draw(tpsText, tpsOffsetX, textY + 3, 6, new Color(155, 155, 155, 255).getRGB());
        }
    }

    private void drawAnimatedTextPerChar(String newText, String oldText, float x, float y, float size, float progress) {
        if (oldText.isEmpty() || progress >= 1.0f) {
            Fonts.BOLD.draw(newText, x, y, size, new Color(255, 255, 255, 255).getRGB());
            return;
        }

        float offsetX = x;
        int maxLen = Math.max(newText.length(), oldText.length());

        String paddedNew = padLeft(newText, maxLen);
        String paddedOld = padLeft(oldText, maxLen);

        for (int i = 0; i < paddedNew.length(); i++) {
            char newChar = paddedNew.charAt(i);
            char oldChar = paddedOld.charAt(i);

            if (newChar == ' ' && oldChar == ' ') {
                continue;
            }

            float charWidth = Fonts.BOLD.getWidth(String.valueOf(newChar != ' ' ? newChar : oldChar), size);

            boolean isNewDigit = Character.isDigit(newChar) || newChar == '.';
            boolean isOldDigit = Character.isDigit(oldChar) || oldChar == '.';
            boolean hasChanged = newChar != oldChar;

            if (!hasChanged || (!isNewDigit && !isOldDigit)) {
                if (newChar != ' ') {
                    Fonts.BOLD.draw(String.valueOf(newChar), offsetX, y, size, new Color(255, 255, 255, 255).getRGB());
                }
            } else {
                float easedProgress = easeOutCubic(progress);

                if (oldChar != ' ' && isOldDigit) {
                    float oldAlpha = 1.0f - easedProgress;
                    float oldOffsetY = easedProgress * ANIMATION_OFFSET;
                    int oldAlphaClamped = clampAlpha(oldAlpha);
                    if (oldAlphaClamped > 0) {
                        int oldColor = new Color(255, 255, 255, oldAlphaClamped).getRGB();
                        Fonts.BOLD.draw(String.valueOf(oldChar), offsetX, y + oldOffsetY, size, oldColor);
                    }
                }

                if (newChar != ' ' && isNewDigit) {
                    float newAlpha = easedProgress;
                    float newOffsetY = (1.0f - easedProgress) * -ANIMATION_OFFSET;
                    int newAlphaClamped = clampAlpha(newAlpha);
                    if (newAlphaClamped > 0) {
                        int newColor = new Color(255, 255, 255, newAlphaClamped).getRGB();
                        Fonts.BOLD.draw(String.valueOf(newChar), offsetX, y + newOffsetY, size, newColor);
                    }
                }
            }

            if (newChar != ' ') {
                offsetX += charWidth;
            }
        }
    }

    private String padLeft(String text, int length) {
        if (text.length() >= length) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - text.length(); i++) {
            sb.append(' ');
        }
        sb.append(text);
        return sb.toString();
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }
}