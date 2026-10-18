package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
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
import rich.modules.module.setting.implement.SelectSetting;
import rich.screens.clickgui.impl.settingsrender.BindComponent;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.MovementController;
import rich.util.inventory.SwapSettings;
import rich.util.string.chat.ChatMessage;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClickPearl extends ModuleStructure {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ броска")
            .value("Instant", "Legit")
            .selected("Legit");

    final BindSetting keySetting = new BindSetting("Кнопка", "Кнопка для броска");

    enum Phase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_TO_HAND, AWAIT_ITEM, THROW, POST_THROW, SWAP_BACK, RESUMING
    }

    final MovementController movement = new MovementController();

    Phase phase = Phase.IDLE;
    int savedSlot = -1;
    int pearlSlot = -1;
    boolean fromInventory = false;
    long phaseStartTime = 0;
    int currentDelay = 0;
    long lastThrowTime = 0;

    public ClickPearl() {
        super("ClickPearl", "Click Pearl", ModuleCategory.MISC);
        settings(modeSetting, keySetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (phase != Phase.IDLE) return;
        if (e.action() != 1) return;

        int bindKey = keySetting.getKey();
        int bindType = keySetting.getType();

        if (bindKey == GLFW.GLFW_KEY_UNKNOWN || bindKey == -1) return;

        boolean matches = false;

        if (bindType == 2) {
            if (bindKey == BindComponent.MIDDLE_MOUSE_BIND
                    && e.type() == InputUtil.Type.MOUSE
                    && e.key() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                matches = true;
            }
        } else if (bindType == 0 && e.type() == InputUtil.Type.MOUSE) {
            matches = e.key() == bindKey;
        } else if (bindType == 1 && e.type() == InputUtil.Type.KEYSYM) {
            matches = e.key() == bindKey;
        }

        if (matches) {
            tryThrowPearl();
        }
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (phase != Phase.IDLE) return;

        int bindKey = keySetting.getKey();
        int bindType = keySetting.getType();

        if (bindType != 2) return;

        boolean matches = false;
        if (bindKey == BindComponent.SCROLL_UP_BIND && e.getVertical() > 0) {
            matches = true;
        } else if (bindKey == BindComponent.SCROLL_DOWN_BIND && e.getVertical() < 0) {
            matches = true;
        }

        if (matches) {
            e.setCancelled(true);
            tryThrowPearl();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void tryThrowPearl() {
        if (System.currentTimeMillis() - lastThrowTime < 100) return;
        lastThrowTime = System.currentTimeMillis();
        startPearlProcess();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }

        if (phase != Phase.IDLE) {
            boolean continueProcessing = true;
            int iterations = 0;

            while (continueProcessing && iterations < 10) {
                iterations++;
                continueProcessing = processTick();
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;
        if (movement.isBlocked()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void startPearlProcess() {
        savedSlot = mc.player.getInventory().getSelectedSlot();

        int hotbarSlot = InventoryUtils.findItemInHotbar(Items.ENDER_PEARL);
        if (hotbarSlot != -1) {
            pearlSlot = hotbarSlot;
            fromInventory = false;
            InventoryUtils.selectSlot(pearlSlot);
            startPhase(Phase.AWAIT_ITEM, 0);
            return;
        }

        int invSlot = InventoryUtils.findItemInInventory(Items.ENDER_PEARL);
        if (invSlot != -1) {
            pearlSlot = invSlot;
            fromInventory = true;

            SwapSettings settings = buildSettings();
            if (settings.shouldStopMovement()) {
                startPhase(Phase.PRE_STOP, settings.randomPreStopDelay());
            } else {
                startPhase(Phase.SWAP_TO_HAND, 0);
            }
        } else {
            ChatMessage.brandmessage("Нету жемчуга");
            resetState();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean processTick() {
        if (mc.player == null || mc.currentScreen != null) {
            resetState();
            return false;
        }

        long elapsed = System.currentTimeMillis() - phaseStartTime;
        SwapSettings settings = buildSettings();

        switch (phase) {
            case PRE_STOP -> {
                if (elapsed >= currentDelay) {
                    movement.saveState();
                    movement.block();
                    if (settings.shouldStopSprint()) {
                        movement.stopSprint();
                    }
                    startPhase(Phase.STOPPING, 0);
                    return true;
                }
            }
            case STOPPING -> {
                movement.block();
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                startPhase(Phase.WAIT_STOP, settings.randomWaitStopDelay());
                return currentDelay == 0;
            }
            case WAIT_STOP -> {
                movement.block();
                boolean stopped = movement.isPlayerStopped(settings.getVelocityThreshold());
                boolean timeout = elapsed >= currentDelay;

                if (stopped || timeout) {
                    startPhase(Phase.PRE_SWAP, settings.randomPreSwapDelay());
                    return currentDelay == 0;
                }
            }
            case PRE_SWAP -> {
                movement.block();
                if (elapsed >= currentDelay) {
                    startPhase(Phase.SWAP_TO_HAND, 0);
                    return true;
                }
            }
            case SWAP_TO_HAND -> {
                int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InventoryUtils.click(pearlSlot, hotbarSlot, SlotActionType.SWAP);
                startPhase(Phase.AWAIT_ITEM, 0);
                return true;
            }
            case AWAIT_ITEM -> {
                if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                    startPhase(Phase.THROW, 0);
                    return true;
                }
            }
            case THROW -> {
                sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                        Hand.MAIN_HAND,
                        sequence,
                        mc.player.getYaw(),
                        mc.player.getPitch()
                ));
                mc.player.swingHand(Hand.MAIN_HAND);
                startPhase(Phase.POST_THROW, settings.randomPostSwapDelay());
                return currentDelay == 0;
            }
            case POST_THROW -> {
                if (elapsed >= currentDelay) {
                    if (fromInventory) {
                        startPhase(Phase.SWAP_BACK, 0);
                        return true;
                    } else {
                        InventoryUtils.selectSlot(savedSlot);
                        startPhase(Phase.RESUMING, settings.randomResumeDelay());
                        return currentDelay == 0;
                    }
                }
            }
            case SWAP_BACK -> {
                int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InventoryUtils.click(pearlSlot, hotbarSlot, SlotActionType.SWAP);
                InventoryUtils.selectSlot(savedSlot);
                if (settings.shouldCloseInventory()) {
                    InventoryUtils.closeScreen();
                }
                startPhase(Phase.RESUMING, settings.randomResumeDelay());
                return currentDelay == 0;
            }
            case RESUMING -> {
                if (elapsed >= currentDelay) {
                    if (buildSettings().shouldStopMovement()) {
                        movement.restoreFromCurrent();
                    }
                    resetState();
                    return false;
                }
            }
        }
        return false;
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

    private void startPhase(Phase newPhase, int delay) {
        this.phase = newPhase;
        this.phaseStartTime = System.currentTimeMillis();
        this.currentDelay = delay;
    }

    private void resetState() {
        movement.reset();
        phase = Phase.IDLE;
        savedSlot = -1;
        pearlSlot = -1;
        fromInventory = false;
        phaseStartTime = 0;
        currentDelay = 0;
    }

    public boolean isRunning() {
        return phase != Phase.IDLE;
    }

    @Override
    public void deactivate() {
        if (movement.isBlocked()) {
            movement.restoreFromCurrent();
        }
        resetState();
    }
}