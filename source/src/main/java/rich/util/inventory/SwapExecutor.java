package rich.util.inventory;

import net.minecraft.client.MinecraftClient;

public class SwapExecutor {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum Phase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAPPING, POST_SWAP, RESUMING, FINISHED
    }

    private Phase phase = Phase.IDLE;
    private final MovementController movement = new MovementController();
    private SwapSettings settings = SwapSettings.defaults();

    private Runnable swapAction;
    private Runnable onComplete;

    private long phaseStartTime;
    private int currentDelay;

    public void execute(Runnable swapAction, SwapSettings settings) {
        execute(swapAction, settings, null);
    }

    public void execute(Runnable swapAction, SwapSettings settings, Runnable onComplete) {
        if (phase != Phase.IDLE) return;

        this.swapAction = swapAction;
        this.settings = settings != null ? settings : SwapSettings.defaults();
        this.onComplete = onComplete;

        if (this.settings.shouldStopMovement()) {
            movement.saveState();
            movement.block();
            if (this.settings.shouldStopSprint()) {
                movement.stopSprint();
            }
            startPhase(Phase.PRE_STOP, this.settings.randomPreStopDelay());
        } else {
            startPhase(Phase.SWAPPING, 0);
        }
    }

    public void tick() {
        if (phase == Phase.IDLE || phase == Phase.FINISHED) return;
        if (mc.player == null) {
            reset();
            return;
        }

        if (settings.shouldStopMovement() && phase != Phase.RESUMING && phase != Phase.FINISHED) {
            movement.block();
            if (settings.shouldStopSprint()) {
                movement.stopSprint();
            }
        }

        boolean continueProcessing = true;
        int maxIterations = 10;
        int iterations = 0;

        while (continueProcessing && iterations < maxIterations) {
            iterations++;
            continueProcessing = processPhase();
        }
    }

    private boolean processPhase() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;

        switch (phase) {
            case PRE_STOP -> {
                movement.block();
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                if (elapsed >= currentDelay) {
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
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                boolean stopped = movement.isPlayerStopped(settings.getVelocityThreshold());
                boolean timeout = elapsed >= currentDelay;

                if (stopped || timeout) {
                    startPhase(Phase.PRE_SWAP, settings.randomPreSwapDelay());
                    return currentDelay == 0;
                }
            }
            case PRE_SWAP -> {
                movement.block();
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                if (elapsed >= currentDelay) {
                    startPhase(Phase.SWAPPING, 0);
                    return true;
                }
            }
            case SWAPPING -> {
                movement.block();
                if (settings.shouldStopSprint()) {
                    movement.stopSprint();
                }
                if (swapAction != null) {
                    swapAction.run();
                }
                startPhase(Phase.POST_SWAP, settings.randomPostSwapDelay());
                return currentDelay == 0;
            }
            case POST_SWAP -> {
                movement.block();
                if (elapsed >= currentDelay) {
                    if (settings.shouldCloseInventory()) {
                        InventoryUtils.closeScreen();
                    }
                    startPhase(Phase.RESUMING, settings.randomResumeDelay());
                    return currentDelay == 0;
                }
            }
            case RESUMING -> {
                if (elapsed >= currentDelay) {
                    if (settings.shouldStopMovement()) {
                        movement.restoreFromCurrent();
                    }
                    phase = Phase.FINISHED;
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    reset();
                    return false;
                }
            }
        }
        return false;
    }

    private void startPhase(Phase newPhase, int delay) {
        this.phase = newPhase;
        this.phaseStartTime = System.currentTimeMillis();
        this.currentDelay = delay;
    }

    public void cancel() {
        if (movement.isBlocked()) {
            movement.restoreFromCurrent();
        }
        reset();
    }

    public void reset() {
        phase = Phase.IDLE;
        swapAction = null;
        onComplete = null;
        movement.reset();
    }

    public boolean isRunning() {
        return phase != Phase.IDLE && phase != Phase.FINISHED;
    }

    public boolean isBlocking() {
        return movement.isBlocked() || (isRunning() && settings.shouldStopMovement());
    }

    public Phase getPhase() {
        return phase;
    }
}