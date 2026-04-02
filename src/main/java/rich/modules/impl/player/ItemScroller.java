package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.ClickSlotEvent;
import rich.events.impl.HandledScreenEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.InventoryUtils;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemScroller extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    SliderSettings scrollerSetting = new SliderSettings("Задержка прокрутки предметов", "Выберите задержку прокрутки предметов")
            .setValue(50).range(0, 200);

    public ItemScroller() {
        super("ItemScroller", "Item Scroller", ModuleCategory.PLAYER);
        settings(scrollerSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onHandledScreen(HandledScreenEvent e) {
        if (mc.player == null) return;

        Slot hoverSlot = e.getSlotHover();
        SlotActionType actionType = getActionType();

        if (PlayerInteractionHelper.isKey(mc.options.sneakKey)
                && !PlayerInteractionHelper.isKey(mc.options.sprintKey)
                && hoverSlot != null
                && hoverSlot.hasStack()
                && actionType != null
                && stopWatch.every(scrollerSetting.getValue())) {

            InventoryUtils.click(
                    hoverSlot.id,
                    actionType.equals(SlotActionType.THROW) ? 1 : 0,
                    actionType
            );
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private SlotActionType getActionType() {
        return PlayerInteractionHelper.isKey(mc.options.dropKey)
                ? SlotActionType.THROW
                : PlayerInteractionHelper.isKey(mc.options.attackKey)
                ? SlotActionType.QUICK_MOVE
                : null;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onClickSlot(ClickSlotEvent e) {
        if (mc.player == null) return;

        int slotId = e.getSlotId();
        if (slotId < 0 || slotId >= mc.player.currentScreenHandler.slots.size()) return;

        Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
        Item item = slot.getStack().getItem();

        if (item != null
                && PlayerInteractionHelper.isKey(mc.options.sneakKey)
                && PlayerInteractionHelper.isKey(mc.options.sprintKey)
                && stopWatch.every(50)) {

            processSlotClick(slot, item, e);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processSlotClick(Slot slot, Item item, ClickSlotEvent e) {
        getSlots()
                .filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
                .forEach(s -> InventoryUtils.click(s.id, 1, e.getActionType()));
    }

    private Stream<Slot> getSlots() {
        return mc.player.currentScreenHandler.slots.stream();
    }
}