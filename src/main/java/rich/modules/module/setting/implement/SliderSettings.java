package rich.modules.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import rich.modules.module.setting.Setting;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class SliderSettings extends Setting {
    private float value, min, max, defaultValue;
    private boolean integer;

    public SliderSettings(String name, String description) {
        super(name, description);
    }

    public SliderSettings range(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public SliderSettings range(int min, int max) {
        this.min = min;
        this.max = max;
        this.integer = true;
        return this;
    }

    public SliderSettings defaultValue(float defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public int getInt() {
        return (int) value;
    }

    public SliderSettings visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}