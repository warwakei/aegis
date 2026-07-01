package rich.screens.hud;

import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.render.Render2D;

import java.awt.*;

public final class HudStyle {

    public enum Variant {
        DEFAULT,
        SOFT,
        ACCENT,
        DENSE,
        GLOW,
        AMOLED
    }

    private HudStyle() {}

    public static float alphaMulFrom255(int alpha255) {
        return Math.max(0f, Math.min(1f, alpha255 / 255.0f));
    }

    public static void panel(float x, float y, float w, float h, float radius, float alphaMul) {
        panel(x, y, w, h, radius, alphaMul, Variant.DEFAULT);
    }

    public static void panel(float x, float y, float w, float h, float radius, float alphaMul, Variant variant) {
        if (alphaMul <= 0.01f) return;

        int baseMain = ClickGuiPalette.bgMain(alphaMul);
        int baseElevated = ClickGuiPalette.bgElevated(alphaMul);

        int c0 = baseElevated;
        int c1 = baseMain;
        int c2 = baseMain;
        int c3 = baseElevated;
        int border = ClickGuiPalette.border(alphaMul);

        if (variant == Variant.SOFT) {
            c0 = brighten(baseElevated, 0.05f);
            c1 = brighten(baseMain, 0.03f);
            c2 = brighten(baseMain, 0.01f);
            c3 = brighten(baseElevated, 0.02f);
            border = ClickGuiPalette.borderSubtle(alphaMul);
        } else if (variant == Variant.ACCENT) {
            c0 = tint(baseElevated, 0x6A8CFF, 0.15f);
            c1 = tint(baseMain, 0x2EE6D6, 0.10f);
            c2 = tint(baseMain, 0xE97BFF, 0.10f);
            c3 = tint(baseElevated, 0x6A8CFF, 0.13f);
            border = tint(ClickGuiPalette.border(alphaMul), 0x82B0FF, 0.30f);
        } else if (variant == Variant.DENSE) {
            c0 = darken(baseElevated, 0.20f);
            c1 = darken(baseMain, 0.18f);
            c2 = darken(baseMain, 0.12f);
            c3 = darken(baseElevated, 0.10f);
            border = tint(ClickGuiPalette.border(alphaMul), 0x5280B8, 0.15f);
        } else if (variant == Variant.GLOW) {
            c0 = tint(baseElevated, 0x6A8CFF, 0.08f);
            c1 = tint(baseMain, 0x6A8CFF, 0.05f);
            c2 = tint(baseMain, 0xB877D8, 0.05f);
            c3 = tint(baseElevated, 0x3ECFBA, 0.06f);
            border = tint(ClickGuiPalette.border(alphaMul), 0x6A9CFF, 0.35f);
        } else if (variant == Variant.AMOLED) {
            c0 = tint(baseElevated, 0x6A9CFF, 0.04f);
            c1 = baseMain;
            c2 = baseMain;
            c3 = tint(baseElevated, 0x3ECFBA, 0.03f);
            border = tint(ClickGuiPalette.border(alphaMul), 0x6A9CFF, 0.18f);
        }

        Render2D.gradientRect(x, y, w, h, new int[]{c0, c1, c2, c3}, radius);
        Render2D.outline(x, y, w, h, 0.8f, border, radius);

        // Glow variant has an extra soft glow border
        if (variant == Variant.GLOW) {
            Render2D.outline(x - 1, y - 1, w + 2, h + 2, 2.5f, ClickGuiPalette.accentGlow(alphaMul * 0.5f), radius + 1);
        }
    }

    public static void inset(float x, float y, float w, float h, float radius, float alphaMul) {
        inset(x, y, w, h, radius, alphaMul, Variant.DEFAULT);
    }

    public static void inset(float x, float y, float w, float h, float radius, float alphaMul, Variant variant) {
        if (alphaMul <= 0.01f) return;
        int inset = ClickGuiPalette.panelInset(alphaMul);
        int border = ClickGuiPalette.borderSubtle(alphaMul);

        if (variant == Variant.SOFT) {
            inset = brighten(inset, 0.06f);
        } else if (variant == Variant.ACCENT) {
            inset = tint(inset, 0x7DA7FF, 0.15f);
            border = tint(border, 0x7DA7FF, 0.28f);
        } else if (variant == Variant.DENSE) {
            inset = darken(inset, 0.16f);
            border = darken(border, 0.14f);
        } else if (variant == Variant.GLOW) {
            inset = tint(inset, 0x7DA7FF, 0.10f);
            border = tint(border, 0x6A9CFF, 0.25f);
        } else if (variant == Variant.AMOLED) {
            inset = tint(inset, 0x6A9CFF, 0.06f);
            border = tint(border, 0x6A9CFF, 0.14f);
        }

        Render2D.rect(x, y, w, h, inset, radius);
        Render2D.outline(x, y, w, h, 0.6f, border, radius);
    }

    private static int brighten(int color, float amount) {
        return shift(color, amount);
    }

    private static int darken(int color, float amount) {
        return shift(color, -amount);
    }

    private static int shift(int color, float amount) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        int rr = clamp((int) (r + 255f * amount));
        int gg = clamp((int) (g + 255f * amount));
        int bb = clamp((int) (b + 255f * amount));
        return (a << 24) | (rr << 16) | (gg << 8) | bb;
    }

    private static int tint(int color, int tintRgb, float strength) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        int tr = (tintRgb >>> 16) & 0xFF;
        int tg = (tintRgb >>> 8) & 0xFF;
        int tb = tintRgb & 0xFF;

        int rr = clamp((int) (r + (tr - r) * strength));
        int gg = clamp((int) (g + (tg - g) * strength));
        int bb = clamp((int) (b + (tb - b) * strength));
        return (a << 24) | (rr << 16) | (gg << 8) | bb;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
