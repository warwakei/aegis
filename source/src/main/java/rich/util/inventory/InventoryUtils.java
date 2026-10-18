package rich.util.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.client.network.PendingUpdateManager;
import rich.mixin.ClientWorldAccessor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class InventoryUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static int savedSlot = -1;
    private static int silentSlot = -1;

    private InventoryUtils() {}

    public static int findItemInHotbar(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findItemInInventory(Item item) {
        if (mc.player == null) return -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findItemAnywhere(Item item) {
        int hotbar = findItemInHotbar(item);
        if (hotbar != -1) return hotbar;
        return findItemInInventory(item);
    }

    public static InventoryResult find(Item item) {
        return find(stack -> stack.getItem() == item);
    }

    public static InventoryResult find(Item... items) {
        return find(Arrays.asList(items));
    }

    public static InventoryResult find(List<Item> items) {
        return find(stack -> items.contains(stack.getItem()));
    }

    public static boolean hasElytra() {
        if (mc.player == null) return false;
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).get(DataComponentTypes.GLIDER) != null;
    }

    public static int findHotbarItem(Item item) {
        if (mc.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findElytraSlot() {
        if (mc.player == null) return -1;

        for (int i = 0; i < 46; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    public static int findChestArmorSlot() {
        if (mc.player == null) return -1;

        for (int i = 0; i < 46; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
            if (component != null && component.slot() == EquipmentSlot.CHEST && stack.getItem() != Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    public static InventoryResult find(ItemSearcher searcher) {
        if (mc.player == null) return InventoryResult.notFound();

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (isValid(stack) && searcher.matches(stack)) {
                return new InventoryResult(-2, true, stack);
            }
        }

        for (int i = 35; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValid(stack) && searcher.matches(stack)) {
                int slot = i < 9 ? i + 36 : i;
                return InventoryResult.of(slot, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findHotbar(Item item) {
        return findHotbar(stack -> stack.getItem() == item);
    }

    public static InventoryResult findHotbar(ItemSearcher searcher) {
        if (mc.player == null) return InventoryResult.notFound();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValid(stack) && searcher.matches(stack)) {
                return InventoryResult.of(i, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static Slot findSlot(Item item) {
        return findSlot(s -> s.getStack().getItem() == item, null);
    }

    public static Slot findSlot(Predicate<Slot> filter) {
        return findSlot(filter, null);
    }

    public static Slot findSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        if (mc.player == null) return null;
        var stream = mc.player.currentScreenHandler.slots.stream().filter(filter);
        return comparator != null ? stream.max(comparator).orElse(null) : stream.findFirst().orElse(null);
    }

    public static Slot findSlot(Item item, Predicate<Slot> extraFilter, Comparator<Slot> comparator) {
        Predicate<Slot> combined = s -> s.getStack().getItem() == item && extraFilter.test(s);
        return findSlot(combined, comparator);
    }

    public static Slot findSlotInHotbar(Item item) {
        if (mc.player == null) return null;
        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty() && slot.getStack().getItem() == item) {
                return slot;
            }
        }
        return null;
    }

    public static Slot findSlotInInventory(Item item) {
        if (mc.player == null) return null;
        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty() && slot.getStack().getItem() == item) {
                return slot;
            }
        }
        return null;
    }

    public static Slot findSlotAnywhere(Item item) {
        Slot hotbar = findSlotInHotbar(item);
        if (hotbar != null) return hotbar;
        return findSlotInInventory(item);
    }

    public static Slot findRegularTotemSlot() {
        if (mc.player == null) return null;

        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty()
                    && slot.getStack().getItem() == Items.TOTEM_OF_UNDYING
                    && !slot.getStack().hasEnchantments()) {
                return slot;
            }
        }

        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty()
                    && slot.getStack().getItem() == Items.TOTEM_OF_UNDYING
                    && !slot.getStack().hasEnchantments()) {
                return slot;
            }
        }

        return null;
    }

    public static Slot findEnchantedTotemSlot() {
        if (mc.player == null) return null;

        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty()
                    && slot.getStack().getItem() == Items.TOTEM_OF_UNDYING
                    && slot.getStack().hasEnchantments()) {
                return slot;
            }
        }

        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty()
                    && slot.getStack().getItem() == Items.TOTEM_OF_UNDYING
                    && slot.getStack().hasEnchantments()) {
                return slot;
            }
        }

        return null;
    }

    public static Slot findTotemSlot(boolean preferNonEnchanted) {
        if (mc.player == null) return null;

        Slot regularTotem = null;
        Slot enchantedTotem = null;

        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty() && slot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                if (!slot.getStack().hasEnchantments()) {
                    if (regularTotem == null) regularTotem = slot;
                } else {
                    if (enchantedTotem == null) enchantedTotem = slot;
                }
            }
        }

        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty() && slot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                if (!slot.getStack().hasEnchantments()) {
                    if (regularTotem == null) regularTotem = slot;
                } else {
                    if (enchantedTotem == null) enchantedTotem = slot;
                }
            }
        }

        if (preferNonEnchanted) {
            return regularTotem != null ? regularTotem : enchantedTotem;
        } else {
            return regularTotem != null ? regularTotem : enchantedTotem;
        }
    }

    public static boolean hasEnchantedTotemInOffhand() {
        if (mc.player == null) return false;
        ItemStack offhand = mc.player.getOffHandStack();
        return offhand.getItem() == Items.TOTEM_OF_UNDYING && offhand.hasEnchantments();
    }

    public static boolean hasRegularTotemInOffhand() {
        if (mc.player == null) return false;
        ItemStack offhand = mc.player.getOffHandStack();
        return offhand.getItem() == Items.TOTEM_OF_UNDYING && !offhand.hasEnchantments();
    }

    public static void swap(int from, int to) {
        click(from, 0, SlotActionType.PICKUP);
        click(to, 0, SlotActionType.PICKUP);
        click(from, 0, SlotActionType.PICKUP);
    }

    public static void swapHotbar(int slot, int hotbarSlot) {
        click(slot, hotbarSlot, SlotActionType.SWAP);
    }

    public static void swapToOffhand(int slot) {
        click(slot, 40, SlotActionType.SWAP);
    }

    public static void swapToOffhand(Slot slot) {
        if (slot != null) {
            click(slot.id, 40, SlotActionType.SWAP);
        }
    }

    public static void swapOffhandWithSlot(int slotId) {
        if (mc.player == null || mc.interactionManager == null) return;
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slotId, 40, SlotActionType.SWAP, mc.player);
    }

    public static void moveToSlot(int from, int to) {
        swap(from, to);
    }

    public static void click(int slot, int button, SlotActionType type) {
        if (mc.player == null || mc.interactionManager == null || slot == -1) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, type, mc.player);
    }

    public static void selectSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
        }
    }

    public static void selectSlotSilent(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null || slot < 0 || slot > 8) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void saveSlot() {
        if (mc.player != null) {
            savedSlot = mc.player.getInventory().getSelectedSlot();
        }
    }

    public static void restoreSlot() {
        if (savedSlot != -1) {
            selectSlot(savedSlot);
            savedSlot = -1;
        }
    }

    public static void restoreSlotSilent() {
        if (savedSlot != -1) {
            selectSlotSilent(savedSlot);
            savedSlot = -1;
        }
    }

    public static void silentUseHotbarItem(int hotbarSlot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (hotbarSlot != currentSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
        }

        sendUsePacket(Hand.MAIN_HAND);

        if (hotbarSlot != currentSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
        }
    }

    public static void silentSwapUseAndReturn(int inventorySlot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        int currentHotbarSlot = mc.player.getInventory().getSelectedSlot();

        click(inventorySlot, currentHotbarSlot, SlotActionType.SWAP);

        sendUsePacket(Hand.MAIN_HAND);

        click(inventorySlot, currentHotbarSlot, SlotActionType.SWAP);
    }

    public static void silentUseItem(Item item) {
        if (mc.player == null) return;

        int hotbarSlot = findItemInHotbar(item);
        if (hotbarSlot != -1) {
            silentUseHotbarItem(hotbarSlot);
            return;
        }

        int invSlot = findItemInInventory(item);
        if (invSlot != -1) {
            int wrappedSlot = wrapSlot(invSlot);
            silentSwapUseAndReturn(wrappedSlot);
            closeScreen();
        }
    }

    public static void sendUsePacket(Hand hand) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.world == null) return;

        try {
            ClientWorldAccessor worldAccessor = (ClientWorldAccessor) mc.world;
            PendingUpdateManager pendingUpdateManager = worldAccessor.getPendingUpdateManager().incrementSequence();

            int sequence = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                    hand,
                    sequence,
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));

            pendingUpdateManager.close();
        } catch (Exception e) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                    hand,
                    0,
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));
        }
    }

    public static void use(Hand hand) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.interactItem(mc.player, hand);
    }

    public static void closeScreen() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    public static boolean isScreenOpen() {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen);
    }

    public static int wrapSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public static int currentSlot() {
        return mc.player != null ? mc.player.getInventory().getSelectedSlot() : 0;
    }

    public static ItemStack offhandStack() {
        return mc.player != null ? mc.player.getOffHandStack() : ItemStack.EMPTY;
    }

    public static ItemStack mainhandStack() {
        return mc.player != null ? mc.player.getMainHandStack() : ItemStack.EMPTY;
    }

    public static boolean hasTotemInOffhand() {
        return mc.player != null && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
    }

    public static Item getOffhandItem() {
        return mc.player != null ? mc.player.getOffHandStack().getItem() : Items.AIR;
    }

    private static boolean isValid(ItemStack stack) {
        return !stack.isEmpty() && stack.getDamage() < stack.getMaxDamage() - 10;
    }
}