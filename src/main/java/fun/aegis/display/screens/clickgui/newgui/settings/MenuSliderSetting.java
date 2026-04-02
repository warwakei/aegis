package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Locale;

import static fun.aegis.utils.display.interfaces.QuickImports.blur;

@Getter
public class MenuSliderSetting extends AbstractMenuSetting {
    private final SliderSettings setting;
    private boolean dragging = false;
    private Rect rect;

    public MenuSliderSetting(SliderSettings setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext ctx, float mouseX, float mouseY, float x, float settingY,
                       float moduleWidth, float alpha, float animEnable, int themeColor,
                       int textColor, int descriptionColor, Theme theme) {
        MatrixStack matrix = ctx.getMatrices();
        
        float padding = 8f;
        float textSize = 7f;

        // Icon
        MsdfFonts.drawIcon(matrix, "D", x + padding, settingY, 7f, themeColor);

        // Name
        float nameX = x + padding + 10;
        MsdfFonts.drawText(matrix, setting.getName(), nameX, settingY, textSize, textColor);

        float animatedValue = setting.getValue();
        float sliderWidth = setting.getDescription() == null || setting.getDescription().isEmpty() 
                ? moduleWidth - 20 : moduleWidth / 2.8f;
        float sliderX = x + moduleWidth - padding - 4 - sliderWidth;
        float sliderY = settingY + 12;

        // Value text
        String valueText = setting.isInteger() 
                ? String.valueOf((int) animatedValue) 
                : String.format(Locale.US, "%.1f", animatedValue);
        float valueTextWidth = MsdfFonts.getTextWidth(valueText, textSize);
        float valueTextX = x + moduleWidth - padding - valueTextWidth;
        MsdfFonts.drawText(matrix, valueText, valueTextX, settingY, textSize, themeColor);

        // Slider background
        blur.render(ShapeProperties.create(matrix, sliderX, sliderY, sliderWidth, 2)
                .round(0.2f).color(Theme.applyAlpha(theme.getForegroundLightInt(), alpha)).build());

        // Slider fill
        float percent = (animatedValue - setting.getMin()) / (setting.getMax() - setting.getMin());
        float filledWidth = sliderWidth * percent;
        
        int sliderColor = Theme.mixColors(theme.getGrayInt(), theme.getColorInt(), animEnable);
        sliderColor = Theme.applyAlpha(sliderColor, alpha);
        
        blur.render(ShapeProperties.create(matrix, sliderX, sliderY, filledWidth - 2, 2)
                .round(0).color(sliderColor).build());

        // Handle
        float handleX = sliderX + filledWidth;
        float handleY = sliderY - 1;
        
        int circleColor = Theme.mixColors(theme.getGrayLightInt(), theme.getWhiteInt(), animEnable);
        circleColor = Theme.applyAlpha(circleColor, alpha);
        
        blur.render(ShapeProperties.create(matrix, handleX, handleY, 4, 4)
                .round(2).color(circleColor).build());

        rect = new Rect(sliderX, sliderY - 2, sliderWidth, 6);

        // Description
        if (setting.getDescription() != null && !setting.getDescription().isEmpty()) {
            float descY = settingY + 10;
            MsdfFonts.drawRegular(matrix, setting.getDescription(), x + padding, descY, textSize, descriptionColor);
        }

        updateSlider(mouseX);
    }

    public void updateSlider(double mouseX) {
        if (!dragging) return;
        if (rect == null) return;

        double relativeX = mouseX - rect.x();
        double percent = Math.max(0, Math.min(1, relativeX / rect.width()));
        
        float min = setting.getMin();
        float max = setting.getMax();
        float newValue = (float) (min + (max - min) * percent);

        if (setting.isInteger()) {
            newValue = Math.round(newValue);
        }

        newValue = Math.max(min, Math.min(max, newValue));
        setting.setValue(newValue);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && rect != null && rect.contains(mouseX, mouseY)) {
            dragging = true;
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    @Override public float getWidth() { return 0; }
    @Override public float getHeight() { return 14; }
    @Override public boolean isVisible() { return setting.isVisible(); }
}
