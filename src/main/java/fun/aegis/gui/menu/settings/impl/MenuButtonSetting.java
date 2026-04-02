package fun.aegis.gui.menu.settings.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;
import fun.aegis.gui.menu.settings.api.MenuSetting;

public class MenuButtonSetting implements MenuSetting {
    private final String name;
    private final Runnable action;
    
    public MenuButtonSetting(String name, Runnable action) {
        this.name = name;
        this.action = action;
    }
    
    @Override
    public void render(DrawContext context, float mouseX, float mouseY, float x, float y, float width, float alpha, float progress) {
        // Фон кнопки
        int bgColor = 0xFF404040;
        context.fill((int)x, (int)y, (int)(x + width), (int)(y + 16), bgColor);
        
        // Текст кнопки
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        float textWidth = font.getWidth(name);
        float textX = x + (width - textWidth) / 2f;
        float textY = y + (16 - font.fontHeight) / 2f;
        
        context.drawText(font, name, (int)textX, (int)textY, 0xFFFFFFFF, false);
    }
    
    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && action != null) {
            action.run();
        }
    }
    
    @Override
    public void onMouseReleased(double mouseX, double mouseY, int button) {
    }
    
    @Override
    public void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    }
    
    @Override
    public float getHeight() {
        return 16;
    }
    
    @Override
    public String getName() {
        return name;
    }
}
