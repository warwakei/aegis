package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends ModuleStructure {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("ÃƒÂÃ‚Â­ÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¼ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹", "ÃƒÂÃ‚ÂÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¹ÃƒÂÃ‚ÂºÃƒÂÃ‚Â° Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¼ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â² ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¹Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â°")
            .value("Watermark",
                    "TPS",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "test",
                    "TargetHud",
//                    "CoolDowns",
//                    "Inventory",
                    "Coords",
                    "BPS",
                    "Notifications")

            .selected("Watermark",
                    "TPS",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "TargetHud",
//                    "CoolDowns",
//                    "Inventory",
                    "Coords",
                    "BPS",
                    "Notifications");

    public BooleanSetting showBps = new BooleanSetting("Show BPS", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â² Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂµÃƒÂÃ‚ÂºÃƒâ€˜Ã†â€™ÃƒÂÃ‚Â½ÃƒÂÃ‚Â´Ãƒâ€˜Ã†â€™")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("BPS"));

    public BooleanSetting showTps = new BooleanSetting("Show TPS", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ TPS ÃƒÂÃ‚Â² Watermark")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("TPS"));

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        settings(interfaceSettings, showBps, showTps);
    }
}

