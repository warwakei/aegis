package rich.util.render.font;

public class Glyph {

    public final int id;
    public final float x;
    public final float y;
    public final float width;
    public final float height;
    public final float xOffset;
    public final float yOffset;
    public final float xAdvance;

    public final float u0;
    public final float v0;
    public final float u1;
    public final float v1;

    public Glyph(int id, float x, float y, float width, float height,
                 float xOffset, float yOffset, float xAdvance,
                 float atlasWidth, float atlasHeight) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.xAdvance = xAdvance;

        this.u0 = x / atlasWidth;
        this.v0 = y / atlasHeight;
        this.u1 = (x + width) / atlasWidth;
        this.v1 = (y + height) / atlasHeight;
    }
}