package rich.util.animations;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}