package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.string.chat.ChatMessage;

/**
 * Автоматически выбрасывает мусор (компас + книгу) из первых двух слотов хотбара при заходе на сервера Jenro
 */
public class JenroTrasher extends ModuleStructure {

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

        // Проверяем слот 0 хотбара - компас
        var slot0 = inventory.getStack(0);
        // Проверяем слот 1 хотбара - зачарованная книга
        var slot1 = inventory.getStack(1);

        boolean hasCompass = !slot0.isEmpty() && slot0.getItem() == Items.COMPASS;
        boolean hasEnchantedBook = !slot1.isEmpty() && slot1.getItem() == Items.ENCHANTED_BOOK;

        // Выбрасываем только если оба предмета на своих местах
        if (hasCompass && hasEnchantedBook) {
            // Хотбар слоты в currentScreenHandler имеют индексы 36-44 (0-8 хотбара)
            int syncId = mc.player.currentScreenHandler.syncId;

            // Выбрасываем компас (слот 0 хотбара = 36 в currentScreenHandler)
            mc.interactionManager.clickSlot(syncId, 36, 1, SlotActionType.THROW, mc.player);

            // Выбрасываем книгу (слот 1 хотбара = 37 в currentScreenHandler)
            mc.interactionManager.clickSlot(syncId, 37, 1, SlotActionType.THROW, mc.player);

            trashDropped = true;
            ChatMessage.brandmessage("Jenro Trasher: мусор выброшен");
        }
    }
}
