package fun.aegis.display.screens.clickgui.newgui.animation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Animation {
    private int duration;
    private float value;
    private float targetValue;
    private Easing easing;
    private long startTime;
    private float startValue;
    private boolean animating = false;

    public Animation(int duration, float initialValue, Easing easing) {
        this.duration = duration;
        this.value = initialValue;
        this.targetValue = initialValue;
        this.easing = easing;
        this.startValue = initialValue;
    }

    public Animation(int duration, Easing easing) {
        this(duration, 0f, easing);
    }

    public void animateTo(float target) {
        if (this.targetValue != target) {
            this.startValue = this.value;
            this.targetValue = target;
            this.startTime = System.currentTimeMillis();
            this.animating = true;
        }
    }

    public float update() {
        if (!animating) return value;
        
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1f, (float) elapsed / duration);
        float easedProgress = easing.apply(progress);
        
        value = startValue + (targetValue - startValue) * easedProgress;
        
        if (progress >= 1f) {
            value = targetValue;
            animating = false;
        }
        
        return value;
    }

    public float update(float target) {
        animateTo(target);
        return update();
    }

    public void setValue(float value) {
        this.value = value;
        this.targetValue = value;
        this.startValue = value;
        this.animating = false;
    }

    public void reset(float value) {
        setValue(value);
    }

    public boolean isDone() {
        return !animating;
    }

    public void setTargetValue(float target) {
        animateTo(target);
    }
}
