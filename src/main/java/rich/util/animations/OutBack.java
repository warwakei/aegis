package rich.util.animations;

public class OutBack extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;

        double c1 = 1.70158;
        double c3 = c1 + 1;

        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }
}
