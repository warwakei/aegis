package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.modules.impl.render.Hud;
import rich.util.render.font.Fonts;
import rich.util.tps.TPSCalculate;

import java.awt.*;

public class WatermarkTps extends AbstractHudElement {

    private String lastTps = "";
    private String oldTps = "";
    private long tpsAnimationStart = 0;

    private static final long ANIMATION_DURATION = 200;
    private static final float ANIMATION_OFFSET = 8.0f;

    public WatermarkTps() {
        super("TPS", 20, 28, 80, 24, true);
        startAnimation();
    }

    @Override
    public boolean visible() {
        return Hud.getInstance() != null && Hud.getInstance().showTps.isValue();
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * 255)));
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float x = getX();
        float y = getY();

        float tpsValue = 20.0f;
        if (TPSCalculate.getInstance() != null) {
            tpsValue = TPSCalculate.getInstance().getTpsRounded();
        }
        String tpsNumber = String.format("%.1f", tpsValue);
        String tpsText = "tps";

        long currentTime = System.currentTimeMillis();
        if (!tpsNumber.equals(lastTps)) {
            oldTps = lastTps;
            lastTps = tpsNumber;
            tpsAnimationStart = currentTime;
        }

        float tpsAnimation = Math.min(1.0f, (currentTime - tpsAnimationStart) / (float) ANIMATION_DURATION);

        float tpsNumberWidth = Fonts.BOLD.getWidth(tpsNumber, 6);
        float tpsTextWidth = Fonts.BOLD.getWidth(tpsText, 6);

        float boxWidth = 10 + 12 + 8 + tpsNumberWidth + 2 + tpsTextWidth + 10;
        setWidth((int) (boxWidth + 2));
        setHeight(22);

        float alphaFactor = alpha / 255.0f;

        HudStyle.panel(x, y + 3, boxWidth, 20, 5f, alphaFactor, HudStyle.Variant.DENSE);

        float textY = y + 7;
        Fonts.ICONSTYPETHO.draw("t", x + 5, textY + 0.5f, 12, new Color(230, 235, 245, 255).getRGB());

        float tpsOffsetX = x + 19;
        Fonts.TEST.draw(">", tpsOffsetX, textY + 1.5f, 8, new Color(160, 170, 190, 255).getRGB());
        tpsOffsetX += 8;

        drawAnimatedTextPerChar(tpsNumber, oldTps, tpsOffsetX, textY + 3, 6, tpsAnimation);
        tpsOffsetX += tpsNumberWidth + 2;

        Fonts.BOLD.draw(tpsText, tpsOffsetX, textY + 3, 6, new Color(160, 170, 190, 255).getRGB());
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

