package rich.util.inventory.script;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import rich.util.timer.StopWatch;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

@Getter
@Setter
@SuppressWarnings("UnusedReturnValue")
public class Script {
    private final StopWatch time = new StopWatch();
    private final List<ScriptStep> scriptSteps = Lists.newCopyOnWriteArrayList();
    private final List<ScriptTickStep> scriptTickSteps = Lists.newCopyOnWriteArrayList();
    private int currentStepIndex;
    private int currentTickStepIndex;
    private boolean interrupt;
    private LoopStrategy loopStrategy = new FiniteLoopStrategy(1);

    public Script() {
        cleanup();
    }

    public Script addStep(int delay, ScriptAction action) {
        return addStep(delay, action, () -> true, 0);
    }

    public Script addStep(int delay, ScriptAction action, BooleanSupplier condition) {
        return addStep(delay, action, condition, 0);
    }

    public Script addStep(int delay, ScriptAction action, int priority) {
        return addStep(delay, action, () -> true, priority);
    }

    public Script addStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
        scriptSteps.add(new ScriptStep(delay, action, condition, priority));
        Collections.sort(scriptSteps);
        return this;
    }

    public Script addTickStep(int ticks, ScriptAction action) {
        return addTickStep(ticks, action, () -> true, 0);
    }

    public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition) {
        return addTickStep(ticks, action, condition, 0);
    }

    public Script addTickStep(int ticks, ScriptAction action, int priority) {
        return addTickStep(ticks, action, () -> true, priority);
    }

    public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
        scriptTickSteps.add(new ScriptTickStep(ticks, action, condition, priority));
        Collections.sort(scriptTickSteps);
        return this;
    }

    public void resetTime() {
        time.reset();
    }

    public void resetStepIndex() {
        currentStepIndex = 0;
        currentTickStepIndex = 0;
    }

    public Script cleanupIfFinished() {
        if (isFinished()) cleanup();
        return this;
    }

    public Script cleanup() {
        scriptSteps.clear();
        scriptTickSteps.clear();
        resetTime();
        resetStepIndex();
        return this;
    }

    public void update() {
        if (scriptSteps.isEmpty() && scriptTickSteps.isEmpty() || interrupt) {
            return;
        }

        scriptSteps.forEach(step -> {
            if (currentStepIndex < scriptSteps.size()) {
                ScriptStep currentStep = scriptSteps.get(currentStepIndex);
                if (currentStep.condition().getAsBoolean() && time.finished(currentStep.delay())) {
                    currentStep.action().perform();
                    ++currentStepIndex;
                    resetTime();
                    if (loopStrategy.shouldLoop(currentStepIndex, scriptSteps.size())) {
                        resetStepIndex();
                        loopStrategy.onLoop();
                    }
                }
            }
        });

        scriptTickSteps.forEach(step -> {
            if (currentTickStepIndex < scriptTickSteps.size()) {
                ScriptTickStep currentTickStep = scriptTickSteps.get(currentTickStepIndex);
                if (currentTickStep.condition().getAsBoolean() && currentTickStep.ticks() <= 0) {
                    currentTickStep.action().perform();
                    ++currentTickStepIndex;
                    resetTime();
                    if (loopStrategy.shouldLoop(currentTickStepIndex, scriptTickSteps.size())) {
                        resetStepIndex();
                        loopStrategy.onLoop();
                    }
                }
                currentTickStep.decrementTicks();
            }
        });

        currentStepIndex = Math.min(currentStepIndex, scriptSteps.size());
        currentTickStepIndex = Math.min(currentTickStepIndex, scriptTickSteps.size());
    }

    public Script setLoopStrategy(LoopStrategy loopStrategy) {
        this.loopStrategy = loopStrategy;
        return this;
    }

    public boolean isFinished() {
        return currentStepIndex >= scriptSteps.size() && currentTickStepIndex >= scriptTickSteps.size() && !interrupt && loopStrategy.isFinished();
    }

    public static class FiniteLoopStrategy implements LoopStrategy {
        private final int loopCount;
        private int currentLoop;

        public FiniteLoopStrategy(int loopCount) {
            this.loopCount = loopCount - 1;
        }

        @Override
        public boolean shouldLoop(int currentStepIndex, int totalSteps) {
            return currentStepIndex >= totalSteps && currentLoop < loopCount;
        }

        @Override
        public void onLoop() {
            ++currentLoop;
        }

        @Override
        public boolean isFinished() {
            return currentLoop >= loopCount;
        }
    }

    public interface LoopStrategy {
        boolean shouldLoop(int currentStepIndex, int totalSteps);

        void onLoop();

        boolean isFinished();
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static final class ScriptStep implements Comparable<ScriptStep> {
        private int delay;
        private ScriptAction action;
        private BooleanSupplier condition;
        private int priority;

        public ScriptStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
            this.delay = delay;
            this.action = action;
            this.condition = condition;
            this.priority = priority;
        }

        @Override
        public int compareTo(ScriptStep otherStep) {
            return Integer.compare(otherStep.priority(), this.priority());
        }
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static final class ScriptTickStep implements Comparable<ScriptTickStep> {
        private int ticks;
        private ScriptAction action;
        private BooleanSupplier condition;
        private int priority;

        public ScriptTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
            this.ticks = ticks;
            this.action = action;
            this.condition = condition;
            this.priority = priority;
        }

        @Override
        public int compareTo(ScriptTickStep otherStep) {
            return Integer.compare(otherStep.priority(), this.priority());
        }

        public void decrementTicks() {
            this.ticks--;
        }
    }

    public static class InfiniteLoopStrategy implements LoopStrategy {
        @Override
        public boolean shouldLoop(int currentStepIndex, int totalSteps) {
            return currentStepIndex >= totalSteps;
        }

        @Override
        public void onLoop() {
        }

        @Override
        public boolean isFinished() {
            return false;
        }
    }
}
