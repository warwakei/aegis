package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import rich.events.api.EventHandler;
import rich.events.impl.HotBarScrollEvent;
import rich.events.impl.InputEvent;
import rich.events.impl.KeyEvent;
import rich.events.impl.TickEvent;
import rich.mixin.ClientWorldAccessor;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.screens.clickgui.impl.settingsrender.BindComponent;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.MovementController;
import rich.util.inventory.SwapSettings;
import rich.util.string.chat.ChatMessage;
import rich.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraHelper extends ModuleStructure {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свапа")
            .value("Instant", "Legit")
            .selected("Legit");

    final BindSetting swapBind = new BindSetting("Кнопка свапа", "Кнопка для смены элитры/нагрудника");
    final BindSetting fireworkBind = new BindSetting("Кнопка фейерверка", "Кнопка для использования фейерверка");
    final BooleanSetting autoTakeoff = new BooleanSetting("Авто взлёт", "Автоматический взлёт при надетой элитре").setValue(false);
    final BooleanSetting autoFirework = new BooleanSetting("Авто фейерверк", "Автоматическое использование фейерверка при полёте")
            .setValue(false)
            .visible(() -> autoTakeoff.isValue());

    enum SwapPhase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_ARMOR, POST_SWAP, RESUMING
    }

    enum FireworkPhase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_TO_HAND, AWAIT_ITEM, USE, POST_USE, SWAP_BACK, RESUMING
    }

    final MovementController swapMovement = new MovementController();
    final MovementController fireworkMovement = new MovementController();

    final StopWatch fireworkCooldown = new StopWatch();
    final StopWatch autoFireworkTimer = new StopWatch();

    SwapPhase swapPhase = SwapPhase.IDLE;
    FireworkPhase fireworkPhase = FireworkPhase.IDLE;

    int armorSlot = -1;
    int fireworkSlot = -1;
    int savedFireworkSlot = -1;
    boolean fireworkFromInventory = false;

    long swapPhaseStartTime = 0;
    long fireworkPhaseStartTime = 0;

    int swapCurrentDelay = 0;
    int fireworkCurrentDelay = 0;

    boolean shouldJumpForTakeoff = false;

    public ElytraHelper() {
        super("ElytraHelper", "Elytra Helper", ModuleCategory.MISC);
        settings(modeSetting, swapBind, fireworkBind);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (e.action() != 1) return;

        if (matchesBind(e, swapBind) && swapPhase == SwapPhase.IDLE) {
            startArmorSwap();
        }

        if (matchesBind(e, fireworkBind) && fireworkPhase == FireworkPhase.IDLE) {
            if (!isElytraEquipped()) {
                return;
            }
            if (fireworkCooldown.finished(500)) {
                useFirework();
                fireworkCooldown.reset();
            }
        }
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (matchesScrollBind(e, swapBind) && swapPhase == SwapPhase.IDLE) {
            e.setCancelled(true);
            startArmorSwap();
        }

        if (matchesScrollBind(e, fireworkBind) && fireworkPhase == FireworkPhase.IDLE) {
            if (!isElytraEquipped()) {
                return;
            }
            if (fireworkCooldown.finished(500)) {
                e.setCancelled(true);
                useFirework();
                fireworkCooldown.reset();
            }
        }
    }

    private boolean matchesBind(KeyEvent e, BindSetting bind) {
        int bindKey = bind.getKey();
        int bindType = bind.getType();

        if (bindKey == GLFW.GLFW_KEY_UNKNOWN || bindKey == -1) return false;

        if (bindType == 2) {
            if (bindKey == BindComponent.MIDDLE_MOUSE_BIND
                    && e.type() == InputUtil.Type.MOUSE
                    && e.key() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                return true;
            }
        } else if (bindType == 0 && e.type() == InputUtil.Type.MOUSE) {
            return e.key() == bindKey;
        } else if (bindType == 1 && e.type() == InputUtil.Type.KEYSYM) {
            return e.key() == bindKey;
        }
        return false;
    }

    private boolean matchesScrollBind(HotBarScrollEvent e, BindSetting bind) {
        int bindKey = bind.getKey();
        int bindType = bind.getType();

        if (bindType != 2) return false;

        if (bindKey == BindComponent.SCROLL_UP_BIND && e.getVertical() > 0) {
            return true;
        } else if (bindKey == BindComponent.SCROLL_DOWN_BIND && e.getVertical() < 0) {
            return true;
        }
        return false;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            resetAllStates();
            return;
        }

        processArmorSwapLoop();
        processFireworkUseLoop();
        processAutoTakeoff();
        processAutoFirework();
    }

    private void processArmorSwapLoop() {
        if (swapPhase == SwapPhase.IDLE) return;

        boolean continueProcessing = true;
        int iterations = 0;

        while (continueProcessing && iterations < 10) {
            iterations++;
            continueProcessing = processArmorSwapTick();
        }
    }

    private void processFireworkUseLoop() {
        if (fireworkPhase == FireworkPhase.IDLE) return;

        boolean continueProcessing = true;
        int iterations = 0;

        while (continueProcessing && iterations < 10) {
            iterations++;
            continueProcessing = processFireworkUseTick();
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;

        if (swapMovement.isBlocked()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }

        if (fireworkMovement.isBlocked()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }

        if (shouldJumpForTakeoff) {
            e.setJumping(true);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isElytraEquipped() {
        if (mc.player == null) return false;
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return chestStack.getItem() == Items.ELYTRA;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isElytraUsable() {
        if (mc.player == null) return false;
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestStack.getItem() != Items.ELYTRA) return false;
        if (chestStack.getMaxDamage() <= 0) return true;
        return chestStack.getDamage() < chestStack.getMaxDamage() - 1;
    }

    private boolean isInstantMode() {
        return modeSetting.getSelected().equals("Instant");
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processAutoTakeoff() {
        if (!autoTakeoff.isValue()) {
            shouldJumpForTakeoff = false;
            return;
        }
        if (swapPhase != SwapPhase.IDLE) {
            shouldJumpForTakeoff = false;
            return;
        }
        if (fireworkPhase != FireworkPhase.IDLE) {
            shouldJumpForTakeoff = false;
            return;
        }
        if (!isElytraEquipped()) {
            shouldJumpForTakeoff = false;
            return;
        }

        if (mc.player.isOnGround()) {
            shouldJumpForTakeoff = true;
        } else {
            shouldJumpForTakeoff = false;
            if (isElytraUsable() && !mc.player.isGliding() && !mc.player.getAbilities().flying) {
                mc.getNetworkHandler().sendPacket(
                        new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                );
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processAutoFirework() {
        if (!autoTakeoff.isValue() || !autoFirework.isValue()) return;
        if (mc.player == null) return;
        if (!isElytraEquipped()) return;
        if (!mc.player.isGliding()) return;
        if (fireworkPhase != FireworkPhase.IDLE) return;

        if (mc.player.isUsingItem()) {
            return;
        }

        if (autoFireworkTimer.finished(1000)) {
            useFirework();
            autoFireworkTimer.reset();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void useFirework() {
        if (!isElytraEquipped()) return;

        if (isInstantMode()) {
            useFireworkInstant();
        } else {
            startFireworkUseLegit();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void useFireworkInstant() {
        int hotbarSlot = findItemInHotbar(Items.FIREWORK_ROCKET);
        if (hotbarSlot != -1) {
            int currentSlot = mc.player.getInventory().getSelectedSlot();

            if (hotbarSlot != currentSlot) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            }

            sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    sequence,
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));

            if (hotbarSlot != currentSlot) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
            }
            return;
        }

        int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
        if (invSlot != -1) {
            int currentHotbarSlot = mc.player.getInventory().getSelectedSlot();
            int wrappedSlot = InventoryUtils.wrapSlot(invSlot);

            InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);

            sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    sequence,
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));

            InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);
            InventoryUtils.closeScreen();
        } else {
            ChatMessage.brandmessage("Нету фейерверков");
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void startFireworkUseLegit() {
        savedFireworkSlot = mc.player.getInventory().getSelectedSlot();

        int hotbarSlot = findItemInHotbar(Items.FIREWORK_ROCKET);
        if (hotbarSlot != -1) {
            fireworkSlot = hotbarSlot;
            fireworkFromInventory = false;
            InventoryUtils.selectSlot(fireworkSlot);
            startFireworkPhase(FireworkPhase.AWAIT_ITEM, 0);
            return;
        }

        int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
        if (invSlot != -1) {
            fireworkSlot = invSlot;
            fireworkFromInventory = true;

            SwapSettings settings = buildSettings();
            if (settings.shouldStopMovement()) {
                startFireworkPhase(FireworkPhase.PRE_STOP, settings.randomPreStopDelay());
            } else {
                startFireworkPhase(FireworkPhase.SWAP_TO_HAND, 0);
            }
        } else {
            ChatMessage.brandmessage("Нету фейерверков");
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void startArmorSwap() {
        boolean isElytra = isElytraEquipped();
        Item targetItem = isElytra ? null : Items.ELYTRA;

        int slot = findChestArmorSlot(targetItem, isElytra);
        if (slot == -1) {
            String missing = isElytra ? "нагрудника" : "элитры";
            ChatMessage.brandmessage("Нету " + missing);
            return;
        }

        armorSlot = slot;
        String itemName = isElytra ? "Нагрудник" : "Элитру";
        ChatMessage.brandmessage("Свапнул на " + itemName);

        SwapSettings settings = buildSettings();
        if (settings.shouldStopMovement()) {
            startSwapPhase(SwapPhase.PRE_STOP, settings.randomPreStopDelay());
        } else {
            startSwapPhase(SwapPhase.SWAP_ARMOR, 0);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private int findChestArmorSlot(Item targetItem, boolean isElytraEquipped) {
        for (int i = 0; i < 46; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
            if (component == null || component.slot() != EquipmentSlot.CHEST) continue;

            if (targetItem == null) {
                if (stack.getItem() != Items.ELYTRA) {
                    return i;
                }
            } else {
                if (stack.getItem() == targetItem) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean processArmorSwapTick() {
        if (mc.player == null) {
            resetSwapState();
            return false;
        }

        long elapsed = System.currentTimeMillis() - swapPhaseStartTime;
        SwapSettings settings = buildSettings();

        switch (swapPhase) {
            case PRE_STOP -> {
                if (elapsed >= swapCurrentDelay) {
                    swapMovement.saveState();
                    swapMovement.block();
                    if (settings.shouldStopSprint()) {
                        swapMovement.stopSprint();
                    }
                    startSwapPhase(SwapPhase.STOPPING, 0);
                    return true;
                }
            }
            case STOPPING -> {
                swapMovement.block();
                if (settings.shouldStopSprint()) {
                    swapMovement.stopSprint();
                }
                startSwapPhase(SwapPhase.WAIT_STOP, settings.randomWaitStopDelay());
                return swapCurrentDelay == 0;
            }
            case WAIT_STOP -> {
                swapMovement.block();
                boolean stopped = swapMovement.isPlayerStopped(settings.getVelocityThreshold());
                boolean timeout = elapsed >= swapCurrentDelay;

                if (stopped || timeout) {
                    startSwapPhase(SwapPhase.PRE_SWAP, settings.randomPreSwapDelay());
                    return swapCurrentDelay == 0;
                }
            }
            case PRE_SWAP -> {
                swapMovement.block();
                if (elapsed >= swapCurrentDelay) {
                    startSwapPhase(SwapPhase.SWAP_ARMOR, 0);
                    return true;
                }
            }
            case SWAP_ARMOR -> {
                int fromSlot = InventoryUtils.wrapSlot(armorSlot);
                InventoryUtils.swap(fromSlot, 6);
                startSwapPhase(SwapPhase.POST_SWAP, settings.randomPostSwapDelay());
                return swapCurrentDelay == 0;
            }
            case POST_SWAP -> {
                if (elapsed >= swapCurrentDelay) {
                    if (settings.shouldCloseInventory()) {
                        InventoryUtils.closeScreen();
                    }
                    startSwapPhase(SwapPhase.RESUMING, settings.randomResumeDelay());
                    return swapCurrentDelay == 0;
                }
            }
            case RESUMING -> {
                if (elapsed >= swapCurrentDelay) {
                    if (buildSettings().shouldStopMovement()) {
                        swapMovement.restoreFromCurrent();
                    }
                    resetSwapState();
                    return false;
                }
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean processFireworkUseTick() {
        if (mc.player == null) {
            resetFireworkState();
            return false;
        }

        long elapsed = System.currentTimeMillis() - fireworkPhaseStartTime;
        SwapSettings settings = buildSettings();

        switch (fireworkPhase) {
            case PRE_STOP -> {
                if (elapsed >= fireworkCurrentDelay) {
                    fireworkMovement.saveState();
                    fireworkMovement.block();
                    if (settings.shouldStopSprint()) {
                        fireworkMovement.stopSprint();
                    }
                    startFireworkPhase(FireworkPhase.STOPPING, 0);
                    return true;
                }
            }
            case STOPPING -> {
                fireworkMovement.block();
                if (settings.shouldStopSprint()) {
                    fireworkMovement.stopSprint();
                }
                startFireworkPhase(FireworkPhase.WAIT_STOP, settings.randomWaitStopDelay());
                return fireworkCurrentDelay == 0;
            }
            case WAIT_STOP -> {
                fireworkMovement.block();
                boolean stopped = fireworkMovement.isPlayerStopped(settings.getVelocityThreshold());
                boolean timeout = elapsed >= fireworkCurrentDelay;

                if (stopped || timeout) {
                    startFireworkPhase(FireworkPhase.PRE_SWAP, settings.randomPreSwapDelay());
                    return fireworkCurrentDelay == 0;
                }
            }
            case PRE_SWAP -> {
                fireworkMovement.block();
                if (elapsed >= fireworkCurrentDelay) {
                    startFireworkPhase(FireworkPhase.SWAP_TO_HAND, 0);
                    return true;
                }
            }
            case SWAP_TO_HAND -> {
                int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InventoryUtils.click(fireworkSlot, hotbarSlot, SlotActionType.SWAP);
                startFireworkPhase(FireworkPhase.AWAIT_ITEM, 0);
                return true;
            }
            case AWAIT_ITEM -> {
                if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
                    startFireworkPhase(FireworkPhase.USE, 0);
                    return true;
                }
            }
            case USE -> {
                sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                        Hand.MAIN_HAND,
                        sequence,
                        mc.player.getYaw(),
                        mc.player.getPitch()
                ));
                mc.player.swingHand(Hand.MAIN_HAND);
                startFireworkPhase(FireworkPhase.POST_USE, settings.randomPostSwapDelay());
                return fireworkCurrentDelay == 0;
            }
            case POST_USE -> {
                if (elapsed >= fireworkCurrentDelay) {
                    if (fireworkFromInventory) {
                        startFireworkPhase(FireworkPhase.SWAP_BACK, 0);
                        return true;
                    } else {
                        InventoryUtils.selectSlot(savedFireworkSlot);
                        startFireworkPhase(FireworkPhase.RESUMING, settings.randomResumeDelay());
                        return fireworkCurrentDelay == 0;
                    }
                }
            }
            case SWAP_BACK -> {
                int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InventoryUtils.click(fireworkSlot, hotbarSlot, SlotActionType.SWAP);
                InventoryUtils.selectSlot(savedFireworkSlot);
                if (settings.shouldCloseInventory()) {
                    InventoryUtils.closeScreen();
                }
                startFireworkPhase(FireworkPhase.RESUMING, settings.randomResumeDelay());
                return fireworkCurrentDelay == 0;
            }
            case RESUMING -> {
                if (elapsed >= fireworkCurrentDelay) {
                    if (buildSettings().shouldStopMovement()) {
                        fireworkMovement.restoreFromCurrent();
                    }
                    resetFireworkState();
                    return false;
                }
            }
        }
        return false;
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private void sendSequencedPacket(java.util.function.IntFunction<net.minecraft.network.packet.Packet<?>> packetCreator) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.world == null) return;

        try {
            ClientWorldAccessor worldAccessor = (ClientWorldAccessor) mc.world;
            PendingUpdateManager pendingUpdateManager = worldAccessor.getPendingUpdateManager().incrementSequence();

            int sequence = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.apply(sequence));

            pendingUpdateManager.close();
        } catch (Exception e) {
            mc.getNetworkHandler().sendPacket(packetCreator.apply(0));
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private SwapSettings buildSettings() {
        String mode = modeSetting.getSelected();
        return switch (mode) {
            case "Instant" -> SwapSettings.instant();
            default -> SwapSettings.legit();
        };
    }

    private void startSwapPhase(SwapPhase phase, int delay) {
        this.swapPhase = phase;
        this.swapPhaseStartTime = System.currentTimeMillis();
        this.swapCurrentDelay = delay;
    }

    private void startFireworkPhase(FireworkPhase phase, int delay) {
        this.fireworkPhase = phase;
        this.fireworkPhaseStartTime = System.currentTimeMillis();
        this.fireworkCurrentDelay = delay;
    }

    private void resetSwapState() {
        swapMovement.reset();
        swapPhase = SwapPhase.IDLE;
        armorSlot = -1;
        swapPhaseStartTime = 0;
        swapCurrentDelay = 0;
    }

    private void resetFireworkState() {
        fireworkMovement.reset();
        fireworkPhase = FireworkPhase.IDLE;
        fireworkSlot = -1;
        savedFireworkSlot = -1;
        fireworkFromInventory = false;
        fireworkPhaseStartTime = 0;
        fireworkCurrentDelay = 0;
    }

    private void resetAllStates() {
        resetSwapState();
        resetFireworkState();
        shouldJumpForTakeoff = false;
    }

    public boolean isSwapping() {
        return swapPhase != SwapPhase.IDLE;
    }

    public boolean isUsingFirework() {
        return fireworkPhase != FireworkPhase.IDLE;
    }

    @Override
    public void deactivate() {
        if (swapMovement.isBlocked()) {
            swapMovement.restoreFromCurrent();
        }
        if (fireworkMovement.isBlocked()) {
            fireworkMovement.restoreFromCurrent();
        }
        resetAllStates();
    }
}