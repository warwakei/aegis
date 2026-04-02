package fun.aegis.display.screens.clickgui.newgui.utils;

public record Rect(float x, float y, float width, float height) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
