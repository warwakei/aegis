package fun.aegis.display.screens.clickgui.newgui.panels;

import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.font.Fonts;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

@Getter
public class SideBarCategory {
    private final ModuleCategory category;
    private final Animation animationSwitch;

    public SideBarCategory(ModuleCategory category) {
        this.category = category;
        animationSwitch = new Animation(200, category == ModuleCategory.COMBAT ? 1 : 0, Easing.LINEAR);
    }

    public void render(DrawContext ctx, float x, float y, float width, float height, float sidebarProgress,
                       boolean selected, int textColor, int textColorDisable, int iconColorDisable, int primary) {
        MatrixStack matrix = ctx.getMatrices();
        
        animationSwitch.animateTo(selected ? 1 : 0);
        animationSwitch.update();

        int mixColor = Theme.mixColors(iconColorDisable, primary, animationSwitch.getValue());
        int mixColorText = Theme.mixColors(textColorDisable, textColor, animationSwitch.getValue());

        float iconSize = 16f;
        float offestY = (height - iconSize) / 2 + 2;
        float scale = MathHelper.lerp(sidebarProgress, 1f, 0.8f);

        matrix.push();
        float iconX = x + 8;
        float iconY = y + offestY;
        matrix.translate(iconX + iconSize / 2, iconY + iconSize / 2, 0);
        matrix.scale(scale, scale, 1);
        matrix.translate(-(iconX + iconSize / 2), -(iconY + iconSize / 2), 0);
        Fonts.getSize((int) iconSize, Fonts.Type.ICONSCATEGORY).drawString(matrix, getCategoryIcon(), iconX, iconY, mixColor);
        matrix.pop();

        float textX = x + 8 + iconSize * scale + 6;
        float textY = y + (height - 7f) / 2;
        // Только рисуем текст, без иконки
        String categoryName = category.getReadableName();
        Fonts.getSize(14, Fonts.Type.INST).drawString(matrix, categoryName, textX, textY, mixColorText);
    }

    private String getCategoryIcon() {
        return switch (category) {
            case COMBAT -> "b";
            case MOVEMENT -> "c";
            case RENDER -> "d";
            case PLAYER -> "e";
            case MISC -> "f";
            default -> "F";
        };
    }
}
