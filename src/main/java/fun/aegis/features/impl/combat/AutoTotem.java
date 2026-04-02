package fun.aegis.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.inv.InventoryTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoTotem extends Module {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свопа")
            .value("Default", "Legit")
            .selected("Default");

    private final SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для экипировки тотема")
            .setValue(4.5F).range(1F, 20F);

    private final SliderSettings elytraHealth = new SliderSettings("Здоровье на элитре", "Минимальное здоровье при полете")
            .setValue(8.5F).range(1F, 20F);

    private final SliderSettings crystalDistance = new SliderSettings("Дистанция до кристалла", "Макс дистанция до кристалла")
            .setValue(4F).range(1F, 6F);

    private final BooleanSetting fallCheck = new BooleanSetting("Падение", "Экипировать тотем при падении")
            .setValue(true);

    private final BooleanSetting saveTaliks = new BooleanSetting("Сейв таликов", "Использовать обычные тотемы без чар")
            .setValue(true);

    private final BooleanSetting returnItem = new BooleanSetting("Возвращать предмет", "Вернуть предыдущий предмет в руку/хотбар после использования тотема")
            .setValue(true);

    private int savedSlot = -1;
    private int totemSlot = -1;
    private long actionStartTime = 0L;
    private boolean keysOverridden = false;
    private boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed, wasJumpPressed;
    private boolean playerFullyStopped = false;
    private Phase phase = Phase.READY;
    private ItemStack previousMainHandStack = ItemStack.EMPTY;
    private int previousMainHandSlot = -1;
    private boolean needsReturn = false;

    private enum Phase { READY, SLOWING_DOWN, SWAP_TOTEM, AWAIT_SWITCH, RESTORE_SLOT, SPEEDING_UP, FINISH }

    public AutoTotem() {
        super("AutoTotem", "Auto Totem", ModuleCategory.COMBAT);
        setup(modeSetting, healthThreshold, elytraHealth, crystalDistance, fallCheck, saveTaliks, returnItem);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (MC.player == null || MC.world == null) {
            resetState();
            return;
        }

        if (phase != Phase.READY) {
            execute();
            return;
        }

        float health = MC.player.getHealth();
        boolean shouldEquip = false;

        if (MC.player.isGliding() && health <= elytraHealth.getValue()) {
            shouldEquip = true;
        } else if (health <= healthThreshold.getValue()) {
            shouldEquip = true;
        } else if (fallCheck.isValue() && MC.player.fallDistance > 10) {
            shouldEquip = true;
        } else if (getClosestCrystalDistance() <= crystalDistance.getValue()) {
            shouldEquip = true;
        }

        if (shouldEquip) {
            tryEquipTotem();
        }

        if (phase == Phase.READY && needsReturn && returnItem.isValue() && previousMainHandSlot >= 0 && !previousMainHandStack.isEmpty()) {
            if (MC.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                attemptReturnPreviousItem();
                previousMainHandStack = ItemStack.EMPTY;
                previousMainHandSlot = -1;
                needsReturn = false;
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void tryEquipTotem() {
        if (phase != Phase.READY) return;
        if (isTotemInOffhand()) return;
        if (MC.currentScreen != null) return;

        savedSlot = MC.player.getInventory().selectedSlot;
        ItemStack totemInHotbar = findTotemInHotbar();
        if (!totemInHotbar.isEmpty()) {
            totemSlot = MC.player.getInventory().selectedSlot;
            if (modeSetting.getSelected().equals("Default")) {
                executeDefaultSwap();
            } else {
                startLegitEquip();
            }
            return;
        }

        ItemStack totemInInv = findTotemInInventory();
        if (!totemInInv.isEmpty()) {
            totemSlot = findTotemSlot();
            previousMainHandSlot = MC.player.getInventory().selectedSlot;
            previousMainHandStack = MC.player.getMainHandStack().copy();
            needsReturn = true;
            if (modeSetting.getSelected().equals("Legit")) {
                startLegitEquip();
            } else {
                executeDefaultSwap();
            }
        }
    }

    private void executeDefaultSwap() {
        if (totemSlot < 0) {
            resetState();
            return;
        }

        int slotIndex = totemSlot;
        if (slotIndex >= 0 && slotIndex <= 8) slotIndex += 36;

        if (MC.interactionManager != null && MC.player.playerScreenHandler != null) {
            MC.interactionManager.clickSlot(
                    MC.player.playerScreenHandler.syncId,
                    slotIndex,
                    40,
                    SlotActionType.SWAP,
                    MC.player
            );
        }

        if (returnItem.isValue() && needsReturn && previousMainHandSlot >= 0) {
            MC.player.getInventory().selectedSlot = previousMainHandSlot;
        } else if (savedSlot >= 0) {
            MC.player.getInventory().selectedSlot = savedSlot;
        }

        resetState();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void startLegitEquip() {
        wasForwardPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.forwardKey.getDefaultKey().getCode());
        wasBackPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.backKey.getDefaultKey().getCode());
        wasLeftPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.leftKey.getDefaultKey().getCode());
        wasRightPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.rightKey.getDefaultKey().getCode());
        wasJumpPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.jumpKey.getDefaultKey().getCode());

        phase = Phase.SLOWING_DOWN;
        actionStartTime = System.currentTimeMillis();
        playerFullyStopped = false;
        keysOverridden = false;
    }

    private void execute() {
        if (MC.player == null || MC.currentScreen != null) {
            resetState();
            return;
        }

        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (phase) {
            case SLOWING_DOWN -> {
                MC.player.input.movementForward = 0;
                MC.player.input.movementSideways = 0;
                if (MC.player.isSprinting()) {
                    MC.player.setSprinting(false);
                }
                if (!keysOverridden) {
                    MC.options.forwardKey.setPressed(false);
                    MC.options.backKey.setPressed(false);
                    MC.options.leftKey.setPressed(false);
                    MC.options.rightKey.setPressed(false);
                    MC.options.jumpKey.setPressed(false);
                    keysOverridden = true;
                }
                if (elapsed > 1) {
                    phase = Phase.SWAP_TOTEM;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case SWAP_TOTEM -> {
                if (elapsed > 25) {
                    if (totemSlot < 0) {
                        resetState();
                        return;
                    }

                    int slotIndex = totemSlot;
                    if (slotIndex >= 0 && slotIndex <= 8) slotIndex += 36;

                    if (MC.interactionManager != null && MC.player.playerScreenHandler != null) {
                        MC.interactionManager.clickSlot(
                                MC.player.playerScreenHandler.syncId,
                                slotIndex,
                                40,
                                SlotActionType.SWAP,
                                MC.player
                        );
                    }

                    phase = Phase.AWAIT_SWITCH;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case AWAIT_SWITCH -> {
                if (isTotemInOffhand() || elapsed > 50) {
                    phase = Phase.RESTORE_SLOT;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case RESTORE_SLOT -> {
                if (elapsed > 25) {
                    InventoryTask.switchTo(savedSlot);
                    if (modeSetting.getSelected().equals("Legit")) {
                        if (keysOverridden) {
                            restoreKeyStates();
                        }
                        actionStartTime = System.currentTimeMillis();
                        phase = Phase.SPEEDING_UP;
                    } else {
                        phase = Phase.FINISH;
                    }
                }
            }
            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - actionStartTime;
                float speedupProgress = Math.min(1.0f, speedupElapsed / 20.0f);
                if (MC.player.input != null) {
                    boolean forward = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.forwardKey.getDefaultKey().getCode());
                    float targetForward = forward ? 1.0f : 0;
                    MC.player.input.movementForward = lerp(MC.player.input.movementForward, targetForward * speedupProgress, 0.4f);
                    if (speedupProgress > 0.4f && forward && !MC.player.isSprinting()) {
                        MC.player.setSprinting(true);
                    }
                }
                if (speedupElapsed > 25) {
                    phase = Phase.FINISH;
                }
            }
            case FINISH -> resetState();
        }
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private ItemStack findTotemInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findTotemInInventory() {
        if (saveTaliks.isValue()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = MC.player.getInventory().getStack(i);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING && !stack.hasEnchantments()) {
                    return stack;
                }
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private int findTotemSlot() {
        if (saveTaliks.isValue()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = MC.player.getInventory().getStack(i);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING && !stack.hasEnchantments()) {
                    return i;
                }
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }

    private double getClosestCrystalDistance() {
        double minDist = Double.MAX_VALUE;
        if (MC.player == null || MC.world == null) return minDist;
        Box box = MC.player.getBoundingBox().expand(crystalDistance.getValue());
        List<EndCrystalEntity> crystals = MC.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true);
        for (EndCrystalEntity crystal : crystals) {
            double dist = MC.player.getPos().distanceTo(crystal.getPos());
            if (dist < minDist) minDist = dist;
        }
        return minDist;
    }

    private boolean isTotemInOffhand() {
        ItemStack stack = MC.player.getOffHandStack();
        return stack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private void restoreKeyStates() {
        boolean currentForward = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.forwardKey.getDefaultKey().getCode());
        boolean currentBack = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.backKey.getDefaultKey().getCode());
        boolean currentLeft = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.leftKey.getDefaultKey().getCode());
        boolean currentRight = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.rightKey.getDefaultKey().getCode());
        boolean currentJump = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.jumpKey.getDefaultKey().getCode());

        MC.options.forwardKey.setPressed(wasForwardPressed && currentForward);
        MC.options.backKey.setPressed(wasBackPressed && currentBack);
        MC.options.leftKey.setPressed(wasLeftPressed && currentLeft);
        MC.options.rightKey.setPressed(wasRightPressed && currentRight);
        MC.options.jumpKey.setPressed(wasJumpPressed && currentJump);
        keysOverridden = false;
    }

    private void attemptReturnPreviousItem() {
        if (!returnItem.isValue()) return;
        if (previousMainHandStack == null || previousMainHandStack.isEmpty()) return;
        if (MC.player == null || MC.player.playerScreenHandler == null || MC.interactionManager == null) return;
        int targetHotbar = previousMainHandSlot;
        if (targetHotbar < 0 || targetHotbar > 8) targetHotbar = savedSlot >= 0 ? savedSlot : -1;
        if (targetHotbar < 0) return;
        ItemStack currentStackInTarget = MC.player.getInventory().getStack(targetHotbar);
        if (!currentStackInTarget.isEmpty() && currentStackInTarget.getItem() == previousMainHandStack.getItem()) {
            InventoryTask.switchTo(targetHotbar);
            return;
        }
        int foundSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() == previousMainHandStack.getItem()) {
                foundSlot = i;
                break;
            }
        }
        if (foundSlot < 0) {
            InventoryTask.switchTo(targetHotbar);
            return;
        }
        int fromIndex = foundSlot;
        if (fromIndex >= 0 && fromIndex <= 8) fromIndex += 36;
        int toIndex = targetHotbar;
        if (toIndex >= 0 && toIndex <= 8) toIndex += 36;
        MC.interactionManager.clickSlot(MC.player.playerScreenHandler.syncId, fromIndex, toIndex, SlotActionType.SWAP, MC.player);
        InventoryTask.switchTo(targetHotbar);
    }

    private void resetState() {
        if (keysOverridden) restoreKeyStates();
        totemSlot = -1;
        savedSlot = -1;
        actionStartTime = 0L;
        phase = Phase.READY;
        playerFullyStopped = false;
    }

    @Override
    public void deactivate() {
        resetState();
        previousMainHandStack = ItemStack.EMPTY;
        previousMainHandSlot = -1;
        needsReturn = false;
        super.deactivate();
    }
}
