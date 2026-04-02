package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import static fun.aegis.utils.display.interfaces.QuickImports.blur;

@Getter
public class MenuBooleanSetting extends AbstractMenuSetting {
    private final BooleanSetting setting;
    private final Animation animation = new Animation(300, Easing.QUARTIC_OUT);
    private Rect bounds;

    public MenuBooleanSetting(BooleanSetting setting) {
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

        // Setting name
        MsdfFonts.drawText(matrix, setting.getName(), x + 8 + 10, textY, 7f, textColor);

        // Icon
        float iconSize = 6;
        float iconY = textY - 1;
        
        blur.render(ShapeProperties.create(matrix, settingX, iconY, iconSize, iconSize)
                .round(1).color(themeColor).build());
        MsdfFonts.drawIcon(matrix, "S", settingX + 1.2f, iconY + 0.5f, 5f,
                Theme.applyAlpha(theme.getForegroundDarkInt(), alpha));

        // Toggle
        animation.animateTo(setting.isValue() ? 1.0f : 0.0f);
        float progress = animation.update();

        float toggleSize = 8;
        float toggleX = x + moduleWidth - toggleSize - 8;
        float toggleY = settingY;

        int colorEnable = Theme.mixColors(theme.getWhiteGrayInt(), theme.getColorInt(), animEnable);
        int colorToggle = Theme.mixColors(theme.getForegroundLightInt(), colorEnable, animation.getValue());
        colorToggle = Theme.applyAlpha(colorToggle, alpha);

        blur.render(ShapeProperties.create(matrix, toggleX, toggleY, toggleSize, toggleSize)
                .round(2).color(colorToggle).build());

        int checkColor = Theme.mixColors(theme.getGrayLightInt(), theme.getWhiteInt(), animEnable);
        checkColor = Theme.mixColors(0x00000000, checkColor, animation.getValue());
        checkColor = Theme.applyAlpha(checkColor, alpha);

        MsdfFonts.drawIcon(matrix, "S", toggleX + 2, toggleY + 1, 6f, checkColor);

        bounds = new Rect(toggleX, toggleY, toggleSize, toggleSize);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (bounds != null && bounds.contains(mouseX, mouseY) && button == 0) {
            setting.setValue(!setting.isValue());
        }
    }

    @Override public float getWidth() { return 0; }
    @Override public float getHeight() { return 8; }
    @Override public boolean isVisible() { return setting.isVisible(); }
}
