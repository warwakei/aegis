package fun.aegis.features.impl.player;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.keyboard.HotBarScrollEvent;
import fun.aegis.events.player.HotBarUpdateEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.ItemRendererEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.inv.InventoryTask;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import java.util.Objects;

public class ItemFixSwap extends Module {
    private boolean lockActive = false;
    private int lockSlot = -1;
    private int deferredSlot = -1;
    private int lastSentSlot = -1;
    private ItemStack renderStack = null;
    private boolean pendingWorldApply = false;
    private Object lastWorldRef = null;
    private int lastServerSlot = -1;

    public ItemFixSwap() {
        super("ItemFixSwap", "ItemFixSwap", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onItemRenderer(ItemRendererEvent e) {
        if (lockActive && e.getHand() == Hand.MAIN_HAND && mc.player != null && Objects.equals(mc.player, e.getPlayer()) && renderStack != null) {
            e.setStack(renderStack);
        }
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        e.cancel();
        if (mc.player != null && !lockActive && lastServerSlot != -1) {
            mc.player.getInventory().selectedSlot = lastServerSlot;
        }
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (shouldLock()) {
            e.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (lastWorldRef != mc.world) {
            lastWorldRef = mc.world;
            if (mc.world != null) {
                pendingWorldApply = true;
                lastSentSlot = -1;
                lastServerSlot = mc.player != null ? mc.player.getInventory().selectedSlot : 0;
            }
        }

        if (mc.world == null || mc.player == null) return;

        if (pendingWorldApply) {
            if (lastServerSlot == -1) {
                lastServerSlot = mc.player.getInventory().selectedSlot;
            }
            if (deferredSlot != -1) {
                setSelectedSlot(deferredSlot, true);
            } else {
                ensureSelectedSynced(true);
            }
            pendingWorldApply = false;
        }

        boolean usingMain = isUsingMainhand();
        int currentSelected = mc.player.getInventory().selectedSlot;

        if (usingMain) {
            if (!lockActive) {
                lockActive = true;
                lockSlot = clamp(currentSelected);
                renderStack = mc.player.getMainHandStack().copy();
                sendSelected(lockSlot, true);
            } else {
                if (currentSelected != lockSlot) {
                    deferredSlot = clamp(currentSelected);
                    mc.player.getInventory().selectedSlot = lockSlot;
                }
            }
        } else {
            if (lockActive) {
                lockActive = false;
                if (deferredSlot != -1 && deferredSlot != lockSlot) {
                    setSelectedSlot(deferredSlot, true);
                    InventoryTask.updateSlots();
                } else {
                    mc.player.getInventory().selectedSlot = lastServerSlot;
                }
                lockSlot = -1;
                deferredSlot = -1;
                renderStack = null;
            }
        }
    }

    private boolean shouldLock() {
        return lockActive || isUsingMainhand();
    }

    private boolean isUsingMainhand() {
        return mc.player != null && mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND;
    }

    private void ensureSelectedSynced(boolean force) {
        if (mc.player == null) return;
        int s = clamp(mc.player.getInventory().selectedSlot);
        if (force || lastSentSlot != s) {
            sendSelected(s, true);
        }
    }

    private void setSelectedSlot(int idx, boolean force) {
        if (mc.player == null) return;
        idx = clamp(idx);
        mc.player.getInventory().selectedSlot = idx;
        sendSelected(idx, true);
    }

    private void sendSelected(int idx, boolean force) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        if (!force && lastSentSlot == idx) return;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(idx));
        lastSentSlot = idx;
        lastServerSlot = idx;
    }

    private int clamp(int idx) {
        if (idx < 0) return 0;
        if (idx > 8) return 8;
        return idx;
    }
}
