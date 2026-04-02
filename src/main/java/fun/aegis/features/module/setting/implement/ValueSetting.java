package fun.aegis.features.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import fun.aegis.features.module.setting.Setting;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class ValueSetting extends Setting {
    private double value;
    private double min;
    private double max;
    private double step = 0.1;
    private int key = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
    private int type = 1;

    public ValueSetting(String name, String description, double value, double min, double max) {
        super(name, description);
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public ValueSetting(String name, String description, double value, double min, double max, double step) {
        super(name, description);
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public ValueSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public void setValue(double value) {
        this.value = Math.max(min, Math.min(max, value));
    }
}
