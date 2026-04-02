package fun.aegis.features.impl.combat;

import fun.aegis.display.hud.DynamicIsland;
import fun.aegis.events.player.InputEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.features.impl.movement.AutoSprint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.events.keyboard.KeyEvent;

import java.util.Comparator;
import java.util.function.Predicate;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSwap extends Module {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ обхода")
            .value("Обычный", "Легит")
            .selected("Обычный");

    final BindSetting bind = new BindSetting("Кнопка свапа", "Кнопка для переключения предметов");

    final SelectSetting firstItem = new SelectSetting("Первый предмет", "Выберите первый предмет для обмена")
            .value("Тотем", "Голова", "Гепл", "Щит");

    final SelectSetting secondItem = new SelectSetting("Второй предмет", "Выберите второй предмет для обмена")
            .value("Тотем", "Голова", "Гепл", "Щит");

    enum SwapPhase { READY, SLOWING_DOWN, WAITING_STOP, SWAP, SPEEDING_UP, FINISHED }
    SwapPhase swapPhase = SwapPhase.READY;
    Slot targetSlot = null;
    long actionStartTime = 0L;
    boolean playerFullyStopped = false;
    boolean isLegitMode = false;

    public AutoSwap() {
        super("AutoSwap", "Auto Swap", ModuleCategory.COMBAT);
        setup(modeSetting, firstItem, secondItem, bind);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(bind.getKey()) && swapPhase == SwapPhase.READY) {
            Slot hotbarSlot = findValidSlot(s -> s.id >= 36 && s.id <= 44);
            Slot slotToSwap = hotbarSlot != null ? hotbarSlot : findValidSlot(s -> s.id >= 0 && s.id <= 35);
            
            if (slotToSwap != null) {
                if (modeSetting.getSelected().equals("Обычный")) {
                    startSwap(slotToSwap, false);
                } else {
                    startSwap(slotToSwap, true);
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (swapPhase != SwapPhase.READY) {
            processSwapTick();
        }
    }
    
    @EventHandler
    public void onInput(InputEvent e) {
        if (swapPhase != SwapPhase.READY && swapPhase != SwapPhase.FINISHED) {
            processSwapInput(e);
        }
    }

    private void startSwap(Slot slotToSwap, boolean legit) {
        targetSlot = slotToSwap;
        if (targetSlot == null) return;

        isLegitMode = legit;
        swapPhase = SwapPhase.SLOWING_DOWN;
        actionStartTime = System.currentTimeMillis();
        playerFullyStopped = false;
    }
    
    private void processSwapInput(InputEvent e) {
        if (mc.player == null) return;
        
        switch (swapPhase) {
            case SLOWING_DOWN, WAITING_STOP -> {
                if (isLegitMode) {
                    e.inputNone();
                }
                
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    AutoSprint.tickStop = 10;
                }
            }
            case SWAP -> {
                if (isLegitMode) {
                    e.inputNone();
                }
            }
        }
    }

    private void processSwapTick() {
        if (mc.player == null || mc.currentScreen != null) {
            resetState();
            return;
        }

        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (swapPhase) {
            case SLOWING_DOWN -> {
                if (isLegitMode) {
                    if (elapsed > 50) {
                        swapPhase = SwapPhase.WAITING_STOP;
                        actionStartTime = System.currentTimeMillis();
                    }
                } else {
                    if (elapsed > 50) {
                        swapPhase = SwapPhase.SWAP;
                    }
                }
            }
            case WAITING_STOP -> {
                if (elapsed > 200) {
                    swapPhase = SwapPhase.SWAP;
                }
            }
            case SWAP -> {
                if (targetSlot != null) {
                    ItemStack swappedItem = targetSlot.getStack().copy();
                    InventoryTask.moveItem(targetSlot, 45, false, false);
                    
                    DynamicIsland dynamicIsland = DynamicIsland.getInstance();
                    if (dynamicIsland != null && !swappedItem.isEmpty()) {
                        String itemName = swappedItem.getName().getString();
                        dynamicIsland.showSwapNotification(swappedItem, itemName);
                    }
                }
                swapPhase = SwapPhase.SPEEDING_UP;
                actionStartTime = System.currentTimeMillis();
            }
            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - actionStartTime;
                
                if (speedupElapsed > 150) {
                    swapPhase = SwapPhase.FINISHED;
                }
            }
            case FINISHED -> resetState();
        }
    }

    private Slot findValidSlot(Predicate<Slot> slotPredicate) {
        Predicate<Slot> combinedPredicate = s -> s.id != 45 && slotPredicate.test(s);

        Item firstType = getItemByType(firstItem.getSelected());
        Item secondType = getItemByType(secondItem.getSelected());
        Item offHandItem = mc.player.getOffHandStack().getItem();
        String offHandItemName = mc.player.getOffHandStack().getName().getString();

        if (offHandItem == firstType) {
            Slot second = InventoryTask.getSlot(secondType,
                    Comparator.comparing(s -> s.getStack().hasEnchantments()),
                    combinedPredicate.and(s -> s.getStack().getItem() == secondType && !s.getStack().getName().getString().equals(offHandItemName))
            );
            if (second != null) return second;
        }

        if (offHandItem == secondType) {
            Slot first = InventoryTask.getSlot(firstType,
                    Comparator.comparing(s -> s.getStack().hasEnchantments()),
                    combinedPredicate.and(s -> s.getStack().getItem() == firstType && !s.getStack().getName().getString().equals(offHandItemName))
            );
            if (first != null) return first;
        }

        if (offHandItem != firstType && offHandItem != secondType) {
            Slot first = InventoryTask.getSlot(firstType,
                    Comparator.comparing(s -> s.getStack().hasEnchantments()),
                    combinedPredicate.and(s -> s.getStack().getItem() == firstType && !s.getStack().getName().getString().equals(offHandItemName))
            );
            if (first != null) return first;

            Slot second = InventoryTask.getSlot(secondType,
                    Comparator.comparing(s -> s.getStack().hasEnchantments()),
                    combinedPredicate.and(s -> s.getStack().getItem() == secondType && !s.getStack().getName().getString().equals(offHandItemName))
            );
            if (second != null) return second;
        }

        return null;
    }

    private void resetState() {
        swapPhase = SwapPhase.READY;
        targetSlot = null;
        playerFullyStopped = false;
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Голова" -> Items.PLAYER_HEAD;
            case "Гепл" -> Items.GOLDEN_APPLE;
            case "Щит" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }
}
