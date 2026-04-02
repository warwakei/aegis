package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.network.Network;
import rich.util.repository.friend.FriendUtils;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTpAccept extends ModuleStructure {
    private final String[] teleportMessages = new String[]{
            "has requested teleport",
            "просит телепортироваться",
            "хочет телепортироваться к вам",
            "просит к вам телепортироваться"
    };
    private boolean canAccept;

    private final BooleanSetting friendSetting = new BooleanSetting("Только друзья", "Будет принимать запросы только от друзей").setValue(true);

    public AutoTpAccept() {
        super("AutoTpAccept", "Auto Tp Accept", ModuleCategory.MISC);
        settings(friendSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof GameMessageS2CPacket m) {
            String message = m.content().getString();
            boolean validPlayer = !friendSetting.isValue() || FriendUtils.getFriends().stream().anyMatch(s -> message.contains(s.getName()));
            if (isTeleportMessage(message)) {
                canAccept = validPlayer;
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onTick(TickEvent e) {
        if (!Network.isPvp() && canAccept) {
            mc.player.networkHandler.sendChatCommand("tpaccept");
            canAccept = false;
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
    }
}