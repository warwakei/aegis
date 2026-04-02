package fun.aegis.display.screens.clickgui.newgui.elements;

import fun.aegis.features.module.ModuleCategory;
import net.minecraft.client.gui.DrawContext;

public abstract class AbstractMenuElement {
    public abstract void render(DrawContext ctx, float mouseX, float mouseY, float x, float y, float moduleWidth, float alpha, int column);
    
    public abstract float getHeight();
    
    public abstract void onMouseClicked(double mouseX, double mouseY, int button);
    
    public abstract void onMouseReleased(double mouseX, double mouseY, int button);
    
    public abstract void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);
    
    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);
    
    public abstract boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount);
    
    public abstract ModuleCategory getCategory();
    
    public abstract String getName();
}
