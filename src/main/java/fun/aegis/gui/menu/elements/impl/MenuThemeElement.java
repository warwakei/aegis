package fun.aegis.gui.menu.elements.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import fun.aegis.gui.menu.elements.api.AbstractMenuElement;
import fun.aegis.features.module.ModuleCategory;

public class MenuThemeElement extends AbstractMenuElement {
    private final String themeName;
    
    public MenuThemeElement(String themeName) {
        this.themeName = themeName;
    }
    
    @Override
    public void render(DrawContext context, float mouseX, float mouseY, TextRenderer font,
                      float x, float y, float moduleWidth, float alpha, int column) {
        // Простой рендер темы
        int bgColor = 0xFF404040;
        context.fill((int)x, (int)y, (int)(x + moduleWidth), (int)(y + 18), bgColor);
        
        float textWidth = font.getWidth(themeName);
        float textX = x + (moduleWidth - textWidth) / 2f;
        float textY = y + (18 - font.fontHeight) / 2f;
        
        context.drawText(font, themeName, (int)textX, (int)textY, 0xFFFFFFFF, false);
    }
    
    @Override
    public float getHeight() {
        return 18;
    }
    
    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        // Обработка клика по теме
    }
    
    @Override
    public void onMouseReleased(double mouseX, double mouseY, int button) {
    }
    
    @Override
    public void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }
    
    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.RENDER;
    }
    
    @Override
    public String getName() {
        return themeName;
    }
}
