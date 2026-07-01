package rich.util.timer;

import lombok.Getter;

/**
 *  © 2025 © 2026 Copyright Aegis Neo 063 - Dev Build 2026 14:03 21.04 Client 2.0
 *        All Rights Reserved ®
 */

@Getter
public class StopWatch {

    private long startTime;

    public StopWatch() {
        reset();
    }

    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= startTime;
    }

    public boolean every(final double delay) {
        boolean finished = this.finished(delay);
        if (finished) reset();
        return finished;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
    }

    public long elapsedTime() {
        return System.currentTimeMillis() - this.startTime;
    }

    public StopWatch setMs(long ms) {
        this.startTime = System.currentTimeMillis() - ms;
        return this;
    }
}