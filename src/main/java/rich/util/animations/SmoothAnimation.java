package rich.util.animations;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SmoothAnimation {
    private long start;
    private double duration;
    private double fromValue;
    private double toValue;
    @Setter
    private double value;
    private double prevValue;
    private Easing easing = Easings.EXPO_OUT;
    private boolean finished = false;

    public SmoothAnimation run(double valueTo, double durationSeconds) {
        return this.run(valueTo, durationSeconds, Easings.EXPO_OUT, false);
    }

    public SmoothAnimation run(double valueTo, double durationSeconds, Easing easing) {
        return this.run(valueTo, durationSeconds, easing, false);
    }

    public SmoothAnimation run(double valueTo, double durationSeconds, boolean safe) {
        return this.run(valueTo, durationSeconds, Easings.EXPO_OUT, safe);
    }

    public SmoothAnimation run(double valueTo, double durationSeconds, Easing easing, boolean safe) {
        if (this.check(safe, valueTo)) {
            return this;
        }
        this.easing = easing;
        this.start = System.currentTimeMillis();
        this.duration = durationSeconds * 1000.0;
        this.fromValue = this.value;
        this.toValue = valueTo;
        this.finished = this.fromValue == this.toValue;
        return this;
    }

    public boolean update() {
        this.prevValue = this.value;
        boolean alive = this.isAlive();

        if (System.currentTimeMillis() - this.start > this.duration / 1.5) {
            this.finished = this.fromValue == this.toValue;
        }

        if (alive) {
            double part = Math.min(1.0, Math.max(0.0, this.calculatePart()));
            this.value = this.interpolate(this.fromValue, this.toValue, this.easing.ease(part));
        } else {
            this.start = 0L;
            this.value = this.toValue;
        }

        return alive;
    }

    public boolean isAlive() {
        return !this.isFinished();
    }

    public boolean isFinished() {
        return this.calculatePart() >= 1.0;
    }

    public double calculatePart() {
        if (duration <= 0) return 1.0;
        return (double) (System.currentTimeMillis() - this.start) / this.duration;
    }

    public boolean check(boolean safe, double valueTo) {
        return safe && this.isAlive() && (valueTo == this.fromValue || valueTo == this.toValue || valueTo == this.value);
    }

    public double interpolate(double start, double end, double pct) {
        return start + (end - start) * pct;
    }

    public float get() {
        return (float) this.value;
    }

    public float getPrev() {
        return (float) this.prevValue;
    }

    public void set(double value) {
        this.run(value, 0.0001);
        this.update();
        this.value = value;
    }
}