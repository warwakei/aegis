package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.Instance;

public class NoDelay extends ModuleStructure {

    public static NoDelay getInstance() {
        return Instance.get(NoDelay.class);
    }

    public MultiSelectSetting ignoreSetting = new MultiSelectSetting("Тип", "")
            .value("Прыжок", "Правый клик", "Задержка ломания").selected("Прыжок");

    public NoDelay() {
        super("NoDelay", "No Delay", ModuleCategory.PLAYER);
        settings(ignoreSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Задержка ломания
        if (ignoreSetting.isSelected("Задержка ломания")) {
            mc.interactionManager.blockBreakingCooldown = 0;
        }

        // Прыжок
        if (ignoreSetting.isSelected("Прыжок")) {
            mc.player.jumpingCooldown = 0;
        }

        // Правый клик
        if (ignoreSetting.isSelected("Правый клик")) {
            mc.itemUseCooldown = 0;
        }
    }
}