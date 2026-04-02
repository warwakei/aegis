package fun.aegis.utils.interactions.inv;

import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.interactions.item.ItemTask;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.display.hud.Notifications;
import fun.aegis.utils.features.aura.utils.MathAngle;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InventoryTask implements QuickImports {
    public static void moveItem(Slot from, int to) {
        if (from == null) return;
        moveItem(from.id, to, false, false);
    }

    public static void moveItem(Slot from, int to, boolean task) {
        moveItem(from, to, task, false);
    }

    public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
        if (from == null) return;
        moveItem(from.id, to, task, updateInventory);
    }

    public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
        if (from == to || from == -1) return;
        int count = Math.toIntExact(slots().count()) - 10;
        if (from >= count && count == 36) {
            if (task) InventoryFlowManager.addTask(() -> clickSlot(to, from - count, SlotActionType.SWAP, false));
            else clickSlot(to, from - count, SlotActionType.SWAP, false);
            return;
        }
        if (task) InventoryFlowManager.addTask(() -> moveItem(from, to, updateInventory));
        else moveItem(from, to, updateInventory);
    }

    public static void moveItem(int from, int to, boolean updateInventory) {
        clickSlot(from, 0, SlotActionType.SWAP, false);
        clickSlot(to, 0, SlotActionType.SWAP, false);
        clickSlot(from, 0, SlotActionType.SWAP, false);
        if (updateInventory) updateSlots();
    }

    public static void swapHand(Slot slot, Hand hand, boolean task) {
        swapHand(slot, hand, task, false);
    }

    public static void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
        if (slot == null || slot.id == -1) return;
        
        if (hand.equals(Hand.OFF_HAND) && !(slot.inventory instanceof PlayerInventory || slot.inventory instanceof EnderChestInventory)) {
            return;
        }
        
        if (mc.player == null) return;
        
        int button = hand.equals(Hand.MAIN_HAND) ? mc.player.getInventory().selectedSlot : 40;
        if (task) InventoryFlowManager.addTask(() -> swap(slot, button, updateInventory));
        else swap(slot, button, updateInventory);
    }

    public static void swap(Slot slot, int button, boolean updateInventory) {
        if (slot == null) return;
        clickSlot(slot, button, SlotActionType.SWAP, false);
        if (updateInventory) InventoryTask.updateSlots();
    }

    public static void swapAndUse(Slot slot, String text, boolean task) {
        if (slot == null) {
            if (Notifications.getInstance() != null) {
                Notifications.getInstance().addList(Formatting.RED + text + Formatting.RESET + " - не найден!", 3000);
            }
            return;
        }
        if (task) InventoryFlowManager.addTask(() -> swapAndUse(slot, MathAngle.cameraAngle()));
        else swapAndUse(slot, MathAngle.cameraAngle());
    }

    public static void swapAndUse(Item item) {
        swapAndUse(item, MathAngle.cameraAngle(), false);
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, type, mc.player);
    }

    public static void switchTo(int slot) {
        if (mc.player == null) return;
        
        if (mc.player.getInventory().selectedSlot == slot) return;
        mc.player.getInventory().selectedSlot = slot;
    }

    public static void swapAndUse(Item item, String searchName, boolean task) {
        float cooldownProgress = ItemTask.getCooldownProgress(item);
        if (cooldownProgress > 0) {
            String time = Calculate.round(cooldownProgress, 0.1) + "с";
            if (Notifications.getInstance() != null) {
                Notifications.getInstance().addList(Formatting.RED + item.getName().getString() + Formatting.RESET + " - в кд еще " + time, 2000);
            }
            return;
        }
        Slot slot = getSlot(s -> s.getStack().getItem().equals(item) && getCleanName(s.getStack().getName()).contains(searchName.toLowerCase()));
        if (slot == null) {
            if (Notifications.getInstance() != null) {
                Notifications.getInstance().addList(Formatting.RED + item.getName().getString() + Formatting.RESET + " - не найден!", 2000);
            }
            return;
        }
        if (task) InventoryFlowManager.addTask(() -> swapAndUse(slot, MathAngle.cameraAngle()));
        else swapAndUse(slot, MathAngle.cameraAngle());
    }

    public static void swapAndUse(Item item, Turns angle, boolean task) {
        float cooldownProgress = ItemTask.getCooldownProgress(item);
        if (cooldownProgress > 0) {
            String time = Calculate.round(cooldownProgress, 0.1) + "с";
            if (Notifications.getInstance() != null) {
                Notifications.getInstance().addList(Formatting.RED + item.getName().getString() + Formatting.RESET + " - в кд еще " + time, 2000);
            }
            return;
        }
        Slot slot = getSlot(item);
        if (slot == null) {
            if (Notifications.getInstance() != null) {
                Notifications.getInstance().addList(Formatting.RED + item.getName().getString() + Formatting.RESET + " - не найден!", 2000);
            }
            return;
        }
        if (task) InventoryFlowManager.addTask(() -> swapAndUse(slot, angle));
        else swapAndUse(slot, angle);
    }

    public static void swapAndUse(Slot slot, Turns angle) {
        if (slot == null || angle == null) return;
        
        swapHand(slot, Hand.MAIN_HAND, false);
        PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
        swapHand(slot, Hand.MAIN_HAND, false, true);
    }

    public static void updateSlots() {
        if (mc.player == null || mc.player.networkHandler == null) return;
        
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (screenHandler == null) return;
        
        ItemStack stack = Registries.ITEM.get(Calculate.getRandom(0, 100)).getDefaultStack();
        if (stack == null) return;
        
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), 0, 0, SlotActionType.PICKUP_ALL, stack, Int2ObjectMaps.singleton(0, stack)));
    }

    public static void closeScreen(boolean packet) {
        if (mc.player == null) return;
        
        if (packet) {
            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            }
        } else {
            mc.player.closeHandledScreen();
        }
    }

    public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
        if (slot == null) return;
        clickSlot(slot.id, button, clickType, silent);
    }

    public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        if (mc.player == null || mc.player.currentScreenHandler == null) return;
        clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent);
    }

    public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        if (mc.interactionManager == null || mc.player == null) return;
        
        mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, mc.player);
        if (silent && mc.player.currentScreenHandler != null) {
            mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, mc.player);
        }
    }

    public static Slot getSlot(Item item) {
        return getSlot(item, s -> true);
    }

    public static Slot getSlot(Item item, Predicate<Slot> filter) {
        return getSlot(item, Comparator.comparingInt(s -> 0), filter);
    }

    public static Slot getSlot(Predicate<Slot> filter) {
        Slot result = slots().filter(filter).findFirst().orElse(null);
        return result;
    }

    public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        Slot result = slots().filter(filter).max(comparator).orElse(null);
        return result;
    }

    public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        Slot result = slots().filter(s -> s.getStack().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
        return result;
    }

    public static Slot getFoodMaxSaturationSlot() {
        Slot result = slots().filter(s -> {
            var food = s.getStack().get(DataComponentTypes.FOOD);
            return food != null && !food.canAlwaysEat();
        }).max(Comparator.comparingDouble(s -> {
            var food = s.getStack().get(DataComponentTypes.FOOD);
            return food != null ? food.saturation() : 0;
        })).orElse(null);
        return result;
    }

    public static Slot getSlot(List<Item> item) {
        Slot result = slots().filter(s -> item.contains(s.getStack().getItem())).findFirst().orElse(null);
        return result;
    }

    public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
        Slot result = slots().filter(s -> {
            PotionContentsComponent component = s.getStack().get(DataComponentTypes.POTION_CONTENTS);
            if (component == null) return false;
            return StreamSupport.stream(component.getEffects().spliterator(), false).anyMatch(e -> e.getEffectType().equals(effect));
        }).findFirst().orElse(null);
        return result;
    }

    public static Slot getPotionFromCategory(StatusEffectCategory category) {
        Slot result = slots().filter(s -> {
            ItemStack stack = s.getStack();
            PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (!stack.getItem().equals(Items.SPLASH_POTION) || component == null) return false;
            StatusEffectCategory category2 = category.equals(StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> e.getEffectType().value().getCategory().equals(category)).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> e.getEffectType().value().getCategory().equals(category2)).count();
            return effects >= effects2;
        }).findFirst().orElse(null);
        return result;
    }

    public static int getInventoryCount(Item item) {
        if (mc.player == null) return 0;
        int count = IntStream.range(0, 45).filter(i -> Objects.requireNonNull(mc.player).getInventory().getStack(i).getItem().equals(item)).map(i -> mc.player.getInventory().getStack(i).getCount()).sum();
        return count;
    }

    public static int getHotbarItems(List<Item> items) {
        if (mc.player == null) return -1;
        int result = IntStream.range(0, 9).filter(i -> items.contains(mc.player.getInventory().getStack(i).getItem())).findFirst().orElse(-1);
        return result;
    }

    public static int getHotbarSlotId(IntPredicate filter) {
        int result = IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
        return result;
    }

    public static int getCount(Predicate<Slot> filter) {
        return slots().filter(filter).mapToInt(s -> s.getStack().getCount()).sum();
    }

    public static Slot mainHandSlot() {
        if (mc.player == null) return null;
        long count = slots().count();
        int i = count == 46 ? 10 : 9;
        List<Slot> slotList = slots().toList();
        if (slotList.isEmpty()) return null;
        int index = Math.toIntExact(count - i + mc.player.getInventory().selectedSlot);
        if (index < 0 || index >= slotList.size()) return null;
        return slotList.get(index);
    }

    public static boolean isServerScreen() {
        return slots().toList().size() != 46;
    }

    public static Stream<Slot> slots() {
        return mc.player.currentScreenHandler.slots.stream();
    }

    public static void selectCompass() {
        if (mc.player == null) return;
        
        Slot slot = InventoryTask.getSlot(Items.COMPASS);
        if (slot != null) {
            mc.player.getInventory().selectedSlot = slot.id < 9 ? slot.id : 0;
            InventoryTask.swapHand(slot, Hand.MAIN_HAND, false, true);
        }
    }

    public static String getCleanName(Text text) {
        if (text == null) return "";
        String name = text.getString();
        if (name == null) return "";
        String cleaned = name.replaceAll("§[0-9a-fk-or]", "").toLowerCase();
        return cleaned != null ? cleaned : "";
    }
}
