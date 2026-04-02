package fun.aegis.utils.interactions.inv;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static fun.aegis.utils.display.interfaces.QuickImports.mc;

public record InventoryResult(int slot, boolean found, ItemStack stack) {
    private static final InventoryResult NOT_FOUND_RESULT = new InventoryResult(-1, false, null);

    public static InventoryResult notFound() {
        return NOT_FOUND_RESULT;
    }

    public static @NotNull InventoryResult inOffhand(ItemStack stack) {
        return new InventoryResult(999, true, stack);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isHolding() {
        if (mc.player == null) return false;

        return mc.player.getInventory().selectedSlot == slot;
    }

    public boolean isInHotBar() {
        return slot < 9;
    }

    public void switchTo() {
        if (found && isInHotBar())
            InventoryToolkit.switchTo(slot);
    }

    public void switchToSilent() {
        if (found && isInHotBar())
            InventoryToolkit.switchToSilent(slot);
    }
}
