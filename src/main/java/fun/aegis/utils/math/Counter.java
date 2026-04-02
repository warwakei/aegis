package fun.aegis.utils.math;

import lombok.Getter;

@Getter
public class Counter {
    public long lastMS = System.currentTimeMillis();
    private long time;

    public Counter() {
        this.resetCounter();
        reset();
    }

    public static Counter create() {
        return new Counter();
    }

    public void resetCounter() {
        lastMS = System.currentTimeMillis();
    }

    public void reset() {
        this.time = System.nanoTime();
    }

    public boolean isReached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public void setLastMS(long newValue) {
        lastMS = System.currentTimeMillis() + newValue;
    }

    public void setTime(long time) {
        lastMS = time;
    }

    public long getPassedTimeMs() {
        return getMs(System.nanoTime() - time);
    }

    public long getMs(long time) {
        return time / 1000000L;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public boolean passedMs(long ms) {
        return getMs(System.nanoTime() - time) >= ms;
    }

    public boolean isRunning() {
        return System.currentTimeMillis() - lastMS <= 0;
    }

    public boolean hasTimeElapsed() {
        return lastMS < System.currentTimeMillis();
    }
}
