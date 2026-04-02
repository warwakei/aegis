package rich.events.api.types;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.events.impl.UsingItemEvent;

public class EventListener implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        Initialization.getInstance().getManager().getAttackPerpetrator().tick();
        if (Initialization.getInstance().getManager().getHudManager() != null) {
            Initialization.getInstance().getManager().getHudManager().tick();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClientCommandC2SPacket command -> serverSprint = switch (command.getMode()) {
                case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                default -> serverSprint;
            };
            case UpdateSelectedSlotC2SPacket slot -> selectedSlot = slot.getSelectedSlot();
            default -> {}
        }

        Initialization.getInstance().getManager().getAttackPerpetrator().onPacket(e);
        Initialization.getInstance().getManager().getHudManager().onPacket(e);
    }

    @EventHandler
    public void onUsingItemEvent(UsingItemEvent e) {
        Initialization.getInstance().getManager().getAttackPerpetrator().onUsingItem(e);
    }

}