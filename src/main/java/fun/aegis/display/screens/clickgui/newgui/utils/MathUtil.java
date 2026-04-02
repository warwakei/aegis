package fun.aegis.display.screens.clickgui.newgui.utils;

public class MathUtil {
    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static boolean isHoveredByCords(double mouseX, double mouseY, double x1, double y1, double x2, double y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public static float interpolate(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    public static double interpolate(double from, double to, double progress) {
        return from + (to - from) * progress;
    }
}
