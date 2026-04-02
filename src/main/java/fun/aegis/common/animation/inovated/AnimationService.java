package fun.aegis.common.animation.inovated;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

@UtilityClass
public class AnimationService {

    public double delta;

    public float animation(float animation, float target, float speedTarget) {
        float dif = (target - animation) / Math.max((float) MinecraftClient.getInstance().getCurrentFps(), 5) * 15;

        if (dif > 0) {
            dif = Math.max(speedTarget, dif);
            dif = Math.min(target - animation, dif);
        } else if (dif < 0) {
            dif = Math.min(-speedTarget, dif);
            dif = Math.max(target - animation, dif);
        }
        return animation + dif;
    }

    public float animationSpeed(float animation, float target, float speedTarget) {
        float dif = (target - animation) / Math.max((float) MinecraftClient.getInstance().getCurrentFps(), 5) * 15;

        if (dif > 0) {
            dif = speedTarget;
            dif = Math.min(target - animation, dif);
        } else if (dif < 0) {
            dif = -speedTarget;
            dif = Math.max(target - animation, dif);
        }
        return animation + dif;
    }

    public float getAnimationState(float animation, float finalState, float speed) {
        final float add = (float) (delta * (speed / 1000f));
        if (animation < finalState) {
            if (animation + add < finalState) {
                animation += add;
            } else {
                animation = finalState;
            }
        } else if (animation - add > finalState) {
            animation -= add;
        } else {
            animation = finalState;
        }
        return animation;
    }

    public double interpolateAnimation(double start, double end, double step) {
        return start + (end - start) * step;
    }

    public float move(float from, float to, float minstep, float maxstep, float factor) {

        float f = (to - from) * MathHelper.clamp(factor,0,1);

        if (f < 0)
            f = MathHelper.clamp(f, -maxstep, -minstep);
        else
            f = MathHelper.clamp(f, minstep, maxstep);

        if(Math.abs(f) > Math.abs(to - from))
            return to;

        return from + f;
    }

}
