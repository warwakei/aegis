package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fun.aegis.utils.display.interfaces.QuickImports.blur;

public class MenuModeSetting extends AbstractMenuSetting {
    private final SelectSetting setting;
    private final Map<String, Rect> modeSettingOptionBounds = new HashMap<>();
    private Rect bounds;
    private boolean expanded;
    private final Animation expandedAnimation = new Animation(200, 0, Easing.QUAD_IN_OUT);

    public MenuModeSetting(SelectSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext ctx, float mouseX, float mouseY, float x, float settingY,
                       float moduleWidth, float alpha, float animEnable, int themeColor,
                       int textColor, int descriptionColor, Theme theme) {
        MatrixStack matrix = ctx.getMatrices();
        float textSize = 7f;

        // Icon
        MsdfFonts.drawIcon(matrix, "K", x + 8, settingY + 2, 6f, themeColor);

        // Name
        float nameX = x + 18;
        MsdfFonts.drawText(matrix, setting.getName(), nameX, settingY + 2, textSize, textColor);

        float dropdownWidth = moduleWidth / 2.2f;
        float dropdownHeight = 13 + expandedAnimation.update(expanded ? 1 : 0) * setting.getList().size() * 13;
        float dropdownX = x + moduleWidth - dropdownWidth - 8;

        // Dropdown background
        blur.render(ShapeProperties.create(matrix, dropdownX, settingY, dropdownWidth, dropdownHeight)
                .round(3).color(Theme.applyAlpha(theme.getForegroundColorInt(), alpha)).build());
        blur.render(ShapeProperties.create(matrix, dropdownX, settingY, dropdownWidth, 13)
                .round(expanded ? new float[]{3, 3, 0, 0} : new float[]{3, 3, 3, 3})
                .color(Theme.applyAlpha(theme.getForegroundLightInt(), alpha)).build());

        // Current value
        String currentModeText = setting.getSelected();
        MsdfFonts.drawText(matrix, currentModeText, dropdownX + 6, settingY + 4, 6f, textColor);

        // Arrow
        int arrowColor = Theme.mixColors(theme.getGrayInt(), theme.getGrayLightInt(), animEnable);
        arrowColor = Theme.applyAlpha(arrowColor, alpha);
        
        MsdfFonts.drawIcon(matrix, "Q", dropdownX + dropdownWidth - 12, settingY + 4, 6f, arrowColor);

        bounds = new Rect(dropdownX, settingY, dropdownWidth, dropdownHeight);

        // Options
        if (expandedAnimation.getValue() > 0) {
            List<String> modes = setting.getList();
            int disableColor = Theme.mixColors(theme.getGrayInt(), theme.getGrayLightInt(), animEnable);
            disableColor = Theme.applyAlpha(disableColor, alpha * expandedAnimation.getValue());
            
            int enabledColor = Theme.mixColors(theme.getForegroundGrayInt(), theme.getColorInt(), animEnable);
            enabledColor = Theme.applyAlpha(enabledColor, alpha * expandedAnimation.getValue());

            float optionY = settingY + 13;
            modeSettingOptionBounds.clear();
            
            for (String mode : modes) {
                Rect optionRect = new Rect(dropdownX, optionY, dropdownWidth, 13);
                if (optionY > settingY + dropdownHeight) break;

                if (mode.equals(setting.getSelected())) {
                    blur.render(ShapeProperties.create(matrix, dropdownX + 1f, optionY, dropdownWidth - 2f, 13)
                            .round(mode.equals(modes.get(modes.size() - 1)) ? new float[]{0, 0, 3, 3} : new float[]{0, 0, 0, 0})
                            .color(enabledColor).build());
                    MsdfFonts.drawText(matrix, mode, dropdownX + 6, optionY + 4, 6f,
                            Theme.applyAlpha(textColor, expandedAnimation.getValue()));
                } else {
                    MsdfFonts.drawText(matrix, mode, dropdownX + 6, optionY + 4, 6f, disableColor);
                }

                modeSettingOptionBounds.put(mode, optionRect);
                optionY += 13;
            }
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (bounds != null && button == 1 && bounds.contains(mouseX, mouseY)) {
            expanded = !expanded;
        }

        if (expanded && button == 0) {
            for (Map.Entry<String, Rect> entry : modeSettingOptionBounds.entrySet()) {
                if (entry.getValue().contains(mouseX, mouseY)) {
                    setting.setSelected(entry.getKey());
                }
            }
        }
    }

    @Override public float getWidth() { return 0; }
    @Override public float getHeight() { return 13 + expandedAnimation.getValue() * setting.getList().size() * 13; }
    @Override public boolean isVisible() { return setting.isVisible(); }
}
