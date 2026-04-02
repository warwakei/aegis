package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.ColorSetting;
import rich.util.ColorUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChinaHat extends ModuleStructure {

    private static ChinaHat instance;

    public static ChinaHat getInstance() {
        return instance;
    }

    public final ColorSetting color1 = new ColorSetting("Color 1", "First gradient color")
            .value(ColorUtil.getColor(255, 50, 100, 255));

    public final ColorSetting color2 = new ColorSetting("Color 2", "Second gradient color")
            .value(ColorUtil.getColor(100, 50, 255, 255));

    public ChinaHat() {
        super("ChinaHat", "China Hat", ModuleCategory.RENDER);
        instance = this;
        settings(color1, color2);
    }
}