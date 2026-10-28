package rich.util.render.font;

import rich.Initialization;

public class Font {
    private final String name;

    public Font(String name) {
        this.name = name;
    }

    public void draw(String text, float x, float y, float size, int color) {
        Initialization.getInstance().getManager().getRenderCore().getFontRenderer().drawText(name, text, x, y, size, color);
    }

    public void drawCentered(String text, float x, float y, float size, int color) {
        Initialization.getInstance().getManager().getRenderCore().getFontRenderer().drawCenteredText(name, text, x, y, size, color);
    }

    public float getWidth(String text, float size) {
        return Initialization.getInstance().getManager().getRenderCore().getFontRenderer().getTextWidth(name, text, size);
    }

    public float getHeight(float size) {
        return Initialization.getInstance().getManager().getRenderCore().getFontRenderer().getLineHeight(name, size);
    }

    public String getName() {
        return name;
    }
}