package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import rich.events.api.EventHandler;
import rich.events.impl.ClickSlotEvent;
import rich.events.impl.CloseScreenEvent;
import rich.events.impl.InputEvent;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.inventory.MovementController;
import rich.util.move.MoveUtil;
import rich.util.string.PlayerInteractionHelper;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryMove extends ModuleStructure {

    private final List<Packet<?>> packets = new ArrayList<>();

    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Normal", "Legit")
            .selected("Legit");

    private final BooleanSetting grimBypass = new BooleanSetting("Grim Bypass", "Остановка движения при закрытии инвентаря")
            .setValue(true);

    enum MovePhase {
        READY,
        ALLOW_MOVEMENT,
        STOPPING,
        WAIT_STOP,
        SLOWING_DOWN,
        SEND_PACKETS,
        SPEEDING_UP,
        RESUMING,
        CLOSE_INVENTORY,
        FINISHED
    }

    final MovementController movement = new MovementController();

    MovePhase movePhase = MovePhase.READY;
    long actionStartTime = 0L;
    int currentDelay = 0;
    boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed, wasJumpPressed;
    boolean keysOverridden = false;
    boolean inventoryOpened = false;
    boolean packetsHeld = false;
    boolean pendingClose = false;
    int closeScreenSyncId = -1;

    public InventoryMove() {
        super("InventoryMove", "Inventory Move", ModuleCategory.MOVEMENT);
        settings(mode, grimBypass);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        resetState();
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;
        if (movement.isBlocked()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPacket(PacketEvent e) {
        if (mode.isSelected("Legit")) {
            handleLegitPackets(e);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleLegitPackets(PacketEvent e) {
        if (e.getPacket() instanceof ClickSlotC2SPacket slot) {
            if ((packetsHeld || MoveUtil.hasPlayerMovement()) && shouldSkipExecution()) {
                packets.add(slot);
                e.cancel();
                packetsHeld = true;
            }
        } else if (e.getPacket() instanceof CloseScreenS2CPacket screen) {
            if (screen.getSyncId() == 0) {
                e.cancel();
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        if (mode.isSelected("Legit")) {
            processLegitMovement();
        } else {
            if (!isServerScreen() && shouldSkipExecution()) {
                updateMoveKeys();
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processLegitMovement() {
        boolean hasOpenScreen = mc.currentScreen != null;

        if (hasOpenScreen && !inventoryOpened && movePhase == MovePhase.READY) {
            startLegitMovement();
            inventoryOpened = true;
        }

        if (pendingClose) {
            handlePendingClose();
            return;
        }

        if (!hasOpenScreen && inventoryOpened && movePhase != MovePhase.READY) {
            inventoryOpened = false;
            if (movePhase == MovePhase.ALLOW_MOVEMENT) {
                resetState();
            }
            return;
        }

        if (movePhase != MovePhase.READY) {
            handleMovementStates();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handlePendingClose() {
        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (movePhase) {
            case STOPPING -> {
                movement.block();
                movement.stopSprint();
                movePhase = MovePhase.WAIT_STOP;
                actionStartTime = System.currentTimeMillis();
            }
            case WAIT_STOP -> {
                movement.block();
                boolean stopped = isPlayerStopped();
                if (stopped || elapsed >= 100) {
                    if (packetsHeld) {
                        movePhase = MovePhase.SEND_PACKETS;
                    } else {
                        movePhase = MovePhase.CLOSE_INVENTORY;
                    }
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case SLOWING_DOWN -> {
                blockMovementInput();
                if (elapsed > 1) {
                    movePhase = MovePhase.SEND_PACKETS;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case SEND_PACKETS -> {
                sendHeldPackets();
                movePhase = MovePhase.CLOSE_INVENTORY;
                actionStartTime = System.currentTimeMillis();
            }
            case CLOSE_INVENTORY -> {
                closeInventoryNow();
                movePhase = MovePhase.RESUMING;
                currentDelay = 20 + (int)(Math.random() * 30);
                actionStartTime = System.currentTimeMillis();
            }
            case RESUMING -> {
                if (elapsed >= currentDelay) {
                    if (movement.isBlocked()) {
                        movement.restoreFromCurrent();
                    }
                    if (keysOverridden) {
                        restoreKeyStates();
                    }
                    movePhase = MovePhase.FINISHED;
                }
            }
            case FINISHED -> resetState();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isPlayerStopped() {
        if (mc.player == null) return true;
        double vx = Math.abs(mc.player.getVelocity().x);
        double vz = Math.abs(mc.player.getVelocity().z);
        return vx < 0.03 && vz < 0.03;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void blockMovementInput() {
        if (mc.player != null && mc.player.input != null) {
            mc.player.input.playerInput = new PlayerInput(false, false, false, false,
                    mc.player.input.playerInput.jump(), mc.player.input.playerInput.sneak(), mc.player.input.playerInput.sprint());
        }
        if (!keysOverridden) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            keysOverridden = true;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void sendHeldPackets() {
        if (!packets.isEmpty()) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
            updateSlots();
        }
        packetsHeld = false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void closeInventoryNow() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (closeScreenSyncId != -1) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(closeScreenSyncId));
        }

        if (mc.currentScreen != null) {
            mc.currentScreen.close();
        }

        pendingClose = false;
        inventoryOpened = false;
        closeScreenSyncId = -1;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void startLegitMovement() {
        wasForwardPressed = isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode());
        wasBackPressed = isKeyPressed(mc.options.backKey.getDefaultKey().getCode());
        wasLeftPressed = isKeyPressed(mc.options.leftKey.getDefaultKey().getCode());
        wasRightPressed = isKeyPressed(mc.options.rightKey.getDefaultKey().getCode());
        wasJumpPressed = isKeyPressed(mc.options.jumpKey.getDefaultKey().getCode());
        movePhase = MovePhase.ALLOW_MOVEMENT;
        keysOverridden = false;
        packetsHeld = false;
        pendingClose = false;
        closeScreenSyncId = -1;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleMovementStates() {
        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (movePhase) {
            case ALLOW_MOVEMENT -> {
                if (!isServerScreen() && shouldSkipExecution()) updateMoveKeys();
            }
            case SPEEDING_UP -> {
                if (keysOverridden) restoreKeyStates();
                if (mc.player != null && elapsed > 1 && isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()) && !mc.player.isSprinting()) {
                    mc.player.setSprinting(true);
                }
                if (elapsed > 1) movePhase = MovePhase.FINISHED;
            }
            case RESUMING -> {
                if (elapsed >= currentDelay) {
                    movement.restoreFromCurrent();
                    movePhase = MovePhase.FINISHED;
                }
            }
            case FINISHED -> resetState();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void restoreKeyStates() {
        mc.options.forwardKey.setPressed(wasForwardPressed && isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(wasBackPressed && isKeyPressed(mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(wasLeftPressed && isKeyPressed(mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(wasRightPressed && isKeyPressed(mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(wasJumpPressed && isKeyPressed(mc.options.jumpKey.getDefaultKey().getCode()));
        keysOverridden = false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void resetState() {
        if (movement.isBlocked()) movement.restoreFromCurrent();
        if (keysOverridden) restoreKeyStates();
        movePhase = MovePhase.READY;
        inventoryOpened = false;
        packetsHeld = false;
        pendingClose = false;
        closeScreenSyncId = -1;
        packets.clear();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onClickSlot(ClickSlotEvent e) {
        if (mode.isSelected("Legit")) {
            SlotActionType actionType = e.getActionType();
            if ((packetsHeld || MoveUtil.hasPlayerMovement()) &&
                    ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW))
                            || actionType.equals(SlotActionType.PICKUP_ALL))) {
                e.cancel();
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onCloseScreen(CloseScreenEvent e) {
        if (!mode.isSelected("Legit")) return;
        if (movePhase != MovePhase.ALLOW_MOVEMENT) return;
        if (!shouldSkipExecution()) return;

        boolean needsStop = grimBypass.isValue() && MoveUtil.hasPlayerMovement();
        boolean hasPackets = packetsHeld;

        if (needsStop || hasPackets) {
            e.cancel();

            pendingClose = true;
            closeScreenSyncId = mc.player != null ? mc.player.currentScreenHandler.syncId : 0;

            if (needsStop) {
                movePhase = MovePhase.STOPPING;
            } else {
                movePhase = MovePhase.SLOWING_DOWN;
            }
            actionStartTime = System.currentTimeMillis();
        }
    }

    private boolean isKeyPressed(int keyCode) {
        return InputUtil.isKeyPressed(mc.getWindow(), keyCode);
    }

    private void updateMoveKeys() {
        mc.options.forwardKey.setPressed(isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(isKeyPressed(mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(isKeyPressed(mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(isKeyPressed(mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(isKeyPressed(mc.options.jumpKey.getDefaultKey().getCode()));
    }

    private boolean shouldSkipExecution() {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof SignEditScreen)
                && !(mc.currentScreen instanceof AnvilScreen) && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen);
    }

    private boolean isServerScreen() {
        return mc.player != null && mc.player.currentScreenHandler.slots.size() != 46;
    }

    private void updateSlots() {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP_ALL, mc.player);
    }
}