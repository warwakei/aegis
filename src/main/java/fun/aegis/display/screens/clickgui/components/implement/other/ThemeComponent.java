package fun.aegis.display.screens.clickgui.components.implement.other;

import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class ThemeComponent extends AbstractComponent {
    
    private static final int[] THEME_COLORS = {
            0xFFFF6539, // Оранжевый (default)
            0xFF6C9AFD, // Голубой
            0xFF8C7FFF, // Фиолетовый
            0xFFFFA576, // Персиковый
            0xFFFF7B7B, // Розовый
            0xFF7BFFB5, // Зелёный
            0xFFFFD700, // Золотой
            0xFFFF69B4, // Ярко-розовый
            0xFF00CED1, // Бирюзовый
            0xFFFF4500  // Красно-оранжевый
    };
    
    private static final float CIRCLE_SIZE = 10f;
    private static final float CIRCLE_GAP = 5f;
    
    private int selectedIndex = 0;
    
    public ThemeComponent() {
        // Найти текущий выбранный цвет
        int currentColor = Hud.getInstance().colorSetting.getColor() | 0xFF000000;
        for (int i = 0; i < THEME_COLORS.length; i++) {
            if (THEME_COLORS[i] == currentColor) {
                selectedIndex = i;
                break;
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        
        float totalWidth = THEME_COLORS.length * CIRCLE_SIZE + (THEME_COLORS.length - 1) * CIRCLE_GAP;
        float startX = x + (width - totalWidth) / 2f;
        float circleY = y + (height - CIRCLE_SIZE) / 2f;
        
        for (int i = 0; i < THEME_COLORS.length; i++) {
            float circleX = startX + i * (CIRCLE_SIZE + CIRCLE_GAP);
            int color = THEME_COLORS[i];
            
            // Рисуем кружок
            rectangle.render(ShapeProperties.create(matrix, circleX, circleY, CIRCLE_SIZE, CIRCLE_SIZE)
                    .round(CIRCLE_SIZE / 2f)
                    .color(color)
                    .build());
            
            // Обводка для выбранного
            if (i == selectedIndex) {
                rectangle.render(ShapeProperties.create(matrix, circleX - 1, circleY - 1, CIRCLE_SIZE + 2, CIRCLE_SIZE + 2)
                        .round((CIRCLE_SIZE + 2) / 2f)
                        .thickness(1.5f)
                        .color(0x00000000)
                        .outlineColor(0xFFFFFFFF)
                        .build());
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        float totalWidth = THEME_COLORS.length * CIRCLE_SIZE + (THEME_COLORS.length - 1) * CIRCLE_GAP;
        float startX = x + (width - totalWidth) / 2f;
        float circleY = y + (height - CIRCLE_SIZE) / 2f;
        
        for (int i = 0; i < THEME_COLORS.length; i++) {
            float circleX = startX + i * (CIRCLE_SIZE + CIRCLE_GAP);
            
            if (Calculate.isHovered(mouseX, mouseY, circleX, circleY, CIRCLE_SIZE, CIRCLE_SIZE)) {
                selectedIndex = i;
                Hud.getInstance().colorSetting.setColor(THEME_COLORS[i]);
                return true;
            }
        }
        
        return false;
    }
}
