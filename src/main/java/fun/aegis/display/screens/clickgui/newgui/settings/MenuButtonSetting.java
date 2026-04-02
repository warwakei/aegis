package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.setting.implement.ButtonSetting;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import static fun.aegis.utils.display.interfaces.QuickImports.blur;

public class MenuButtonSetting extends AbstractMenuSetting {
    private final ButtonSetting button;
    private Rect bounds;

    public MenuButtonSetting(ButtonSetting button) {
        this.button = button;
    }

    @Override
    public void render(DrawContext ctx, float mouseX, float mouseY, float x, float settingY,
                       float moduleWidth, float alpha, float animEnable, int themeColor,
                       int textColor, int descriptionColor, Theme theme) {
        MatrixStack matrix = ctx.getMatrices();
        bounds = new Rect(x + 8, settingY, moduleWidth - 16, 16);
        blur.render(ShapeProperties.create(matrix, x + 8, settingY, moduleWidth - 16, 16)
                .round(4).color(Theme.applyAlpha(theme.getForegroundColorInt(), alpha)).build());
        float textWidth = MsdfFonts.getTextWidth(button.getName(), 7f);
        MsdfFonts.drawText(matrix, button.getName(), 
                x + (moduleWidth - textWidth) / 2f, settingY + 5, 7f, textColor);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (bounds != null && bounds.contains(mouseX, mouseY) && button == 0) {
            if (this.button.getRunnable() != null) {
                this.button.getRunnable().run();
            }
        }
    }

    @Override public float getWidth() { return 0; }
    @Override public float getHeight() { return 16; }
    @Override public boolean isVisible() { return true; }
}
