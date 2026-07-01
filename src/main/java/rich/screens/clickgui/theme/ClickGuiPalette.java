package rich.screens.clickgui.theme;

import java.awt.*;

/**
 * Единая палитра ClickGUI: монолитный тёмный корпус с холодным акцентом.
 */
public final class ClickGuiPalette {

    private ClickGuiPalette() {}

    public static final int ACCENT = 0xFF5280B8;
    public static final int ACCENT_DIM = 0xFF3D5A8C;
    public static final int ACCENT_GLOW = 0xFF6A9CFF;
    public static final int ACCENT_WARM = 0xFFB877D8;
    public static final int ACCENT_TEAL = 0xFF3ECFBA;
    public static final int SURFACE_DARK = 0xFF0A0B10;
    public static final int SURFACE_CARD = 0xFF141620;

    public static int bgMain(float alphaMul) {
        int a = (int) (255 * alphaMul);
        return new Color(5, 6, 12, a).getRGB();
    }

    public static int bgElevated(float alphaMul) {
        int a = (int) (255 * alphaMul);
        return new Color(10, 11, 18, a).getRGB();
    }

    public static int border(float alphaMul) {
        int a = (int) (180 * alphaMul);
        return new Color(28, 32, 46, a).getRGB();
    }

    public static int borderSubtle(float alphaMul) {
        int a = (int) (110 * alphaMul);
        return new Color(20, 24, 38, a).getRGB();
    }

    public static int panelInset(float alphaMul) {
        int a = (int) (55 * alphaMul);
        return new Color(14, 16, 26, a).getRGB();
    }

    public static int panelList(float alphaMul) {
        int a = (int) (40 * alphaMul);
        return new Color(8, 9, 16, a).getRGB();
    }

    public static int textPrimary(float alphaMul) {
        int a = (int) (245 * alphaMul);
        return new Color(238, 240, 248, a).getRGB();
    }

    public static int textMuted(float alphaMul) {
        int a = (int) (160 * alphaMul);
        return new Color(110, 117, 135, a).getRGB();
    }

    public static int accentLine(float alphaMul) {
        int a = (int) (90 * alphaMul);
        int r = (ACCENT >> 16) & 0xFF;
        int g = (ACCENT >> 8) & 0xFF;
        int b = ACCENT & 0xFF;
        return new Color(r, g, b, a).getRGB();
    }

    public static int accentGlow(float alphaMul) {
        int a = (int) (60 * alphaMul);
        int r = (ACCENT_GLOW >> 16) & 0xFF;
        int g = (ACCENT_GLOW >> 8) & 0xFF;
        int b = ACCENT_GLOW & 0xFF;
        return new Color(r, g, b, a).getRGB();
    }

    public static int accentWarm(float alphaMul) {
        int a = (int) (80 * alphaMul);
        int r = (ACCENT_WARM >> 16) & 0xFF;
        int g = (ACCENT_WARM >> 8) & 0xFF;
        int b = ACCENT_WARM & 0xFF;
        return new Color(r, g, b, a).getRGB();
    }

    public static int accentTeal(float alphaMul) {
        int a = (int) (70 * alphaMul);
        int r = (ACCENT_TEAL >> 16) & 0xFF;
        int g = (ACCENT_TEAL >> 8) & 0xFF;
        int b = ACCENT_TEAL & 0xFF;
        return new Color(r, g, b, a).getRGB();
    }
}
