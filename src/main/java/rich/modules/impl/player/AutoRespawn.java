package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.events.api.EventHandler;
import rich.events.impl.DeathScreenEvent;
import rich.events.impl.PacketEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;

@SuppressWarnings("all")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoRespawn extends ModuleStructure {

    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите, что будет использоваться").value("Default");

    public AutoRespawn() {
        super("AutoRespawn", "Auto Respawn", ModuleCategory.PLAYER);
        settings(modeSetting);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onDeathScreen(DeathScreenEvent e) {
        if (modeSetting.isSelected("Default")) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}