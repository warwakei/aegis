package rich.modules.module.setting.implement;

import rich.modules.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class ButtonSetting extends Setting {
    private Runnable runnable;
    private String buttonName;

    public ButtonSetting(String name, String description) {
        super(name, description);
    }

    public ButtonSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}