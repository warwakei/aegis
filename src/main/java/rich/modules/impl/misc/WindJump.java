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
import rich.events.api.types.EventType;
import rich.events.impl.HotBarScrollEvent;
import rich.events.impl.InputEvent;
import rich.events.impl.KeyEvent;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.mixin.ClientWorldAccessor;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.screens.clickgui.impl.settingsrender.BindComponent;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.MovementController;
import rich.util.inventory.SwapSettings;
import rich.util.math.TaskPriority;
import rich.util.string.chat.ChatMessage;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WindJump extends ModuleStructure {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ броска")
            .value("Instant", "Legit")
            .selected("Legit");

    final BindSetting keySetting = new BindSetting("Кнопка", "Кнопка для заряда ветра");

    enum Phase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_TO_HAND, AWAIT_ITEM, ROTATE_DOWN, THROW, POST_THROW, SWAP_BACK, RESUMING
    }

    final MovementController movement = new MovementController();
    final float THROW_PITCH = 90f;

    Phase phase = Phase.IDLE;
    int savedSlot = -1;
    int chargeSlot = -1;
    boolean fromInventory = false;
    long phaseStartTime = 0;
    int currentDelay = 0;
    long lastThrowTime = 0;
    int rotationTicks = 0;
    boolean pendingThrow = false;

    public WindJump() {
        super("WindJump", "Wind Jump", ModuleCategory.MISC);
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
            tryThrowCharge();
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
            tryThrowCharge();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void tryThrowCharge() {
        if (System.currentTimeMillis() - lastThrowTime < 100) return;
        lastThrowTime = System.currentTimeMillis();
        startChargeProcess();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getType() == EventType.PRE) {
            if (phase == Phase.ROTATE_DOWN || phase == Phase.THROW) {
                Angle throwAngle = new Angle(mc.player.getYaw(), THROW_PITCH);
                AngleConfig config = new AngleConfig(new LinearConstructor(), true, true);

                AngleConnection.INSTANCE.rotateTo(
                        throwAngle,
                        3,
                        config,
                        TaskPriority.HIGH_IMPORTANCE_1,
                        this
                );
            }
        }

        if (event.getType() == EventType.POST) {
            if (pendingThrow) {
                performThrow();
                pendingThrow = false;
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performThrow() {
        if (mc.player == null) return;

        Angle rotation = AngleConnection.INSTANCE.getRotation();
        if (rotation == null) {
            rotation = MathAngle.cameraAngle();
        }

        final Angle finalRotation = rotation;
        sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                sequence,
                finalRotation.getYaw(),
                finalRotation.getPitch()
        ));

        mc.player.swingHand(Hand.MAIN_HAND);

        SwapSettings settings = buildSettings();
        startPhase(Phase.POST_THROW, settings.randomPostSwapDelay());
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
    private void startChargeProcess() {
        savedSlot = mc.player.getInventory().getSelectedSlot();

        int hotbarSlot = InventoryUtils.findItemInHotbar(Items.WIND_CHARGE);
        if (hotbarSlot != -1) {
            chargeSlot = hotbarSlot;
            fromInventory = false;
            InventoryUtils.selectSlot(chargeSlot);
            startPhase(Phase.AWAIT_ITEM, 0);
            return;
        }

        int invSlot = InventoryUtils.findItemInInventory(Items.WIND_CHARGE);
        if (invSlot != -1) {
            chargeSlot = invSlot;
            fromInventory = true;

            SwapSettings settings = buildSettings();
            if (settings.shouldStopMovement()) {
                startPhase(Phase.PRE_STOP, settings.randomPreStopDelay());
            } else {
                startPhase(Phase.SWAP_TO_HAND, 0);
            }
        } else {
            ChatMessage.brandmessage("Нету заряда ветра");
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
                InventoryUtils.click(chargeSlot, hotbarSlot, SlotActionType.SWAP);
                startPhase(Phase.AWAIT_ITEM, 0);
                return true;
            }
            case AWAIT_ITEM -> {
                if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
                    rotationTicks = 0;
                    startPhase(Phase.ROTATE_DOWN, 0);
                    return true;
                }
            }
            case ROTATE_DOWN -> {
                rotationTicks++;

                Angle currentRotation = AngleConnection.INSTANCE.getRotation();
                boolean rotationReady = currentRotation != null && currentRotation.getPitch() >= 80f;
                boolean waitedEnough = rotationTicks >= 2;

                if (rotationReady && waitedEnough) {
                    startPhase(Phase.THROW, 0);
                    return true;
                }

                if (rotationTicks > 10) {
                    resetState();
                    return false;
                }
            }
            case THROW -> {
                pendingThrow = true;
                return false;
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
                InventoryUtils.click(chargeSlot, hotbarSlot, SlotActionType.SWAP);
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
                    AngleConnection.INSTANCE.startReturning();
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
        chargeSlot = -1;
        fromInventory = false;
        phaseStartTime = 0;
        currentDelay = 0;
        rotationTicks = 0;
        pendingThrow = false;
    }

    public boolean isRunning() {
        return phase != Phase.IDLE;
    }

    @Override
    public void deactivate() {
        if (movement.isBlocked()) {
            movement.restoreFromCurrent();
        }
        AngleConnection.INSTANCE.startReturning();
        resetState();
    }
}