package fun.aegis.common.animation.implement;

import fun.aegis.common.animation.Animation;

public class InOutCirc extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;

        return x < 0.5
                ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2
                : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2;
    }
}
