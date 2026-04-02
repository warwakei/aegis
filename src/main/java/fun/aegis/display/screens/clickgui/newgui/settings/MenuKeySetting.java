package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import static fun.aegis.utils.display.interfaces.QuickImports.blur;

@Getter
public class MenuKeySetting extends AbstractMenuSetting {
    private final BindSetting setting;
    private Rect bounds;
    private boolean binding = false;

    public MenuKeySetting(BindSetting setting) {
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
        MsdfFonts.drawIcon(matrix, "L", settingX + 1.2f, textY + 1.2f, 5f, 
                Theme.applyAlpha(Theme.mixColors(theme.getGrayInt(), theme.getColorInt(), animEnable), alpha));

        String keyText = binding ? "..." : "n/a";
        int keyCode = setting.getKey();
        if (keyCode != -1 && keyCode != 0 && !binding) {
            String name = GLFW.glfwGetKeyName(keyCode, 0);
            if (name != null && !name.isBlank()) {
                keyText = name.toLowerCase();
                if (keyText.length() > 6) keyText = keyText.substring(0, 6) + "..";
            }
        }

        float toggleWidth = 4 + MsdfFonts.getTextWidth(keyText, 7f) + 4;
        float toggleHeight = 8;
        float toggleX = x + moduleWidth - toggleWidth - 8;
        float toggleY = settingY;

        int colorToggle = Theme.mixColors(theme.getForegroundLightInt(), theme.getColorInt(), animEnable);
        colorToggle = Theme.applyAlpha(colorToggle, alpha);

        blur.render(ShapeProperties.create(matrix, toggleX, toggleY, toggleWidth, toggleHeight)
                .round(2).color(colorToggle).build());
        MsdfFonts.drawText(matrix, keyText, toggleX + 4, toggleY + 1, 7f, textColor);
        bounds = new Rect(toggleX, toggleY, toggleWidth, toggleHeight);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (binding && button >= 2) { setting.setKey(button); binding = false; return; }
        if (bounds != null && bounds.contains(mouseX, mouseY)) { binding = true; }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) keyCode = -1;
            setting.setKey(keyCode);
            binding = false;
            return true;
        }
        return false;
    }

    @Override public float getWidth() { return 0; }
    @Override public float getHeight() { return 8; }
    @Override public boolean isVisible() { return setting.isVisible(); }
}
