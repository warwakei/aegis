package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import static fun.aegis.utils.display.interfaces.QuickImports.blur;

@Getter
public class MenuColorSetting extends AbstractMenuSetting {
    private final ColorSetting setting;
    private Rect bounds;

    public MenuColorSetting(ColorSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext ctx, float mouseX, float mouseY, float x, float settingY,
                       float moduleWidth, float alpha, float animEnable, int themeColor,
                       int textColor, int descriptionColor, Theme theme) {
        MatrixStack matrix = ctx.getMatrices();
        float settingX = x + 8;
        float fontHeight = 7f;
        float textY = settingY + (8 - fontHeight) / 2 - 0.5f;
        MsdfFonts.drawText(matrix, setting.getName(), x + 8 + 10, textY, 7f, textColor);
        MsdfFonts.drawIcon(matrix, "V", settingX + 1.5f, textY, 6f, themeColor);

        float toggleSize = 8;
        float toggleX = x + moduleWidth - toggleSize - 8;
        float toggleY = settingY;

        int colorValue = setting.getColor();
        int colorEnable = Theme.mixColors(theme.getWhiteGrayInt(), colorValue, animEnable);
        colorEnable = Theme.applyAlpha(colorEnable, alpha);

        blur.render(ShapeProperties.create(matrix, toggleX, toggleY, toggleSize, toggleSize)
                .round(3).color(colorEnable).build());

        bounds = new Rect(toggleX, toggleY, toggleSize, toggleSize);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        // Color picker popup would be opened here
    }

    @Override public float getWidth() { return 0; }
    @Override public float getHeight() { return 8; }
    @Override public boolean isVisible() { return setting.isVisible(); }
}
