package fun.aegis.common.animation;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class Animation implements AnimationCalculation {
    protected long startTime = System.currentTimeMillis();
    protected int ms;
    protected double value;
    protected Direction direction = Direction.FORWARDS;

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public boolean isDone() {
        return System.currentTimeMillis() - startTime >= ms;
    }

    public boolean isFinished(Direction direction) {
        return this.direction == direction && isDone();
    }

    public Direction getDirection() {
        return this.direction;
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            adjustTimer();
        }
    }

    public boolean isDirection(Direction direction) {
        return this.direction == direction;
    }

    private void adjustTimer() {
        long elapsed = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis() - ((long) ms - Math.min(ms, elapsed));
    }

    public Double getOutput() {
        double time = (1 - calculation(getElapsedTime())) * value;

        return direction == Direction.FORWARDS
                ? endValue()
                : isDone() ? 0.0 : time;
    }

    protected double endValue() {
        return isDone()
                ? value
                : calculation(getElapsedTime()) * value;
    }
    
    protected long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    protected double calculation(long time) {
        return Math.min(1.0, (double) time / ms);
    }
    
    public double update(double target) {
        double currentTime = calculation(getElapsedTime());
        value = direction == Direction.FORWARDS ? currentTime : 1.0 - currentTime;
        return value;
    }
    
    public double getValue() {
        return value;
    }
}
