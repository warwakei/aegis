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
 * Автоматически выбрасывает мусор (компас + книгу/зачарованную книгу) из первых двух слотов хотбара при заходе на сервера Jenro
 */
public class JenroTrasher extends ModuleStructure {

    // Мусорные предметы
    private static final Item[] TRASH_ITEMS = {
            Items.COMPASS,
            Items.WRITTEN_BOOK,
            Items.ENCHANTED_BOOK
    };

    private boolean trashDropped = false;
    private int tickDelay = 0;

    public JenroTrasher() {
        super("Jenro Trasher", "Автоматически выбрасывает мусор из инвентаря", ModuleCategory.MISC);
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
     * Проверяет является ли предмет мусорным (компас/книга/зачарованная книга)
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

        // Небольшая задержка чтобы инвентарь успел засинхронизироваться
        if (tickDelay < 10) {
            tickDelay++;
            return;
        }

        var inventory = mc.player.getInventory();
        int syncId = mc.player.currentScreenHandler.syncId;
        boolean foundTrash = false;

        // Проверяем слот 0 хотбара
        var slot0 = inventory.getStack(0);
        if (isTrashItem(slot0)) {
            // Хотбар слот 0 = 36 в currentScreenHandler
            mc.interactionManager.clickSlot(syncId, 36, 1, SlotActionType.THROW, mc.player);
            foundTrash = true;
        }

        // Проверяем слот 1 хотбара
        var slot1 = inventory.getStack(1);
        if (isTrashItem(slot1)) {
            // Хотбар слот 1 = 37 в currentScreenHandler
            mc.interactionManager.clickSlot(syncId, 37, 1, SlotActionType.THROW, mc.player);
            foundTrash = true;
        }

        // Если выбросили хоть один мусорный предмет - помечаем что работа выполнена
        if (foundTrash) {
            trashDropped = true;
            ChatMessage.brandmessage("Jenro Trasher: мусор выброшен");
        }
    }
}
