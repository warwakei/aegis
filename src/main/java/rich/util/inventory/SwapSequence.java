package rich.util.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class SwapSequence {
    private final List<SwapStep> steps = new ArrayList<>();
    private int currentIndex;
    private int tickCounter;
    private boolean running;

    public SwapSequence step(int delayTicks, Runnable action) {
        return step(delayTicks, action, () -> true);
    }

    public SwapSequence step(int delayTicks, Runnable action, BooleanSupplier condition) {
        steps.add(new SwapStep(delayTicks, action, condition));
        return this;
    }

    public SwapSequence start() {
        currentIndex = 0;
        tickCounter = 0;
        running = true;
        return this;
    }

    public void tick() {
        if (!running || currentIndex >= steps.size()) {
            running = false;
            return;
        }

        SwapStep current = steps.get(currentIndex);

        if (!current.condition.getAsBoolean()) {
            return;
        }

        if (tickCounter >= current.delayTicks) {
            current.action.run();
            currentIndex++;
            tickCounter = 0;
        } else {
            tickCounter++;
        }
    }

    public boolean isFinished() {
        return !running || currentIndex >= steps.size();
    }

    public void reset() {
        steps.clear();
        currentIndex = 0;
        tickCounter = 0;
        running = false;
    }

    public void cancel() {
        running = false;
    }

    private record SwapStep(int delayTicks, Runnable action, BooleanSupplier condition) {}
}