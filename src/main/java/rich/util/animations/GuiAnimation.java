package rich.util.animations;

import lombok.Setter;
import lombok.experimental.Accessors;
import rich.util.timer.TimerUtil;

@Setter
@Accessors(chain = true)
public class GuiAnimation {
    public final TimerUtil counter = new TimerUtil();
    protected int ms = 250;
    protected double value = 1.0;
    protected Direction direction = Direction.FORWARDS;

    public GuiAnimation reset() {
        counter.resetCounter();
        return this;
    }

    public boolean isDone() {
        return counter.isReached(ms);
    }

    public boolean isFinished(Direction direction) {
        return this.direction == direction && isDone();
    }

    public Direction getDirection() {
        return this.direction;
    }

    public GuiAnimation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
        }
        return this;
    }

    public Double getOutput() {
        double progress = Math.min(1.0, (double) counter.getTime() / ms);
        double eased = easeOutQuart(progress);

        if (direction == Direction.FORWARDS) {
            return eased * value;
        } else {
            return (1.0 - eased) * value;
        }
    }

    private double easeOutQuart(double x) {
        return 1.0 - Math.pow(1.0 - x, 4);
    }
}