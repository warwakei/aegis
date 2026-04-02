package fun.aegis.utils.interactions.item;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Hand;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.Aegis;
import fun.aegis.events.packet.PacketEvent;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemToolkit implements QuickImports {
    public final static ItemToolkit INSTANCE = new ItemToolkit();
    public boolean useItem, releaseItem = true;

    public ItemToolkit() {
        Aegis.getInstance().getEventManager().register(this);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case PlayerActionC2SPacket player when player.getAction().equals(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) -> releaseItem = true;
            case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) -> releaseItem = true;
            case PlayerRespawnS2CPacket respawn -> releaseItem = true;
            case GameJoinS2CPacket join -> releaseItem = true;
            default -> {}
        }
    }

    public void useHand(Hand hand) {
        if (releaseItem) {
            mc.interactionManager.interactItem(mc.player, hand);
            releaseItem = false;
        }
        useItem = true;
    }
}
