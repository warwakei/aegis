package rich.util.animations;

import lombok.Getter;

@Getter
public class FadeAnimation {
    private final long duration;
    private long startTime;
    private boolean forwards = true;
    private double value = 0.0;
    private Easing easing = Easings.EXPO_OUT;

    public FadeAnimation(long durationMs) {
        this.duration = durationMs;
        this.startTime = System.currentTimeMillis();
    }

    public FadeAnimation(long durationMs, Easing easing) {
        this.duration = durationMs;
        this.startTime = System.currentTimeMillis();
        this.easing = easing;
    }

    public void switchDirection(boolean forwards) {
        if (this.forwards != forwards) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = duration - Math.min(elapsed, duration);
            this.startTime = System.currentTimeMillis() - remaining;
            this.forwards = forwards;
        }
    }

    public void setDirection(boolean forwards) {
        this.forwards = forwards;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.value = forwards ? 0.0 : 1.0;
    }

    public float get() {
        long elapsed = System.currentTimeMillis() - startTime;
        double progress = Math.min((double) elapsed / duration, 1.0);
        double easedProgress = easing.ease(progress);

        if (forwards) {
            value = easedProgress;
        } else {
            value = 1.0 - easedProgress;
        }

        return (float) Math.max(0, Math.min(1, value));
    }

    public boolean isDone() {
        return System.currentTimeMillis() - startTime >= duration;
    }

    public boolean isFullyHidden() {
        return isDone() && !forwards;
    }

    public boolean isFullyVisible() {
        return isDone() && forwards;
    }
}