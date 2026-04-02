package rich.modules.impl.combat.macetarget.armor;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import rich.mixin.ClientWorldAccessor;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.macetarget.state.MaceState.FireworkPhase;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.MovementController;
import rich.util.inventory.SwapSettings;

import java.util.function.IntFunction;

@Getter
public class FireworkHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final MovementController movement = new MovementController();

    @Setter
    private FireworkPhase phase = FireworkPhase.IDLE;
    private int slot = -1;
    private int savedSlot = -1;
    private boolean fromInventory = false;
    private long phaseStartTime = 0;
    private int currentDelay = 0;

    private final SwapSettingsProvider settingsProvider;

    @FunctionalInterface
    public interface SwapSettingsProvider {
        SwapSettings get();
    }

    public FireworkHandler(SwapSettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public boolean isActive() {
        return phase != FireworkPhase.IDLE;
    }

    public void useFirework(boolean isSilent) {
        if (isSilent) {
            useSilent();
        } else {
            startLegit();
        }
    }

    private void useSilent() {
        if (mc.player == null) return;

        int hotbarSlot = InventoryUtils.findHotbarItem(Items.FIREWORK_ROCKET);
        if (hotbarSlot != -1) {
            int currentSlot = mc.player.getInventory().getSelectedSlot();

            if (hotbarSlot != currentSlot) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            }

            Angle rotation = getRotation();
            sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    sequence,
                    rotation.getYaw(),
                    rotation.getPitch()
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

            Angle rotation = getRotation();
            sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    sequence,
                    rotation.getYaw(),
                    rotation.getPitch()
            ));

            InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);
            InventoryUtils.closeScreen();
        }
    }

    private void startLegit() {
        if (mc.player == null) return;

        savedSlot = mc.player.getInventory().getSelectedSlot();

        int hotbarSlot = InventoryUtils.findHotbarItem(Items.FIREWORK_ROCKET);
        if (hotbarSlot != -1) {
            slot = hotbarSlot;
            fromInventory = false;
            InventoryUtils.selectSlot(slot);
            startPhase(FireworkPhase.AWAIT_ITEM, 0);
            return;
        }

        int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
        if (invSlot != -1) {
            slot = invSlot;
            fromInventory = true;

            SwapSettings settings = settingsProvider.get();
            if (settings.shouldStopMovement()) {
                startPhase(FireworkPhase.PRE_STOP, settings.randomPreStopDelay());
            } else {
                startPhase(FireworkPhase.SWAP_TO_HAND, 0);
            }
        }
    }

    public void processLoop() {
        if (phase == FireworkPhase.IDLE) return;

        boolean continueProcessing = true;
        int iterations = 0;

        while (continueProcessing && iterations < 10) {
            iterations++;
            continueProcessing = processTick();
        }
    }

    private boolean processTick() {
        if (mc.player == null) {
            reset();
            return false;
        }

        long elapsed = System.currentTimeMillis() - phaseStartTime;
        SwapSettings settings = settingsProvider.get();

        switch (phase) {
            case PRE_STOP -> {
                if (elapsed >= currentDelay) {
                    movement.saveState();
                    movement.block();
                    if (settings.shouldStopSprint()) {
                        movement.stopSprint();
                    }
                    startPhase(FireworkPhase.STOPPING, 0);
                    return true;
                }
            }
            case STOPPING -> {
                movement.block();
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                startPhase(FireworkPhase.WAIT_STOP, settings.randomWaitStopDelay());
                return currentDelay == 0;
            }
            case WAIT_STOP -> {
                movement.block();
                boolean stopped = movement.isPlayerStopped(settings.getVelocityThreshold());
                boolean timeout = elapsed >= currentDelay;

                if (stopped || timeout) {
                    startPhase(FireworkPhase.PRE_SWAP, settings.randomPreSwapDelay());
                    return currentDelay == 0;
                }
            }
            case PRE_SWAP -> {
                movement.block();
                if (elapsed >= currentDelay) {
                    startPhase(FireworkPhase.SWAP_TO_HAND, 0);
                    return true;
                }
            }
            case SWAP_TO_HAND -> {
                int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InventoryUtils.click(slot, hotbarSlot, SlotActionType.SWAP);
                startPhase(FireworkPhase.AWAIT_ITEM, 0);
                return true;
            }
            case AWAIT_ITEM -> {
                if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
                    startPhase(FireworkPhase.USE, 0);
                    return true;
                }
            }
            case USE -> {
                Angle rotation = getRotation();
                sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(
                        Hand.MAIN_HAND,
                        sequence,
                        rotation.getYaw(),
                        rotation.getPitch()
                ));

                mc.player.swingHand(Hand.MAIN_HAND);
                startPhase(FireworkPhase.POST_USE, settings.randomPostSwapDelay());
                return currentDelay == 0;
            }
            case POST_USE -> {
                if (elapsed >= currentDelay) {
                    if (fromInventory) {
                        startPhase(FireworkPhase.SWAP_BACK, 0);
                        return true;
                    } else {
                        InventoryUtils.selectSlot(savedSlot);
                        startPhase(FireworkPhase.RESUMING, settings.randomResumeDelay());
                        return currentDelay == 0;
                    }
                }
            }
            case SWAP_BACK -> {
                int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InventoryUtils.click(slot, hotbarSlot, SlotActionType.SWAP);
                InventoryUtils.selectSlot(savedSlot);
                if (settings.shouldCloseInventory()) {
                    InventoryUtils.closeScreen();
                }
                startPhase(FireworkPhase.RESUMING, settings.randomResumeDelay());
                return currentDelay == 0;
            }
            case RESUMING -> {
                if (elapsed >= currentDelay) {
                    movement.restoreFromCurrent();
                    reset();
                    return false;
                }
            }
        }
        return false;
    }

    private Angle getRotation() {
        Angle rotation = AngleConnection.INSTANCE.getRotation();
        return rotation != null ? rotation : MathAngle.cameraAngle();
    }

    private void sendSequencedPacket(IntFunction<Packet<?>> packetCreator) {
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

    private void startPhase(FireworkPhase phase, int delay) {
        this.phase = phase;
        this.phaseStartTime = System.currentTimeMillis();
        this.currentDelay = delay;
    }

    public void reset() {
        movement.reset();
        phase = FireworkPhase.IDLE;
        slot = -1;
        savedSlot = -1;
        fromInventory = false;
        phaseStartTime = 0;
        currentDelay = 0;
    }

    public void forceRestore() {
        if (movement.isBlocked()) {
            movement.restoreFromCurrent();
        }
    }
}