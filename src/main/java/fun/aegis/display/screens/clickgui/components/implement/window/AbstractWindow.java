package fun.aegis.display.screens.clickgui.components.implement.window;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import fun.aegis.common.animation.Easy.EaseBackIn;
import fun.aegis.common.animation.Easy.Direction;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;

public abstract class AbstractWindow extends AbstractComponent {
    protected boolean dragging;
    private boolean draggable;
    protected int dragX, dragY;
    @Getter
    private final EaseBackIn scaleAnimation = new EaseBackIn(320, 1, 1.5f, Direction.FORWARDS);

    public AbstractWindow draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    @Override
    public AbstractWindow size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public AbstractWindow position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0 && draggable) {
            dragging = true;
            dragX = (int) (x - mouseX);
            dragY = (int) (y - mouseY);
            return true;
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (dragging && draggable) {
            x = mouseX + dragX;
            y = mouseY + dragY;
        }
        float scale = (float) scaleAnimation.getOutput();
        Calculate.scale(context.getMatrices(), x + width / 2, y + height / 2, scale, () -> drawWindow(context, mouseX, mouseY, delta));
    }

    protected abstract void drawWindow(DrawContext context, int mouseX, int mouseY, float delta);

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return true;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void startCloseAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    public boolean isCloseAnimationFinished() {
        return scaleAnimation.finished(Direction.BACKWARDS);
    }
}
