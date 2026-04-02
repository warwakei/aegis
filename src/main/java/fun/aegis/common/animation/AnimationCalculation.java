package fun.aegis.common.animation;

public interface AnimationCalculation {
    default double calculation(double value) {
        return 0;
    }
}
