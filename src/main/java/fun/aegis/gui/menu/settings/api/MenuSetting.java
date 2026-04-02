package fun.aegis.gui.menu.settings.api;

import net.minecraft.client.gui.DrawContext;

public interface MenuSetting {
    void render(DrawContext context, float mouseX, float mouseY, float x, float y, float width, float alpha, float progress);
    
    void onMouseClicked(double mouseX, double mouseY, int button);
    
    void onMouseReleased(double mouseX, double mouseY, int button);
    
    void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);
    
    float getHeight();
    
    String getName();
}
