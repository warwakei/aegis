package rich.modules.module.setting.implement;

import rich.modules.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class BooleanSetting extends Setting {
    private boolean value;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private int type = 1;

    public BooleanSetting(String name, String description) {
        super(name, description);
    }

    public BooleanSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}