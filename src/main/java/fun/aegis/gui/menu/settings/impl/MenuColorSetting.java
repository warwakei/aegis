package fun.aegis.gui.menu.settings.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;
import fun.aegis.gui.menu.settings.api.MenuSetting;

public class MenuColorSetting implements MenuSetting {
    private final String name;
    private int color;
    
    public MenuColorSetting(String name, int defaultColor) {
        this.name = name;
        this.color = defaultColor;
    }
    
    @Override
    public void render(DrawContext context, float mouseX, float mouseY, float x, float y, float width, float alpha, float progress) {
        // Фон настройки
        int bgColor = 0xFF303030;
        context.fill((int)x, (int)y, (int)(x + width), (int)(y + 16), bgColor);
        
        // Цветовой квадрат
        context.fill((int)x + 2, (int)y + 2, (int)(x + 14), (int)(y + 14), color);
        
        // Текст названия
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        float textX = x + 18;
        float textY = y + (16 - font.fontHeight) / 2f;
        
        context.drawText(font, name, (int)textX, (int)textY, 0xFFFFFFFF, false);
    }
    
    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        // Обработка клика для открытия колорпикера
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
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
}
