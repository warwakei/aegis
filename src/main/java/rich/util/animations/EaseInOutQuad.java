package rich.util.animations;

public class EaseInOutQuad extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
    }
}