package fun.aegis.gui.menu.elements.api;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import fun.aegis.features.module.ModuleCategory;

public abstract class AbstractMenuElement {
    public abstract void render(DrawContext context, float mouseX, float mouseY, TextRenderer font,
                                float x, float y, float moduleWidth, float alpha, int column);

    public abstract float getHeight();

    public abstract void onMouseClicked(double mouseX, double mouseY, int button);

    public abstract void onMouseReleased(double mouseX, double mouseY, int button);

    public abstract void onMouseDragged(double mouseX, double mouseY, int button,
                                        double deltaX, double deltaY);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract boolean mouseScrolled(double mouseX, double mouseY,
                                          double horizontalAmount, double verticalAmount);
    public abstract ModuleCategory getCategory();
    public abstract String getName();
}
