package rich.util.interfaces;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import rich.modules.module.setting.Setting;

import java.awt.*;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;
    protected float alphaMultiplier = 1f;

    public void setAlphaMultiplier(float alpha) {
        this.alphaMultiplier = alpha;
    }

    protected int applyAlpha(int color, float extraAlpha) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, (int)(a * alphaMultiplier * extraAlpha)));
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    protected int applyAlpha(int color) {
        return applyAlpha(color, 1f);
    }

    protected Color applyAlpha(Color color) {
        int newAlpha = Math.max(0, Math.min(255, (int)(color.getAlpha() * alphaMultiplier)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }

    protected Color applyAlpha(Color color, float extraAlpha) {
        int newAlpha = Math.max(0, Math.min(255, (int)(color.getAlpha() * alphaMultiplier * extraAlpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }
}