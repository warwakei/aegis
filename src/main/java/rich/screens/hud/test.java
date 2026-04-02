package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.Render2D;
import rich.util.ColorUtil;

public class test extends AbstractHudElement {

    private float rotation = 0f;
    private float pulsePhase = 0f;
    private float toggleProgress = 0f;
    private boolean toggled = false;
    private float loadingProgress = 0f;
    private float hue = 0f;

    public test() {
        super("test", 10, 10, 340, 240, false);
    }

    @Override
    public void tick() {
        rotation += 3f;
        if (rotation >= 360f) rotation -= 360f;

        pulsePhase += 0.05f;
        if (pulsePhase >= (float)(Math.PI * 2)) pulsePhase -= (float)(Math.PI * 2);

        if (toggled) {
            if (toggleProgress < 1f) toggleProgress = Math.min(1f, toggleProgress + 0.05f);
        } else {
            if (toggleProgress > 0f) toggleProgress = Math.max(0f, toggleProgress - 0.05f);
        }

        loadingProgress += 0.02f;
        if (loadingProgress >= 1f) {
            loadingProgress = 0f;
            toggled = !toggled;
        }

        hue += 0.005f;
        if (hue >= 1f) hue -= 1f;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        float x = getX();
        float y = getY();

        int bgColor = ColorUtil.applyAlpha(0x80000000, alpha);
        Render2D.rect(x, y, width, height, bgColor, 8f);

        int borderColor = ColorUtil.applyAlpha(0xFF404040, alpha);
        Render2D.outline(x, y, width, height, 1f, borderColor, 8f);

        drawSpinnerArc(x + 30, y + 30, alpha);
        drawPulsingArc(x + 90, y + 30, alpha);
        drawToggleArc(x + 150, y + 30, alpha);
        drawLoadingArc(x + 210, y + 30, alpha);
        drawRainbowArc(x + 270, y + 30, alpha);

        drawSegmentedArc(x + 30, y + 90, alpha);
        drawDoubleArc(x + 90, y + 90, alpha);
        drawProgressRingArc(x + 150, y + 90, alpha);
        drawGradientSpinner(x + 210, y + 90, alpha);
        drawPieChart(x + 270, y + 90, alpha);

        drawCooldownArc(x + 30, y + 150, alpha);
        drawHealthRing(x + 90, y + 150, alpha);

        drawOutlinedSpinner(x + 150, y + 150, alpha);
        drawOutlinedProgress(x + 210, y + 150, alpha);
        drawOutlinedPulsing(x + 270, y + 150, alpha);

        drawOutlinedToggle(x + 30, y + 210, alpha);
        drawOutlinedRainbow(x + 90, y + 210, alpha);
        drawOutlinedDouble(x + 150, y + 210, alpha);
    }

    private void drawSpinnerArc(float cx, float cy, int alpha) {
        int color = ColorUtil.applyAlpha(0xFF00AAFF, alpha);
        Render2D.arc(cx, cy, 20f, 3f, 270f, rotation, color);
    }

    private void drawPulsingArc(float cx, float cy, int alpha) {
        float pulse = (float)(Math.sin(pulsePhase) * 0.3 + 0.7);
        int baseColor = 0xFF00FF88;
        int r = (int)(((baseColor >> 16) & 0xFF) * pulse);
        int g = (int)(((baseColor >> 8) & 0xFF) * pulse);
        int b = (int)((baseColor & 0xFF) * pulse);
        int color = ColorUtil.applyAlpha((0xFF << 24) | (r << 16) | (g << 8) | b, alpha);

        float thickness = 2f + pulse * 2f;
        Render2D.arc(cx, cy, 20f, thickness, 360f, 0f, color);
    }

    private void drawToggleArc(float cx, float cy, int alpha) {
        float degree = 90f + toggleProgress * 270f;

        int offColor = 0xFF666666;
        int onColor = 0xFF00FF00;

        int r = (int)(((offColor >> 16) & 0xFF) * (1 - toggleProgress) + ((onColor >> 16) & 0xFF) * toggleProgress);
        int g = (int)(((offColor >> 8) & 0xFF) * (1 - toggleProgress) + ((onColor >> 8) & 0xFF) * toggleProgress);
        int b = (int)((offColor & 0xFF) * (1 - toggleProgress) + (onColor & 0xFF) * toggleProgress);
        int color = ColorUtil.applyAlpha((0xFF << 24) | (r << 16) | (g << 8) | b, alpha);

        Render2D.arc(cx, cy, 20f, 4f, degree, -90f, color);

        int bgColor = ColorUtil.applyAlpha(0x40FFFFFF, alpha);
        Render2D.arc(cx, cy, 20f, 2f, 360f, 0f, bgColor);
    }

