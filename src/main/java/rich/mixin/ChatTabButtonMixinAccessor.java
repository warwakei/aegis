package rich.modules.impl.combat.macetarget;

import net.minecraft.client.gui.Click;

public interface ChatTabButtonMixinAccessor {
    void onDragEnd(Click click);
    void onCursorMove(double mouseX, double mouseY);
}
