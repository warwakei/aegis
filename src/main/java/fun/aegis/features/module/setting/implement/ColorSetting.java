package fun.aegis.features.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.utils.math.calc.Calculate;

import java.awt.*;
import java.util.function.Supplier;

import static fun.aegis.utils.math.calc.Calculate.*;

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
        return (getColorWithAlpha() & 0x00FFFFFF) | (Math.round(alpha * 255) << 24);
    }

    public int getColorWithAlpha() {
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public ColorSetting setColor(int color) {
        float[] hsb = Color.RGBtoHSB(
                getRed(color),
                getGreen(color),
                getBlue(color),
                null
        );

        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = (Calculate.getAlpha(color) / 255f);

        return this;
    }
}
