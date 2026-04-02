package rich.modules.module.setting.implement;

import rich.modules.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.function.Supplier;

@Getter
@Setter
public class ColorSetting extends Setting {
    private float hue = 0,
            saturation = 1,
            brightness = 1,
            alpha = 1;

    private int[] presets = new int[0];

    public ColorSetting(String name, String description) {
        super(name, description);
    }

    public ColorSetting value(int value) {
        setColor(value);
        return this;
    }

    public ColorSetting presets(int... presets) {
        this.presets = presets;
        return this;
    }

    public ColorSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public int getColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int alphaInt = Math.round(alpha * 255);
        return (alphaInt << 24) | (rgb & 0x00FFFFFF);
    }

    public int getColorWithAlpha() {
        return getColor();
    }

    public int getColorNoAlpha() {
        return Color.HSBtoRGB(hue, saturation, brightness) | 0xFF000000;
    }

    public ColorSetting setColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        float[] hsb = Color.RGBtoHSB(r, g, b, null);

        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = a / 255f;

        return this;
    }

    public Color getAwtColor() {
        int color = getColor();
        return new Color(color, true);
    }

    public ColorSetting setHue(float hue) {
        this.hue = Math.max(0f, Math.min(1f, hue));
        return this;
    }

    public ColorSetting setSaturation(float saturation) {
        this.saturation = Math.max(0f, Math.min(1f, saturation));
        return this;
    }

    public ColorSetting setBrightness(float brightness) {
        this.brightness = Math.max(0f, Math.min(1f, brightness));
        return this;
    }

    public ColorSetting setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        return this;
    }
}