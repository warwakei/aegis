package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;

public class NoDelay extends ModuleStructure {

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
        
        // Задержка ломания - работает через mixin в другом месте
        // if (ignoreSetting.isSelected("Задержка ломания")) mc.interactionManager.blockBreakingCooldown = 0;
        
        // Прыжок - используем безопасный доступ
        if (ignoreSetting.isSelected("Прыжок")) {
            try {
                mc.player.jumpingCooldown = 0;
            } catch (Exception ignored) {}
        }
        
        // Правый клик - используем безопасный доступ  
        if (ignoreSetting.isSelected("Правый клик")) {
            try {
                mc.itemUseCooldown = 0;
            } catch (Exception ignored) {}
        }
    }
}