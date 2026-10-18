package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.string.chat.ChatMessage;

/**
 * выкидываем мусор с хотбара
 * компас книжки на дженро
 */
public class JenroTrasher extends ModuleStructure {

    // хлам какой выкидываем
    private static final Item[] TRASH_ITEMS = {
            Items.COMPASS,
            Items.WRITTEN_BOOK,
            Items.ENCHANTED_BOOK
    };

    private boolean trashDropped = false;
    private int tickDelay = 0;

    public JenroTrasher() {
        super("Jenro Trasher", "Автоматически выбрасывает мусор из инвентаря", ModuleCategory.JENRO);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        trashDropped = false;
        tickDelay = 0;
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        trashDropped = false;
        tickDelay = 0;
    }

    /**
     * чек хлам ли это
     */
    private boolean isTrashItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        for (Item trashItem : TRASH_ITEMS) {
            if (item == trashItem) return true;
        }
        return false;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            setState(false);
            return;
        }

        if (trashDropped) return;

        // ждем чтоб инв синхнулся
        if (tickDelay < 10) {
            tickDelay++;
            return;
        }

        var inventory = mc.player.getInventory();
        int syncId = mc.player.currentScreenHandler.syncId;
        boolean foundTrash = false;

        // чекаем слот 0
        var slot0 = inventory.getStack(0);
        if (isTrashItem(slot0)) {
            // слот 0 это 36 там
            mc.interactionManager.clickSlot(syncId, 36, 1, SlotActionType.THROW, mc.player);
            foundTrash = true;
        }

        // чекаем слот 1
        var slot1 = inventory.getStack(1);
        if (isTrashItem(slot1)) {
            // слот 1 это 37 там
            mc.interactionManager.clickSlot(syncId, 37, 1, SlotActionType.THROW, mc.player);
            foundTrash = true;
        }

        // выкинули че то работа done
        if (foundTrash) {
            trashDropped = true;
            ChatMessage.brandmessage("Jenro Trasher: мусор выброшен");
        }
    }
}
