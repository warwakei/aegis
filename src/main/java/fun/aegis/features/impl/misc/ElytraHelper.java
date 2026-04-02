package fun.aegis.features.impl.misc;

import antidaunleak.api.annotation.Native;
import fun.aegis.display.hud.DynamicIsland;
import fun.aegis.events.keyboard.KeyEvent;
import fun.aegis.events.player.InputEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.features.impl.movement.AutoSprint;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryResult;
import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.utils.interactions.inv.InventoryToolkit;
import fun.aegis.utils.math.script.Script;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraHelper extends Module {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ замены").value("Default", "Legit").selected("Default");
    final BindSetting elytraSetting = new BindSetting("Замена элитр", "Меняет нагрудник на элитры");
    final BindSetting fireworkSetting = new BindSetting("Использовать фейерверк", "Меняет и использует фейерверки");
    final BooleanSetting startSetting = new BooleanSetting("Быстрый старт", "При замене на элитры автоматически взлетает и использует фейерверки").setValue(false);
    final BooleanSetting recast = new BooleanSetting("Авто взлет", "Автоматически начинает полет").setValue(false);
    final BooleanSetting autoFireworkSetting = new BooleanSetting("Авто фейерверк", "Автоматически использовать фейерверк каждые X мс").setValue(false);
    final SliderSettings fireworkDelay = new SliderSettings("Задержка использования в мс", "Задержка использования фейерверка в мс").setValue(500F).range(10F, 1500F).visible(() -> autoFireworkSetting.isValue());
    final SliderSettings stopDelay = new SliderSettings("Задержка остановки", "Время остановки перед свапом в мс").setValue(75F).range(25F, 200F).visible(() -> modeSetting.isSelected("Legit"));

    final Script script = new Script();

    enum ElytraPhase { READY, SLOWING_DOWN, WAITING_STOP, SWAP, SPEEDING_UP, FINISHED }
    ElytraPhase elytraPhase = ElytraPhase.READY;
    long actionStartTime = 0L;
    Slot targetSlot = null;
    boolean playerFullyStopped = false;
    boolean isLegitMode = false;

    enum FireworkPhase { READY, START, SWAP_TO_HOTBAR, WAITING_TO_USE, USE_FIREWORK, SWAP_BACK_TO_INV, FINISH }
    FireworkPhase fireworkPhase = FireworkPhase.READY;
    int fireworkSlot = -1;
    int savedHotbarSlot = -1;
    int originalHotbarSlot = -1;
    long useDelayStartTime = 0L;
    long lastAutoFireworkTime = 0L;

    public ElytraHelper() {
        super("ElytraHelper", "Elytra Helper", ModuleCategory.MISC);
        setup(modeSetting, elytraSetting, fireworkSetting, startSetting, recast, autoFireworkSetting, fireworkDelay, stopDelay);
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && recast.isValue()) {
            if (mc.player.isOnGround()) e.setJumping(true);
            else if (!mc.player.isGliding()) PlayerInteractionHelper.startFallFlying();
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (!script.isFinished()) return;

        if (e.isKeyDown(elytraSetting.getKey())) {
            if (modeSetting.getSelected().equals("Default")) {
                executeDefaultSwap();
            } else if (modeSetting.getSelected().equals("Legit") && elytraPhase == ElytraPhase.READY) {
                startLegitSwap();
            }
        } else if (e.isKeyDown(fireworkSetting.getKey()) && mc.player.isGliding() && fireworkPhase == FireworkPhase.READY) {
            fireworkPhase = FireworkPhase.START;
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        script.update();

        if (elytraPhase != ElytraPhase.READY) {
            processLegitSwap();
        }

        if (fireworkPhase != FireworkPhase.READY) {
            if (modeSetting.getSelected().equals("Default")) {
                processDefaultFireworkUsage();
            } else {
                processLegitFireworkUsage();
            }
        }

        if (autoFireworkSetting.isValue() && mc.player != null && mc.player.isGliding()) {
            long now = System.currentTimeMillis();
            if (now - lastAutoFireworkTime >= (long) fireworkDelay.getValue()) {
                if (fireworkPhase == FireworkPhase.READY && elytraPhase == ElytraPhase.READY) {
                    if (modeSetting.getSelected().equals("Default")) {
                        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.FIREWORK_ROCKET);
                        if (hotbar.found()) {
                            InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
                            lastAutoFireworkTime = now;
                        } else {
                            InventoryResult inv = InventoryToolkit.findItemInInventory(Items.FIREWORK_ROCKET);
                            if (inv.found()) {
                                int fireworkInvSlot = inv.slot();
                                int currentHotbarSlot = mc.player.getInventory().selectedSlot;
                                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
                                PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
                                lastAutoFireworkTime = now;
                            }
                        }
                    } else {
                        fireworkPhase = FireworkPhase.START;
                        lastAutoFireworkTime = now;
                    }
                }
            }
        }
    }

    private void processDefaultFireworkUsage() {
        if (mc.player == null) {
            resetFireworkState();
            return;
        }

        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.FIREWORK_ROCKET);
        if (hotbar.found()) {
            InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
        } else {
            InventoryResult inv = InventoryToolkit.findItemInInventory(Items.FIREWORK_ROCKET);
            if (inv.found()) {
                int fireworkInvSlot = inv.slot();
                int currentHotbarSlot = mc.player.getInventory().selectedSlot;
                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
                PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
            } else {
                ChatMessage.brandmessage("Нету фейерверков");
            }
        }
        resetFireworkState();
    }

    private void processLegitFireworkUsage() {
        if (mc.player == null || mc.currentScreen != null) {
            resetFireworkState();
            return;
        }

        switch (fireworkPhase) {
            case START -> {
                originalHotbarSlot = mc.player.getInventory().selectedSlot;
                InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.FIREWORK_ROCKET);
                if (hotbar.found()) {
                    savedHotbarSlot = hotbar.slot();
                    InventoryToolkit.switchTo(savedHotbarSlot);
                    useDelayStartTime = System.currentTimeMillis();
                    fireworkPhase = FireworkPhase.WAITING_TO_USE;
                } else {
                    InventoryResult inv = InventoryToolkit.findItemInInventory(Items.FIREWORK_ROCKET);
                    if (inv.found()) {
                        fireworkSlot = inv.slot();
                        savedHotbarSlot = originalHotbarSlot;
                        fireworkPhase = FireworkPhase.SWAP_TO_HOTBAR;
                    } else {
                        ChatMessage.brandmessage("Нету фейерверков");
                        resetFireworkState();
                    }
                }
            }
            case SWAP_TO_HOTBAR -> {
                InventoryToolkit.clickSlot(fireworkSlot, savedHotbarSlot, SlotActionType.SWAP);
                useDelayStartTime = System.currentTimeMillis();
                fireworkPhase = FireworkPhase.WAITING_TO_USE;
            }
            case WAITING_TO_USE -> {
                if (System.currentTimeMillis() - useDelayStartTime > 20) {
                    fireworkPhase = FireworkPhase.USE_FIREWORK;
                }
            }
            case USE_FIREWORK -> {
                if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
                    PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                }
                fireworkPhase = (fireworkSlot != -1) ? FireworkPhase.SWAP_BACK_TO_INV : FireworkPhase.FINISH;
            }
            case SWAP_BACK_TO_INV -> {
                InventoryToolkit.clickSlot(fireworkSlot, savedHotbarSlot, SlotActionType.SWAP);
                fireworkPhase = FireworkPhase.FINISH;
            }
            case FINISH -> resetFireworkState();
        }
    }

    private void resetFireworkState() {
        if (originalHotbarSlot != -1 && mc.player != null) {
            InventoryToolkit.switchTo(originalHotbarSlot);
        }
        fireworkPhase = FireworkPhase.READY;
        fireworkSlot = -1;
        savedHotbarSlot = -1;
        originalHotbarSlot = -1;
        useDelayStartTime = 0L;
    }

    private void executeDefaultSwap() {
        Slot slot = chestPlate();

        if (slot != null) {
            Slot fireWork = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
            boolean elytra = slot.getStack().getItem().equals(Items.ELYTRA);
            
            // Уведомление в DynamicIsland
            DynamicIsland dynamicIsland = DynamicIsland.getInstance();
            if (dynamicIsland != null) {
                ItemStack swappedItem = slot.getStack().copy();
                String itemName = elytra ? "Элитры" : "Броня";
                dynamicIsland.showElytraSwap(swappedItem, itemName);
            }
            
            InventoryTask.moveItem(slot, 6, false, true);

            if (startSetting.isValue() && fireWork != null && elytra) {
                script.cleanup().addTickStep(4, () -> {
                    if (mc.player.isOnGround()) mc.player.jump();
                }).addTickStep(3, () -> {
                    PlayerInteractionHelper.startFallFlying();
                    InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
                });
            }
        }
    }

    private void startLegitSwap() {
        targetSlot = chestPlate();
        if (targetSlot == null) return;

        elytraPhase = ElytraPhase.SLOWING_DOWN;
        actionStartTime = System.currentTimeMillis();
        playerFullyStopped = false;
    }

    private void processLegitSwap() {
        if (mc.player == null || mc.currentScreen != null) {
            resetLegitState();
            return;
        }

        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (elytraPhase) {
            case SLOWING_DOWN -> {
                // Полностью останавливаем игрока
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    AutoSprint.tickStop = 1;
                }
                if (elapsed > 1) {
                    elytraPhase = ElytraPhase.WAITING_STOP;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case WAITING_STOP -> {
                // Ждём полной остановки на заданное время
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                double velocityX = Math.abs(mc.player.getVelocity().x);
                double velocityZ = Math.abs(mc.player.getVelocity().z);
                // Ждём пока скорость станет минимальной или пройдёт заданное время
                if ((velocityX < 0.001 && velocityZ < 0.001) || elapsed > (long) stopDelay.getValue()) {
                    playerFullyStopped = true;
                    elytraPhase = ElytraPhase.SWAP;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case SWAP -> {
                // Продолжаем блокировать движение во время свапа
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                if (targetSlot != null) {
                    boolean elytra = targetSlot.getStack().getItem().equals(Items.ELYTRA);
                    
                    // Уведомление в DynamicIsland
                    DynamicIsland dynamicIsland = DynamicIsland.getInstance();
                    if (dynamicIsland != null) {
                        ItemStack swappedItem = targetSlot.getStack().copy();
                        String itemName = elytra ? "Элитры" : "Броня";
                        dynamicIsland.showElytraSwap(swappedItem, itemName);
                    }
                    
                    InventoryTask.moveItem(targetSlot, 6, false, false);

                    if (startSetting.isValue() && elytra) {
                        Slot fireWork = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
                        if (fireWork != null) {
                            script.cleanup().addTickStep(2, () -> {
                                if (mc.player.isOnGround()) mc.player.jump();
                            }).addTickStep(1, () -> {
                                PlayerInteractionHelper.startFallFlying();
                                InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
                            });
                        }
                    }
                }
                elytraPhase = ElytraPhase.SPEEDING_UP;
                actionStartTime = System.currentTimeMillis();
            }
            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - actionStartTime;
                // Плавно возвращаем управление через 75ms
                if (speedupElapsed > 75) {
                    elytraPhase = ElytraPhase.FINISHED;
                }
            }
            case FINISHED -> resetLegitState();
            default -> {}
        }
    }

    private void resetLegitState() {
        elytraPhase = ElytraPhase.READY;
        targetSlot = null;
        playerFullyStopped = false;
    }

    private Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
            return InventoryTask.getSlot(List.of(
                    Items.NETHERITE_CHESTPLATE,
                    Items.DIAMOND_CHESTPLATE,
                    Items.IRON_CHESTPLATE,
                    Items.GOLDEN_CHESTPLATE,
                    Items.CHAINMAIL_CHESTPLATE,
                    Items.LEATHER_CHESTPLATE
            ));
        } else {
            return InventoryTask.getSlot(Items.ELYTRA);
        }
    }
}
