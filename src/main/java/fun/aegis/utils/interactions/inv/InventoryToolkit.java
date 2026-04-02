package fun.aegis.utils.interactions.inv;

import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import fun.aegis.utils.display.interfaces.QuickImports;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class InventoryToolkit implements QuickImports {
    private static int cachedSlot = -1;

//    public static int findPotionSlot(AutoPotion.PotionType type) {
//        int hbSlot = getPotionIndexHb(type);
//        if (hbSlot != -1) {
//            return hbSlot;
//        }
//        int invSlot = getPotionIndexInv(type);
//        if (invSlot != -1) {
//            return invSlot;
//        }
//        return -1;
//    }
//
//    public static boolean isPotionAvailable(AutoPotion.PotionType type) {
//        return getPotionIndexInv(type) != -1 || getPotionIndexHb(type) != -1;
//    }
//
//    public static int getPotionIndexHb(AutoPotion.PotionType type) {
//        for (int i = 0; i < 9; ++i) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            if (stack.getItem() == Items.SPLASH_POTION) {
//                Potion potion = stack.getComponents().get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS).potion().map(RegistryEntry::value).orElse(null);
//                if (potion != null) {
//                    if (type == AutoPotion.PotionType.HEALING && (potion == Potions.HEALING || potion == Potions.STRONG_HEALING)) {
//                        return i;
//                    }
//                    for (StatusEffectInstance effect : potion.getEffects()) {
//                        RegistryEntry<StatusEffect> effectType = effect.getEffectType();
//                        if (type.getEffect() != null && effectType.value() == type.getEffect().value()) {
//                            return i;
//                        }
//                    }
//                }
//            }
//        }
//        return -1;
//    }

    public static int getItemSlot(Item input) {
        for (ItemStack stack : mc.player.getInventory().armor) {
            if (stack.getItem() == input && stack.getDamage() < 430) {
                return -2;
            }
        }
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == input && stack.getDamage() < 430) {
                slot = i;
                break;
            }
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

//    public static int getPotionIndexInv(AutoPotion.PotionType type) {
//        for (int i = 9; i < 36; ++i) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            if (stack.getItem() == Items.SPLASH_POTION) {
//                Potion potion = stack.getComponents().get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS).potion().map(RegistryEntry::value).orElse(null);
//                if (potion != null) {
//                    if (type == AutoPotion.PotionType.HEALING && (potion == Potions.HEALING || potion == Potions.STRONG_HEALING)) {
//                        return i;
//                    }
//                    for (StatusEffectInstance effect : potion.getEffects()) {
//                        RegistryEntry<StatusEffect> effectType = effect.getEffectType();
//                        if (type.getEffect() != null && effectType.value() == type.getEffect().value()) {
//                            return i;
//                        }
//                    }
//                }
//            }
//        }
//        return -1;
//    }

    public static void moveTo(int syncId, ItemStack stack, int slot) {
        if (Objects.isNull(stack)) return;
        mc.interactionManager.clickSlot(syncId, mc.player.getInventory().getSlotWithStack(stack), 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    public static InventoryResult findInInventory(Searcher searcher) {
        if (mc.player != null) {
            for (ItemStack stack : mc.player.getInventory().armor) {
                if (searcher.isValid(stack) && stack.getDamage() < 430) {
                    return new InventoryResult(-2, true, stack);
                }
            }
            for (int i = 36; i >= 0; i--) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (searcher.isValid(stack) && stack.getDamage() < 430) {
                    if (i < 9) i += 36;
                    return new InventoryResult(i, true, stack);
                }
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInInventory(List<Item> items) {
        return findInInventory(stack -> items.contains(stack.getItem()));
    }

    public static InventoryResult findItemInInventory(Item... items) {
        return findItemInInventory(Arrays.asList(items));
    }

    public static int getAxe() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public static InventoryResult findItemInHotBar(Item item) {
        if (mc.player == null) return InventoryResult.notFound();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item && stack.getDamage() < 430) {
                return new InventoryResult(i, true, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInInventory(Item item) {
        if (mc.player == null) return InventoryResult.notFound();
        for (ItemStack stack : mc.player.getInventory().armor) {
            if (stack.getItem() == item && stack.getDamage() < 430) {
                return new InventoryResult(-2, true, stack);
            }
        }
        for (int i = 36; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item && stack.getDamage() < 430) {
                if (i < 9) i += 36;
                return new InventoryResult(i, true, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findInHotBar(Searcher searcher) {
        if (mc.player != null) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (searcher.isValid(stack) && stack.getDamage() < 430) {
                    return new InventoryResult(i, true, stack);
                }
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInHotBar(List<Item> items) {
        return findInHotBar(stack -> items.contains(stack.getItem()));
    }

    public static InventoryResult findItemInHotBar(Item... items) {
        return findItemInHotBar(Arrays.asList(items));
    }

    public static void saveSlot() {
        cachedSlot = mc.player.getInventory().selectedSlot;
    }

    public static void returnSlot() {
        if (cachedSlot != -1) switchTo(cachedSlot);
        cachedSlot = -1;
    }

    public static void switchTo(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.getInventory().selectedSlot == slot) return;
        mc.player.getInventory().selectedSlot = slot;
    }

    public static void switchToSilent(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void clickSlot(int id) {
        if (id == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, 0, SlotActionType.PICKUP, mc.player);
    }

    public static void clickSlot(int id, SlotActionType type) {
        if (id == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, 0, type, mc.player);
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, type, mc.player);
    }

    public static ItemStack byItem(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem().equals(item) && itemStack.getDamage() < 430) return itemStack;
        }
        return null;
    }

    public static boolean quickMoveFromTo(int from, int to) {
        if (from == -1 || to == -1 || mc.interactionManager == null || mc.player == null) return false;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, to, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, mc.player);
        return true;
    }

    public static int getSlotWithStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack invStack = mc.player.getInventory().getStack(i);
            if (ItemStack.areEqual(invStack, stack)) {
                return i;
            }
        }
        return -1;
    }

    public interface Searcher {
        boolean isValid(ItemStack stack);
    }
}
