package fun.aegis.main.listener.impl;

import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.utils.client.packet.network.Network;
import fun.aegis.Aegis;
import fun.aegis.main.listener.Listener;
import fun.aegis.events.item.UsingItemEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.TickEvent;

public class EventListener implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        Network.tick();
        Aegis.getInstance().getAttackPerpetrator().tick();
        InventoryFlowManager.tick();
        Aegis.getInstance().getDraggableRepository().draggable().forEach(AbstractDraggable::tick);
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
        Network.packet(e);
        Aegis.getInstance().getAttackPerpetrator().onPacket(e);
        Aegis.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.packet(e));
    }

    @EventHandler
    public void onUsingItemEvent(UsingItemEvent e) {
        Aegis.getInstance().getAttackPerpetrator().onUsingItem(e);
    }
}
