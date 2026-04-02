package fun.aegis.display.screens.clickgui.newgui.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScrollHandler {
    private double value = 0;
    private double targetValue = 0;
    private double max = 0;
    private double smoothness = 0.15;

    public void scroll(double amount) {
        targetValue -= amount * 30;
        clamp();
    }

    public void setTargetValue(double target) {
        this.targetValue = target;
        clamp();
    }

    public void update() {
        value += (targetValue - value) * smoothness;
        if (Math.abs(value - targetValue) < 0.01) {
            value = targetValue;
        }
    }

    private void clamp() {
        if (targetValue < 0) targetValue = 0;
        if (targetValue > max) targetValue = max;
    }

    public void setMax(double max) {
        this.max = Math.max(0, max);
        clamp();
    }
}
