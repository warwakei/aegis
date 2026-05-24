package rich.screens.clickgui.theme;

import java.awt.*;

/**
 * Единая палитра ClickGUI: монолитный тёмный корпус с одним холодным акцентом.
 */
public final class ClickGuiPalette {

    private ClickGuiPalette() {}

    public static final int ACCENT = 0xFF5280B8;
    public static final int ACCENT_DIM = 0xFF3D5A8C;

    public static int bgMain(float alphaMul) {
        int a = (int) (255 * alphaMul);
        return new Color(12, 13, 18, a).getRGB();
    }

    public static int bgElevated(float alphaMul) {
        int a = (int) (255 * alphaMul);
        return new Color(16, 18, 26, a).getRGB();
    }

    public static int border(float alphaMul) {
        int a = (int) (230 * alphaMul);
        return new Color(42, 46, 56, a).getRGB();
    }

    public static int borderSubtle(float alphaMul) {
        int a = (int) (150 * alphaMul);
        return new Color(34, 38, 48, a).getRGB();
    }

    public static int panelInset(float alphaMul) {
        int a = (int) (42 * alphaMul);
        return new Color(22, 24, 32, a).getRGB();
    }

    public static int panelList(float alphaMul) {
        int a = (int) (32 * alphaMul);
        return new Color(20, 22, 30, a).getRGB();
    }

    public static int textPrimary(float alphaMul) {
        int a = (int) (240 * alphaMul);
        return new Color(235, 237, 242, a).getRGB();
    }

    public static int textMuted(float alphaMul) {
        int a = (int) (160 * alphaMul);
        return new Color(120, 125, 138, a).getRGB();
    }

    public static int accentLine(float alphaMul) {
        int a = (int) (90 * alphaMul);
        int r = (ACCENT >> 16) & 0xFF;
        int g = (ACCENT >> 8) & 0xFF;
        int b = ACCENT & 0xFF;
        return new Color(r, g, b, a).getRGB();
    }
}
