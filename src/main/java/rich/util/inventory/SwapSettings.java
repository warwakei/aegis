package rich.util.inventory;

public class SwapSettings {
    private boolean stopMovement = true;
    private boolean stopSprint = true;
    private boolean closeInventory = true;

    private int preStopDelayMin = 0;
    private int preStopDelayMax = 50;

    private int waitStopDelayMin = 50;
    private int waitStopDelayMax = 150;

    private int preSwapDelayMin = 20;
    private int preSwapDelayMax = 100;

    private int postSwapDelayMin = 20;
    private int postSwapDelayMax = 80;

    private int resumeDelayMin = 50;
    private int resumeDelayMax = 150;

    private double velocityThreshold = 0.001;

    public static SwapSettings defaults() {
        return new SwapSettings();
    }

    public static SwapSettings instant() {
        return new SwapSettings()
                .stopMovement(false)
                .stopSprint(false)
                .preStopDelay(0, 0)
                .waitStopDelay(0, 0)
                .preSwapDelay(0, 0)
                .postSwapDelay(0, 0)
                .resumeDelay(0, 0);
    }

    public static SwapSettings instantWithStop() {
        return new SwapSettings()
                .stopMovement(true)
                .stopSprint(true)
                .preStopDelay(0, 0)
                .waitStopDelay(15, 30)
                .preSwapDelay(0, 5)
                .postSwapDelay(0, 5)
                .resumeDelay(10, 20);
    }

    public static SwapSettings legit() {
        return new SwapSettings()
                .stopMovement(true)
                .stopSprint(true)
                .preStopDelay(0, 10)
                .waitStopDelay(40, 80)
                .preSwapDelay(15, 40)
                .postSwapDelay(15, 30)
                .resumeDelay(25, 50);
    }

    public SwapSettings stopMovement(boolean value) {
        this.stopMovement = value;
        return this;
    }

    public SwapSettings stopSprint(boolean value) {
        this.stopSprint = value;
        return this;
    }

    public SwapSettings closeInventory(boolean value) {
        this.closeInventory = value;
        return this;
    }

    public SwapSettings preStopDelay(int min, int max) {
        this.preStopDelayMin = min;
        this.preStopDelayMax = max;
        return this;
    }

    public SwapSettings waitStopDelay(int min, int max) {
        this.waitStopDelayMin = min;
        this.waitStopDelayMax = max;
        return this;
    }

    public SwapSettings preSwapDelay(int min, int max) {
        this.preSwapDelayMin = min;
        this.preSwapDelayMax = max;
        return this;
    }

    public SwapSettings postSwapDelay(int min, int max) {
        this.postSwapDelayMin = min;
        this.postSwapDelayMax = max;
        return this;
    }

    public SwapSettings resumeDelay(int min, int max) {
        this.resumeDelayMin = min;
        this.resumeDelayMax = max;
        return this;
    }

    public SwapSettings velocityThreshold(double value) {
        this.velocityThreshold = value;
        return this;
    }

    public boolean shouldStopMovement() { return stopMovement; }
    public boolean shouldStopSprint() { return stopSprint; }
    public boolean shouldCloseInventory() { return closeInventory; }
    public double getVelocityThreshold() { return velocityThreshold; }

    public int randomPreStopDelay() { return random(preStopDelayMin, preStopDelayMax); }
    public int randomWaitStopDelay() { return random(waitStopDelayMin, waitStopDelayMax); }
    public int randomPreSwapDelay() { return random(preSwapDelayMin, preSwapDelayMax); }
    public int randomPostSwapDelay() { return random(postSwapDelayMin, postSwapDelayMax); }
    public int randomResumeDelay() { return random(resumeDelayMin, resumeDelayMax); }

    private int random(int min, int max) {
        if (min >= max) return min;
        return min + (int)(Math.random() * (max - min + 1));
    }
}