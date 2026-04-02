package fun.aegis.features.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;

import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.utils.client.packet.network.Network;
import fun.aegis.events.player.TickEvent;
import fun.aegis.display.hud.Notifications;
import fun.aegis.display.hud.StaffList;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoLeave extends Module {
    SelectSetting leaveType = new SelectSetting("Тип выхода", "Позволяет выбрать тип выхода")
            .value("Отключиться", "/hub").selected("Отключиться");

    MultiSelectSetting triggerSetting = new MultiSelectSetting("Триггеры", "Выберите, в каких случаях произойдет выход")
            .value("Игрок рядом", "Стафф на сервере").selected("Игрок рядом", "Стафф на сервере");

    SliderSettings distanceSetting = new SliderSettings("Максимальная дистанция", "Максимальная дистанция для активации авто-выхода")
            .setValue(10).range(5, 40).visible(() -> triggerSetting.isSelected("Игрок рядом"));

    public AutoLeave() {
        super("AutoLeave", "Auto Leave", ModuleCategory.MISC);
        setup(leaveType, triggerSetting, distanceSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (Network.isPvp()) return;

        if (triggerSetting.isSelected("Игрок рядом")) {
            mc.world.getPlayers().stream()
                    .filter(p -> mc.player.distanceTo(p) < distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p))
                    .findFirst()
                    .ifPresent(p -> leave(Text.of("Игрок рядом: " + p.getName().getString())));
        }

        if (triggerSetting.isSelected("Стафф на сервере") && !StaffList.getInstance().list.isEmpty()) {
            leave(Text.of("Стафф на сервере"));
        }
    }

    public void leave(Text text) {
        switch (leaveType.getSelected()) {
            case "/hub" -> {
                Notifications.getInstance().addList(Text.of("[AutoLeave] ").copy().append(text), 10000);
                mc.getNetworkHandler().sendChatCommand("hub");
            }
            case "Отключиться" ->
                    mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
        }
        setState(false);
    }
}
