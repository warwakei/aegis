package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.util.InputUtil;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.simulate.Simulations;
import fun.aegis.utils.display.interfaces.QuickImports;

import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.events.container.CloseScreenEvent;
import fun.aegis.events.item.ClickSlotEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryMove extends Module implements QuickImports {
    private final List<Packet<?>> packets = new ArrayList<>();
    private final List<ClickSlotC2SPacket> pendingPackets = new ArrayList<>();
    public static List<ClickSlotEvent> packete = new ArrayList<>();
    public static List<ClickSlotEvent> packete1 = new ArrayList<>();

    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Vanilla", "Spooky", "Grim", "Normal", "Legit")
            .selected("Vanilla");

    private final BooleanSetting syncSwap = new BooleanSetting("Синх свапы", "Синхронизация свапов")
            .visible(() -> !mode.isSelected("Vanilla"));

    // Переменные для новых режимов
    private int tick = 0;
    private boolean inventoryOpened = false;
    private boolean packetsHeld = false;

    // Переменные для Legit режима
    enum MovePhase { READY, SLOWING_DOWN, ALLOW_MOVEMENT, SPEEDING_UP, SEND_PACKETS, FINISHED }
    MovePhase movePhase = MovePhase.READY;
    long actionStartTime = 0L;
    boolean playerFullyStopped = false;
    boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed, wasJumpPressed;
    boolean keysOverridden = false;

    public InventoryMove() {
        super("InventoryMove", "Inventory Move", ModuleCategory.MOVEMENT);
        setup(mode, syncSwap);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null) return;

        switch (mode.getSelected()) {
            case "Vanilla", "Normal" -> handleNormalPacket(e);
            case "Legit" -> handleLegitPacket(e);
            case "Spooky", "Grim" -> handleSpookyGrimPacket(e);
        }
    }

    private void handleNormalPacket(PacketEvent e) {
        // Базовая обработка пакетов для Normal режима
    }

    private void handleLegitPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClickSlotC2SPacket slot when (packetsHeld || Simulations.hasPlayerMovement()) && InventoryFlowManager.shouldSkipExecution() -> {
                packets.add(slot);
                e.cancel();
                packetsHeld = true;
            }
            case CloseScreenS2CPacket screen when screen.getSyncId() == 0 -> e.cancel();
            default -> {}
        }
    }

    private void handleSpookyGrimPacket(PacketEvent e) {
        Packet<?> p = e.getPacket();
        if (p instanceof ClickSlotC2SPacket clickPacket) {
            if (isMoving()) {
                if (mc.currentScreen instanceof InventoryScreen) {
                    pendingPackets.add(clickPacket);
                    e.cancel();
                }
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        switch (mode.getSelected()) {
            case "Vanilla", "Normal" -> handleVanillaMovement();
            case "Legit" -> processLegitMovement();
            case "Spooky", "Grim" -> handleSpookyGrimMovement();
        }
    }

    private void handleVanillaMovement() {
        if (tick != 0) {
            stopAllMovement();
            tick--;
            return;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return;
        }

        if (mode.isSelected("Grim") && mc.currentScreen instanceof GenericContainerScreen) {
            return;
        }

        updateMovementKeys();
    }

    private void handleSpookyGrimMovement() {
        // Обработка для Spooky и Grim режимов
        if (tick != 0) {
            stopAllMovement();
            tick--;
            return;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return;
        }

        if (mode.isSelected("Grim") && mc.currentScreen instanceof GenericContainerScreen) {
            return;
        }

        updateMovementKeys();
    }

    private void stopAllMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    private void updateMovementKeys() {
        boolean[] keys = {
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode()),
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode()),
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode()),
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode()),
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode()),
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sprintKey.getDefaultKey().getCode())
        };

        mc.options.forwardKey.setPressed(keys[0]);
        mc.options.backKey.setPressed(keys[1]);
        mc.options.leftKey.setPressed(keys[2]);
        mc.options.rightKey.setPressed(keys[3]);
        mc.options.jumpKey.setPressed(keys[4]);
        mc.options.sprintKey.setPressed(keys[5]);
    }

    private boolean isMoving() {
        return mc.player != null && (mc.player.input.movementForward != 0 || 
                mc.player.input.movementSideways != 0 || mc.options.jumpKey.isPressed());
    }

    private void processLegitMovement() {
        boolean hasOpenScreen = mc.currentScreen != null;

        if (hasOpenScreen && !inventoryOpened && movePhase == MovePhase.READY) {
            startLegitMovement();
            inventoryOpened = true;
        }

        if (!hasOpenScreen && inventoryOpened) {
            if (packetsHeld && movePhase == MovePhase.ALLOW_MOVEMENT) {
                movePhase = MovePhase.SLOWING_DOWN;
                actionStartTime = System.currentTimeMillis();
            } else if (!packetsHeld) {
                resetState();
            }
            inventoryOpened = false;
            return;
        }

        if (movePhase != MovePhase.READY) {
            handleMovementStates();
        }
    }

    private void startLegitMovement() {
        wasForwardPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
        wasBackPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode());
        wasLeftPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode());
        wasRightPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode());
        wasJumpPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode());

        movePhase = MovePhase.ALLOW_MOVEMENT;
        keysOverridden = false;
        packetsHeld = false;
    }

    private void handleMovementStates() {
        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (movePhase) {
            case SLOWING_DOWN -> {
                if (mc.player != null && mc.player.input != null) {
                    mc.player.input.movementForward = 0;
                    mc.player.input.movementSideways = 0;
                }

                if (!keysOverridden) {
                    stopAllMovement();
                    keysOverridden = true;
                }

                if (elapsed > 1) {
                    movePhase = MovePhase.SEND_PACKETS;
                    actionStartTime = System.currentTimeMillis();
                }
            }

            case ALLOW_MOVEMENT -> {
                if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution()) {
                    InventoryFlowManager.updateMoveKeys();
                }
            }

            case SEND_PACKETS -> {
                if (!packets.isEmpty()) {
                    packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                    packets.clear();
                    InventoryTask.updateSlots();
                }
                packetsHeld = false;
                movePhase = MovePhase.SPEEDING_UP;
                actionStartTime = System.currentTimeMillis();
            }

            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - actionStartTime;
                float speedupProgress = Math.min(1.0f, speedupElapsed / 1.0f);

                if (keysOverridden) {
                    restoreKeyStates();
                }

                if (mc.player != null && mc.player.input != null) {
                    boolean forward = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
                    float targetForward = forward ? 1.0f : 0;
                    mc.player.input.movementForward = lerp(mc.player.input.movementForward, targetForward * speedupProgress, 0.4f);

                    if (speedupProgress > 0.5f && forward && !mc.player.isSprinting()) {
                        mc.player.setSprinting(true);
                    }
                }

                if (speedupElapsed > 1) {
                    movePhase = MovePhase.FINISHED;
                }
            }

            case FINISHED -> {
                resetState();
            }
        }
    }

    private void restoreKeyStates() {
        boolean currentForward = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
        boolean currentBack = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode());
        boolean currentLeft = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode());
        boolean currentRight = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode());
        boolean currentJump = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode());

        mc.options.forwardKey.setPressed(wasForwardPressed && currentForward);
        mc.options.backKey.setPressed(wasBackPressed && currentBack);
        mc.options.leftKey.setPressed(wasLeftPressed && currentLeft);
        mc.options.rightKey.setPressed(wasRightPressed && currentRight);
        mc.options.jumpKey.setPressed(wasJumpPressed && currentJump);
        keysOverridden = false;
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private void resetState() {
        if (keysOverridden) {
            restoreKeyStates();
        }
        movePhase = MovePhase.READY;
        playerFullyStopped = false;
        inventoryOpened = false;
        packetsHeld = false;
        packets.clear();
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        if (mode.isSelected("Legit")) {
            SlotActionType actionType = e.getActionType();
            if ((packetsHeld || Simulations.hasPlayerMovement()) && ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW)) || actionType.equals(SlotActionType.PICKUP_ALL))) {
                e.cancel();
            }
        }
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent e) {
        if (mode.isSelected("Legit") && packetsHeld && movePhase == MovePhase.ALLOW_MOVEMENT) {
            movePhase = MovePhase.SLOWING_DOWN;
            actionStartTime = System.currentTimeMillis();
        } else if ((mode.isSelected("Spooky") || mode.isSelected("Grim")) && 
                   mc.currentScreen instanceof InventoryScreen && 
                   !pendingPackets.isEmpty() && 
                   isMoving()) {
            
            // Создаем отдельный поток для задержки и отправки пакетов
            new Thread(() -> {
                tick = 5;
                try {
                    Thread.sleep(mode.isSelected("Spooky") ? 90 : 40);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                for (ClickSlotC2SPacket pkt : pendingPackets) {
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(pkt);
                    }
                }
                pendingPackets.clear();
            }).start();
            e.cancel();
        } else {
            pendingPackets.clear();
        }
    }

    public static void stopMovementTemporarily(int ticks) {
        // Находим экземпляр InventoryMove
        for (var module : fun.aegis.Aegis.getInstance().getModuleRepository().modules()) {
            if (module instanceof InventoryMove inventoryMove && inventoryMove.isState()) {
                inventoryMove.tick = Math.max(inventoryMove.tick, ticks);
                break;
            }
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        pendingPackets.clear();
        packete.clear();
        packete1.clear();
        packets.clear();
        resetState();
    }
}
