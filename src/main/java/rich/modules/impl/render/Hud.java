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

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
            .value("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "test",
                    "TargetHud",
//                    "CoolDowns",
//                    "Inventory",
                    "Info",
                    "Notifications")

            .selected("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "TargetHud",
//                    "CoolDowns",
//                    "Inventory",
                    "Info",
                    "Notifications");

    public BooleanSetting showBps = new BooleanSetting("Show BPS", "Показывать блоки в секунду")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Info"));

    public BooleanSetting showTps = new BooleanSetting("Show TPS", "Показывать TPS в Watermark")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        settings(interfaceSettings, showBps, showTps);
    }
}