package rich.util.render.layout;

/**
 * Единый источник геометрии: один объект используют и рендер, и хитбоксы,
 * чтобы координаты не разъезжались между mouseClicked/render/scroll.
 */
public record Bounds(float x, float y, float w, float h) {

    public static Bounds of(float x, float y, float w, float h) {
        return new Bounds(x, y, w, h);
    }

    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= right() && my >= y && my <= bottom();
    }

    public Bounds offset(float dx, float dy) {
        return new Bounds(x + dx, y + dy, w, h);
    }

    public Bounds inset(float amount) {
        return inset(amount, amount);
    }

    public Bounds inset(float horizontal, float vertical) {
        return new Bounds(x + horizontal, y + vertical, w - horizontal * 2, h - vertical * 2);
    }

    public Bounds withSize(float newW, float newH) {
        return new Bounds(x, y, newW, newH);
    }
}