    private void drawLoadingArc(float cx, float cy, int alpha) {
        int bgColor = ColorUtil.applyAlpha(0x40FFFFFF, alpha);
        Render2D.arc(cx, cy, 20f, 3f, 360f, 0f, bgColor);

        int fillColor = ColorUtil.applyAlpha(0xFFFFAA00, alpha);
        float degree = loadingProgress * 360f;
        Render2D.arc(cx, cy, 20f, 3f, degree, -90f, fillColor);
    }

    private void drawRainbowArc(float cx, float cy, int alpha) {
        int color1 = ColorUtil.applyAlpha(hsvToRgb(hue, 1f, 1f), alpha);
        int color2 = ColorUtil.applyAlpha(hsvToRgb((hue + 0.33f) % 1f, 1f, 1f), alpha);
        int color3 = ColorUtil.applyAlpha(hsvToRgb((hue + 0.66f) % 1f, 1f, 1f), alpha);

        Render2D.arc(cx, cy, 20f, 4f, 360f, rotation, color1, color2, color3);
    }

    private void drawSegmentedArc(float cx, float cy, int alpha) {
        int[] colors = {
                ColorUtil.applyAlpha(0xFFFF0000, alpha),
                ColorUtil.applyAlpha(0xFFFF8800, alpha),
                ColorUtil.applyAlpha(0xFFFFFF00, alpha),
                ColorUtil.applyAlpha(0xFF00FF00, alpha)
        };

        for (int i = 0; i < 4; i++) {
            float segmentRotation = i * 90f + rotation * 0.5f;
            Render2D.arc(cx, cy, 20f, 3f, 80f, segmentRotation, colors[i]);
        }
    }

    private void drawDoubleArc(float cx, float cy, int alpha) {
        int outerColor = ColorUtil.applyAlpha(0xFF8800FF, alpha);
        Render2D.arc(cx, cy, 20f, 2f, 180f, rotation, outerColor);

        int innerColor = ColorUtil.applyAlpha(0xFFFF0088, alpha);
        Render2D.arc(cx, cy, 14f, 2f, 180f, -rotation * 1.5f, innerColor);
    }

    private void drawProgressRingArc(float cx, float cy, int alpha) {
        int bgColor = ColorUtil.applyAlpha(0x30FFFFFF, alpha);
        Render2D.arc(cx, cy, 20f, 6f, 360f, 0f, bgColor);

        float progress = loadingProgress;
        int progressColor = ColorUtil.applyAlpha(0xFF00DDFF, alpha);
        Render2D.arc(cx, cy, 20f, 6f, progress * 360f, -90f, progressColor);

        int glowColor = ColorUtil.applyAlpha(0x6000DDFF, alpha);
        Render2D.arc(cx, cy, 22f, 2f, progress * 360f, -90f, glowColor);
    }

    private void drawGradientSpinner(float cx, float cy, int alpha) {
        int startColor = ColorUtil.applyAlpha(0xFFFF0000, alpha);
        int midColor = ColorUtil.applyAlpha(0xFFFFFF00, alpha);
        int endColor = ColorUtil.applyAlpha(0xFF00FF00, alpha);

        Render2D.arc(cx, cy, 16f, 4f, 300f, rotation,
                startColor, midColor, endColor,
                midColor, endColor, startColor,
                endColor, startColor, midColor);
    }

    private void drawPieChart(float cx, float cy, int alpha) {
        float[] values = {0.35f, 0.25f, 0.25f, 0.15f};
        int[] colors = {
                ColorUtil.applyAlpha(0xFFFF4444, alpha),
                ColorUtil.applyAlpha(0xFF44FF44, alpha),
                ColorUtil.applyAlpha(0xFF4444FF, alpha),
                ColorUtil.applyAlpha(0xFFFFFF44, alpha)
        };

        float currentRotation = -90f;
        for (int i = 0; i < values.length; i++) {
            float degree = values[i] * 360f;
            Render2D.arc(cx, cy, 18f, 18f, degree - 2f, currentRotation, colors[i]);
            currentRotation += degree;
        }
    }

    private void drawCooldownArc(float cx, float cy, int alpha) {
        float cooldown = 1f - loadingProgress;

        int readyColor = ColorUtil.applyAlpha(0xFF00FF00, alpha);
        int cooldownColor = ColorUtil.applyAlpha(0xFF888888, alpha);

        Render2D.arc(cx, cy, 18f, 18f, 360f, -90f, cooldownColor);

        if (cooldown < 1f) {
            float degree = (1f - cooldown) * 360f;
            Render2D.arc(cx, cy, 18f, 18f, degree, -90f, readyColor);
        }

        int borderColor = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arc(cx, cy, 18f, 1f, 360f, 0f, borderColor);
    }

