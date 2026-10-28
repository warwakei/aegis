package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoRender extends ModuleStructure {

    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Элементы", "Выберите элементы для игнорирования")
            .value("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea", "Scoreboard", "BossBar")
            .selected("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea");

    public NoRender() {
        super("NoRender", "No Render", ModuleCategory.RENDER);
        settings(modeSetting);
    }
}