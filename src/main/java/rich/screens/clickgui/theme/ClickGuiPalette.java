package rich.screens.clickgui.theme;

import java.awt.*;

/**
 * Единая палитра ClickGUI: монолитный тёмный корпус с одним холодным акцентом.
 */
public final class ClickGuiPalette {

    private ClickGuiPalette() {}

    public static final int ACCENT = 0xFF4A6FA5;
    public static final int ACCENT_DIM = 0xFF354E75;

    public static int bgMain(float alphaMul) {
        int a = (int) (255 * alphaMul);
        return new Color(14, 15, 20, a).getRGB();
    }

    public static int bgElevated(float alphaMul) {
        int a = (int) (255 * alphaMul);
        return new Color(18, 20, 28, a).getRGB();
    }

    public static int border(float alphaMul) {
        int a = (int) (220 * alphaMul);
        return new Color(38, 42, 52, a).getRGB();
    }

    public static int borderSubtle(float alphaMul) {
        int a = (int) (140 * alphaMul);
        return new Color(32, 36, 46, a).getRGB();
    }

    public static int panelInset(float alphaMul) {
        int a = (int) (38 * alphaMul);
        return new Color(22, 24, 32, a).getRGB();
    }

    public static int panelList(float alphaMul) {
        int a = (int) (28 * alphaMul);
        return new Color(20, 22, 30, a).getRGB();
    }

    public static int textPrimary(float alphaMul) {
        int a = (int) (235 * alphaMul);
        return new Color(230, 232, 238, a).getRGB();
    }

    public static int textMuted(float alphaMul) {
        int a = (int) (150 * alphaMul);
        return new Color(110, 115, 128, a).getRGB();
    }

    public static int accentLine(float alphaMul) {
        int a = (int) (90 * alphaMul);
        int r = (ACCENT >> 16) & 0xFF;
        int g = (ACCENT >> 8) & 0xFF;
        int b = ACCENT & 0xFF;
        return new Color(r, g, b, a).getRGB();
    }
}
