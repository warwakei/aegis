package rich.util.animations;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Easings {
    // Basic
    public final Easing LINEAR = (value) -> value;

    // Quad
    public final Easing QUAD_IN = (value) -> value * value;
    public final Easing QUAD_OUT = (value) -> 1.0 - Math.pow(1.0 - value, 2);
    public final Easing QUAD_IN_OUT = (value) -> value < 0.5 ? 2 * value * value : 1 - Math.pow(-2 * value + 2, 2) / 2;

    // Cubic
    public final Easing CUBIC_IN = (value) -> value * value * value;
    public final Easing CUBIC_OUT = (value) -> 1.0 - Math.pow(1.0 - value, 3);
    public final Easing CUBIC_IN_OUT = (value) -> value < 0.5 ? 4 * value * value * value : 1 - Math.pow(-2 * value + 2, 3) / 2;

    // Quart
    public final Easing QUART_IN = (value) -> value * value * value * value;
    public final Easing QUART_OUT = (value) -> 1.0 - Math.pow(1.0 - value, 4);
    public final Easing QUART_IN_OUT = (value) -> value < 0.5 ? 8 * value * value * value * value : 1 - Math.pow(-2 * value + 2, 4) / 2;

    // Expo
    public final Easing EXPO_IN = (value) -> value == 0 ? 0 : Math.pow(2.0, 10.0 * value - 10.0);
    public final Easing EXPO_OUT = (value) -> value == 1 ? 1 : 1.0 - Math.pow(2.0, -10.0 * value);
    public final Easing EXPO_IN_OUT = (value) -> {
        if (value == 0 || value == 1) return value;
        return value < 0.5
                ? Math.pow(2.0, 20.0 * value - 10.0) / 2.0
                : (2.0 - Math.pow(2.0, -20.0 * value + 10.0)) / 2.0;
    };

    // Sine
    public final Easing SINE_IN = (value) -> 1 - Math.cos((value * Math.PI) / 2);
    public final Easing SINE_OUT = (value) -> Math.sin((value * Math.PI) / 2);
    public final Easing SINE_IN_OUT = (value) -> -(Math.cos(Math.PI * value) - 1) / 2;

    // Back (overshoot)
    public final Easing BACK_IN = (value) -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return c3 * value * value * value - c1 * value * value;
    };
    public final Easing BACK_OUT = (value) -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1.0 + c3 * Math.pow(value - 1.0, 3.0) + c1 * Math.pow(value - 1.0, 2.0);
    };
    public final Easing BACK_IN_OUT = (value) -> {
        double c1 = 1.70158;
        double c2 = c1 * 1.525;
        return value < 0.5
                ? (Math.pow(2 * value, 2) * ((c2 + 1) * 2 * value - c2)) / 2
                : (Math.pow(2 * value - 2, 2) * ((c2 + 1) * (value * 2 - 2) + c2) + 2) / 2;
    };

    // Elastic (bouncy)
    public final Easing ELASTIC_OUT = (value) -> {
        if (value == 0 || value == 1) return value;
        return Math.pow(2, -10 * value) * Math.sin((value * 10 - 0.75) * (2 * Math.PI) / 3) + 1;
    };
    public final Easing ELASTIC_IN = (value) -> {
        if (value == 0 || value == 1) return value;
        return -Math.pow(2, 10 * value - 10) * Math.sin((value * 10 - 10.75) * (2 * Math.PI) / 3);
    };

    // Bounce
    public final Easing BOUNCE_OUT = (value) -> {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (value < 1 / d1) {
            return n1 * value * value;
        } else if (value < 2 / d1) {
            return n1 * (value -= 1.5 / d1) * value + 0.75;
        } else if (value < 2.5 / d1) {
            return n1 * (value -= 2.25 / d1) * value + 0.9375;
        } else {
            return n1 * (value -= 2.625 / d1) * value + 0.984375;
        }
    };
    public final Easing BOUNCE_IN = (value) -> 1 - BOUNCE_OUT.ease(1 - value);

    // Circ
    public final Easing CIRC_IN = (value) -> 1 - Math.sqrt(1 - Math.pow(value, 2));
    public final Easing CIRC_OUT = (value) -> Math.sqrt(1 - Math.pow(value - 1, 2));

    // Spring (fast start + slight overshoot)
    public final Easing SPRING = (value) -> {
        double c3 = 1.5; // tension
        double c4 = 0.3; // damping
        return 1 - Math.exp(-c3 * value) * Math.cos(value * Math.PI * c4);
    };

    // Smooth step (very gentle, good for subtle UI)
    public final Easing SMOOTH_STEP = (value) -> value * value * (3 - 2 * value);
}