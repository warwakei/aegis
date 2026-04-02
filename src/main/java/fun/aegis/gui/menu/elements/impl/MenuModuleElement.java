package fun.aegis.gui.menu.elements.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import fun.aegis.gui.menu.elements.api.AbstractMenuElement;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.Module;

public class MenuModuleElement extends AbstractMenuElement {
    private final Module module;
    
    public MenuModuleElement(Module module) {
        this.module = module;
    }
    
    @Override
    public void render(DrawContext context, float mouseX, float mouseY, TextRenderer font,
                      float x, float y, float moduleWidth, float alpha, int column) {
        // Фон модуля
        int bgColor = module.isState() ? 0xFF6060FF : 0xFF303030;
        context.fill((int)x, (int)y, (int)(x + moduleWidth), (int)(y + 18), bgColor);
        
        // Текст модуля
        String moduleName = module.getName();
        float textWidth = font.getWidth(moduleName);
        float textX = x + (moduleWidth - textWidth) / 2f;
        float textY = y + (18 - font.fontHeight) / 2f;
        
        context.drawText(font, moduleName, (int)textX, (int)textY, 0xFFFFFFFF, false);
    }
    
    @Override
    public float getHeight() {
        return 18;
    }
    
    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        // Обработка клика по модулю
        if (button == 0) { // Left click
            module.switchState();
        }
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
        return module.getCategory();
    }
    
    @Override
    public String getName() {
        return module.getName();
    }
}
