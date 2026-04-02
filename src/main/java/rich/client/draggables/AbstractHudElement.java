package rich.client.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import rich.util.animations.Animation;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;

public abstract class AbstractHudElement implements HudElement {

    protected int x, y, width, height;
    protected String name;
    protected boolean enabled = true;
    protected boolean draggable = true;

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected final Animation scaleAnimation = new Decelerate().setMs(300).setValue(1);
    protected float lastTickDelta = 0f;

    public AbstractHudElement(String name, int x, int y, int width, int height, boolean draggable) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.draggable = draggable;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (!visible()) return;

        this.lastTickDelta = tickDelta;
        scaleAnimation.update();

        int alpha = (int) (scaleAnimation.getOutput().floatValue() * 255);
        if (alpha <= 0) return;

        drawDraggable(context, alpha);
    }

    public abstract void drawDraggable(DrawContext context, int alpha);

    @Override
    public void tick() {
    }

    @Override
    public boolean visible() {
        return true;
    }

    public void startAnimation() {
        scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void stopAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    protected boolean isChat(Screen screen) {
        return screen instanceof ChatScreen;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public float getLastTickDelta() {
        return lastTickDelta;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }
}