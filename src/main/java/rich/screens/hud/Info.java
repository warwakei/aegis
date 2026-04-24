package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;

public class Info extends AbstractHudElement {

    public Info() {
        super("Info", 10, 0, 200, 24, false);
        startAnimation();
    }

    @Override
    public boolean visible() {
        // Legacy element kept for config compatibility; functionality moved to `Coords` and `BPS`.
        return false;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        // no-op
    }
}
