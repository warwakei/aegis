package rich.modules.impl.combat.macetarget.armor;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import rich.modules.impl.combat.macetarget.state.MaceState.SwapPhase;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.MovementController;
import rich.util.inventory.SwapSettings;

@Getter
public class ArmorSwapHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final MovementController movement = new MovementController();

    @Setter
    private SwapPhase phase = SwapPhase.IDLE;
    private int slot = -1;
    private long phaseStartTime = 0;
    private int currentDelay = 0;

    private final SwapSettingsProvider settingsProvider;

    @FunctionalInterface
    public interface SwapSettingsProvider {
        SwapSettings get();
    }

    public ArmorSwapHandler(SwapSettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public boolean isActive() {
        return phase != SwapPhase.IDLE;
    }

    public void startSwap(int slot, boolean isSilent) {
        if (isSilent) {
            swapSilent(slot);
        } else {
            startLegit(slot);
        }
    }

    private void swapSilent(int slot) {
        int wrappedSlot = InventoryUtils.wrapSlot(slot);
        InventoryUtils.swap(wrappedSlot, 6);
        InventoryUtils.closeScreen();
    }

    private void startLegit(int slot) {
        this.slot = slot;
        SwapSettings settings = settingsProvider.get();

        if (settings.shouldStopMovement()) {
            startPhase(SwapPhase.PRE_STOP, settings.randomPreStopDelay());
        } else {
            startPhase(SwapPhase.DO_SWAP, 0);
        }
    }

    public void processLoop() {
        if (phase == SwapPhase.IDLE) return;

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
                    startPhase(SwapPhase.STOPPING, 0);
                    return true;
                }
            }
            case STOPPING -> {
                movement.block();
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                startPhase(SwapPhase.WAIT_STOP, settings.randomWaitStopDelay());
                return currentDelay == 0;
            }
            case WAIT_STOP -> {
                movement.block();
                boolean stopped = movement.isPlayerStopped(settings.getVelocityThreshold());
                boolean timeout = elapsed >= currentDelay;

                if (stopped || timeout) {
                    startPhase(SwapPhase.PRE_SWAP, settings.randomPreSwapDelay());
                    return currentDelay == 0;
                }
            }
            case PRE_SWAP -> {
                movement.block();
                if (elapsed >= currentDelay) {
                    startPhase(SwapPhase.DO_SWAP, 0);
                    return true;
                }
            }
            case DO_SWAP -> {
                int wrappedSlot = InventoryUtils.wrapSlot(slot);
                InventoryUtils.swap(wrappedSlot, 6);
                startPhase(SwapPhase.POST_SWAP, settings.randomPostSwapDelay());
                return currentDelay == 0;
            }
            case POST_SWAP -> {
                if (elapsed >= currentDelay) {
                    if (settings.shouldCloseInventory()) {
                        InventoryUtils.closeScreen();
                    }
                    startPhase(SwapPhase.RESUMING, settings.randomResumeDelay());
                    return currentDelay == 0;
                }
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

    private void startPhase(SwapPhase phase, int delay) {
        this.phase = phase;
        this.phaseStartTime = System.currentTimeMillis();
        this.currentDelay = delay;
    }

    public void reset() {
        movement.reset();
        phase = SwapPhase.IDLE;
        slot = -1;
        phaseStartTime = 0;
        currentDelay = 0;
    }

    public void forceRestore() {
        if (movement.isBlocked()) {
            movement.restoreFromCurrent();
        }
    }
}