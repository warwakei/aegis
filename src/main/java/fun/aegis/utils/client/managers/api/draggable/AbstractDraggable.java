package fun.aegis.utils.client.managers.api.draggable;

import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.interfaces.QuickLogger;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.Aegis;
import fun.aegis.events.container.SetScreenEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.features.impl.render.Hud;

@Setter
@Getter
public abstract class AbstractDraggable implements Draggable, QuickImports, QuickLogger {
    private String name;
    private int x, y, width, height;
    private boolean dragging, canDrag;
    private int dragX, dragY;

    public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.canDrag = canDrag;
    }

    public final Animation scaleAnimation = new Decelerate().setValue(1).setMs(200);

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {}

    @Override
    public void packet(PacketEvent e) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!dragging) {
            dragX = 0;
            dragY = 0;
        }
        Hud hud = Hud.getInstance();
        float mouseDragX = mouseX + dragX;
        float mouseDragY = mouseY + dragY;
        int windowWidth = window.getScaledWidth();
        int windowHeight = window.getScaledHeight();
        int radius = 3;

        if (dragging) {
            this.x = (int) Math.max(0, Math.min(mouseDragX, windowWidth - width));
            this.y = (int) Math.max(0, Math.min(mouseDragY, windowHeight - height));
        }

        for (AbstractDraggable drag : Aegis.getInstance().getDraggableRepository().draggable()) {
            if (!drag.canDraw(hud, drag)) continue;
            if (!drag.canDrag) continue;
            if (drag == this) continue;
            int x1 = drag.x + drag.width + radius;
            int x2 = drag.x - width - radius;
            int y1 = drag.y + drag.height + radius;
            int y2 = drag.y - height - radius;
            int y3 = drag.y;

            if (Math.abs(x1 - mouseDragX) <= radius) {
                drawRect(x1 - 1.5F, 0, 1, windowHeight);
                this.x = x1;
            }
            if (Math.abs(x2 - mouseDragX) <= radius) {
                drawRect(x2 + width + 1, 0, 1, windowHeight);
                this.x = x2;
            }
            if (Math.abs(y1 - mouseDragY) <= radius) {
                drawRect(0, y1 - 1.5F, windowWidth, 1);
                this.y = y1;
            }
            if (Math.abs(y2 - mouseDragY) <= radius) {
                drawRect(0, y2 + height + 1, windowWidth, 1);
                this.y = y2;
            }
            if (Math.abs(y3 - mouseDragY) <= radius) {
                drawRect(0, y3 - 1.5F, windowWidth, 1);
                this.y = y3;
            }
        }

        if (Math.abs(x + (width - windowWidth) / 2) <= radius) {
            drawRect((float) windowWidth / 2 - 0.5F, 0, 1, windowHeight);
            this.x = (windowWidth - width) / 2;
        }
        if (Math.abs(y + (height - windowHeight) / 2) <= radius) {
            drawRect(0, (float) windowHeight / 2 - 0.5F, windowWidth, 1);
            this.y = (windowHeight - height) / 2;
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (PlayerInteractionHelper.isChat(e.getScreen())) {
            dragging = false;
            dragX = 0;
            dragY = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0 && canDrag) {
            dragging = true;
            dragX = x - (int) mouseX;
            dragY = y - (int) mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        dragX = 0;
        dragY = 0;
        return true;
    }

    public abstract void drawDraggable(DrawContext context);

    public void drawRect(float x, float y, float width, float height) {
        Render2D.drawQuad(x, y, width, height, ColorAssist.getText(0.5F));
    }

    public void stopAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    public void startAnimation() {
        scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void validPosition() {
        if (x + width > window.getScaledWidth()) x = window.getScaledWidth() - width;
        if (y + height > window.getScaledHeight()) y = window.getScaledHeight() - height;
        if (y < 0) y = 0;
        if (x < 0) x = 0;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isCloseAnimationFinished() {
        return scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        return hud.isState() && hud.interfaceSettings.isSelected(draggable.getName()) && visible();
    }
}
