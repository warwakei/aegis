package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.network.Network;
import rich.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoLeave extends ModuleStructure {
    SelectSetting leaveType = new SelectSetting("Тип выхода", "Позволяет выбрать тип выхода")
            .value("Hub", "Main Menu").selected("Main Menu");

    MultiSelectSetting triggerSetting = new MultiSelectSetting("Триггеры", "Выберите, в каких случаях произойдет выход")
            .value("Players", "Staff").selected("Players", "Staff");

    SliderSettings distanceSetting = new SliderSettings("Максимальная дистанция", "Максимальная дистанция для активации авто-выхода")
            .setValue(10).range(5, 40).visible(() -> triggerSetting.isSelected("Players"));

    public AutoLeave() {
        super("AutoLeave", "Auto Leave", ModuleCategory.MISC);
        settings(leaveType, triggerSetting, distanceSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPacket(PacketEvent e) {
        if (e.getType() != PacketEvent.Type.RECEIVE) return;
        if (!(e.getPacket() instanceof GameMessageS2CPacket)) return;
        
        // Проверяем Staff триггер
        if (triggerSetting.isSelected("Staff")) {
            String message = ((GameMessageS2CPacket) e.getPacket()).content().getString();
            if (isStaffDetected(message)) {
                leave(Text.of("Staff detected"));
                e.cancel();
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isStaffDetected(String message) {
        String lower = message.toLowerCase();
        // Проверяем сообщение о входе стаффера
        if (lower.contains("вошёл") || lower.contains("joined") || lower.contains("присоединился")) {
            for (String staff : rich.util.repository.staff.StaffUtils.getStaffNames()) {
                if (lower.contains(staff.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (Network.isPvp()) return;

        if (triggerSetting.isSelected("Players")) {
            mc.world.getPlayers().stream()
                    .filter(p -> mc.player.distanceTo(p) < distanceSetting.getValue() 
                            && mc.player != p 
                            && !FriendUtils.isFriend(p))
                    .findFirst()
                    .ifPresent(p -> leave(p.getName().copy().append(" - Появился рядом " + String.format("%.1f", mc.player.distanceTo(p)) + "м")));
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public void leave(Text text) {
        switch (leaveType.getSelected()) {
            case "Hub" -> {
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendChatCommand("hub");
                }
            }
            case "Main Menu" -> {
                if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
                    mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
                }
            }
        }
        setState(false);
    }
}