package fun.aegis.display.screens.clickgui.newgui.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeRect {
    private float x, y, width, height;

    public ChangeRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
