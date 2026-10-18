package rich.util.animations;

public class SweepAnim {

    private float progress;
    private final float duration;
    private boolean completed;
    private boolean active;
    private long startTime;

    public SweepAnim(float durationSeconds) {
        this.progress = 0.0f;
        this.duration = durationSeconds * 1000f;
        this.completed = false;
        this.active = false;
        this.startTime = 0;
    }

    public void start() {
        if (!active && !completed) {
            progress = 0.0f;
            completed = false;
            active = true;
            startTime = System.currentTimeMillis();
        }
    }

    public void reset() {
        progress = 0.0f;
        completed = false;
        active = false;
        startTime = 0;
    }

    public void update() {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - startTime;
        progress = Math.min(elapsed / duration, 1.0f);

        if (progress >= 1.0f) {
            progress = 1.0f;
            completed = true;
            active = false;
        }
    }

    public float getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return completed && !active;
    }
}