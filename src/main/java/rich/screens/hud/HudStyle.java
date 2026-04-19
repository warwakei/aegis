package rich.screens.hud;

import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.render.Render2D;

import java.awt.*;

public final class HudStyle {

    private HudStyle() {}

    public static float alphaMulFrom255(int alpha255) {
        return Math.max(0f, Math.min(1f, alpha255 / 255.0f));
    }

    public static void panel(float x, float y, float w, float h, float radius, float alphaMul) {
        if (alphaMul <= 0.01f) return;

        int c0 = ClickGuiPalette.bgElevated(alphaMul);
        int c1 = ClickGuiPalette.bgMain(alphaMul);
        int c2 = ClickGuiPalette.bgMain(alphaMul);
        int c3 = ClickGuiPalette.bgElevated(alphaMul);

        Render2D.gradientRect(x, y, w, h, new int[]{c0, c1, c2, c3}, radius);

        // Subtle depth + "premium" vibe. Kept low so it doesn't distract in gameplay.
        Render2D.holoSheen(
                x, y, w, h,
                radius,
                new Color(255, 255, 255, (int) (255 * alphaMul)).getRGB(),
                0.30f * alphaMul,
                0.18f,
                1.05f,
                0.10f
        );

        Render2D.iridescentOutline(x, y, w, h, 0.9f, radius, 0.14f, 0.65f, 1.0f, 0.22f * alphaMul);
        Render2D.outline(x, y, w, h, 0.7f, ClickGuiPalette.border(alphaMul), radius);
    }

    public static void inset(float x, float y, float w, float h, float radius, float alphaMul) {
        if (alphaMul <= 0.01f) return;
        Render2D.rect(x, y, w, h, ClickGuiPalette.panelInset(alphaMul), radius);
        Render2D.outline(x, y, w, h, 0.5f, ClickGuiPalette.borderSubtle(alphaMul), radius);
    }
}