    private void drawHealthRing(float cx, float cy, int alpha) {
        float health = 0.5f + (float)Math.sin(pulsePhase) * 0.3f;

        int bgColor = ColorUtil.applyAlpha(0x40FF0000, alpha);
        Render2D.arc(cx, cy, 18f, 5f, 360f, -90f, bgColor);

        int healthColor;
        if (health > 0.6f) {
            healthColor = ColorUtil.applyAlpha(0xFF00FF00, alpha);
        } else if (health > 0.3f) {
            healthColor = ColorUtil.applyAlpha(0xFFFFAA00, alpha);
        } else {
            healthColor = ColorUtil.applyAlpha(0xFFFF0000, alpha);
        }

        float degree = health * 360f;
        Render2D.arc(cx, cy, 18f, 5f, degree, -90f, healthColor);

        if (health < 0.3f) {
            float pulse = (float)(Math.sin(pulsePhase * 4) * 0.5 + 0.5);
            int pulseColor = ColorUtil.applyAlpha((int)(0x60 * pulse) << 24 | 0xFF0000, alpha);
            Render2D.arc(cx, cy, 20f, 2f, degree, -90f, pulseColor);
        }
    }

    private void drawOutlinedSpinner(float cx, float cy, int alpha) {
        int fillColor = ColorUtil.applyAlpha(0xFF00AAFF, alpha);
        int outlineColor = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, 4f, 270f, rotation, 1.5f, fillColor, outlineColor);
    }

    private void drawOutlinedProgress(float cx, float cy, int alpha) {
        int bgFill = ColorUtil.applyAlpha(0x40FFFFFF, alpha);
        int bgOutline = ColorUtil.applyAlpha(0x80FFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, 5f, 360f, 0f, 1f, bgFill, bgOutline);

        float degree = loadingProgress * 360f;
        int fillColor = ColorUtil.applyAlpha(0xFF00FF88, alpha);
        int outlineColor = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, 5f, degree, -90f, 1.5f, fillColor, outlineColor);
    }

    private void drawOutlinedPulsing(float cx, float cy, int alpha) {
        float pulse = (float)(Math.sin(pulsePhase) * 0.3 + 0.7);
        float thickness = 3f + pulse * 2f;

        int fillColor = ColorUtil.applyAlpha(0xFFFF6600, alpha);
        int outlineColor = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, thickness, 360f, 0f, 1f, fillColor, outlineColor);
    }

    private void drawOutlinedToggle(float cx, float cy, int alpha) {
        float degree = 90f + toggleProgress * 270f;

        int offFill = 0xFF444444;
        int onFill = 0xFF00DD00;
        int r = (int)(((offFill >> 16) & 0xFF) * (1 - toggleProgress) + ((onFill >> 16) & 0xFF) * toggleProgress);
        int g = (int)(((offFill >> 8) & 0xFF) * (1 - toggleProgress) + ((onFill >> 8) & 0xFF) * toggleProgress);
        int b = (int)((offFill & 0xFF) * (1 - toggleProgress) + (onFill & 0xFF) * toggleProgress);
        int fillColor = ColorUtil.applyAlpha((0xFF << 24) | (r << 16) | (g << 8) | b, alpha);

        int outlineColor = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, 5f, degree, -90f, 2f, fillColor, outlineColor);
    }

    private void drawOutlinedRainbow(float cx, float cy, int alpha) {
        int fillColor = ColorUtil.applyAlpha(hsvToRgb(hue, 0.8f, 1f), alpha);
        int outlineColor = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, 4f, 300f, rotation, 1.5f, fillColor, outlineColor);
    }

    private void drawOutlinedDouble(float cx, float cy, int alpha) {
        int outerFill = ColorUtil.applyAlpha(0xFF8800FF, alpha);
        int outerOutline = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 20f, 3f, 180f, rotation, 1f, outerFill, outerOutline);

        int innerFill = ColorUtil.applyAlpha(0xFFFF0088, alpha);
        int innerOutline = ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        Render2D.arcOutline(cx, cy, 12f, 3f, 180f, -rotation * 1.5f, 1f, innerFill, innerOutline);
    }

    private int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;

        float r, g, b;
        if (h < 1f/6f) { r = c; g = x; b = 0; }
        else if (h < 2f/6f) { r = x; g = c; b = 0; }
        else if (h < 3f/6f) { r = 0; g = c; b = x; }
        else if (h < 4f/6f) { r = 0; g = x; b = c; }
        else if (h < 5f/6f) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        int ri = (int)((r + m) * 255);
        int gi = (int)((g + m) * 255);
        int bi = (int)((b + m) * 255);

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    public void toggle() {
        toggled = !toggled;
    }

    public boolean isToggled() {
        return toggled;
    }
}