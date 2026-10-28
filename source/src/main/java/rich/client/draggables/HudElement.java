package rich.client.draggables;

import net.minecraft.client.gui.DrawContext;
import rich.events.impl.PacketEvent;

public interface HudElement {
    void render(DrawContext context, float tickDelta);

    void tick();

    default void onPacket(PacketEvent e) {}

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getName();

    int getX();

    int getY();

    void setX(int x);

    void setY(int y);

    int getWidth();

    int getHeight();

    void setWidth(int width);

    void setHeight(int height);

    default float getRoundingRadius() {
        return 4.0f;
    }

    default boolean visible() {
        return true;
    }

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}