package rich.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.Initialization;
import rich.client.draggables.Drag;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        Drag.onDraw(context, mouseX, mouseY, deltaTicks, true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int button = click.button();

        if (Initialization.getInstance() != null && Initialization.getInstance().getManager() != null
                && Initialization.getInstance().getManager().getHudManager() != null) {
            if (Initialization.getInstance().getManager().getHudManager().mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }

        Drag.onMouseClick(click);
        if (Drag.isDragging()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        Drag.onMouseRelease(click);

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public void removed() {
        Drag.resetDragging();
        super.removed();
    }

    @Override
    public void close() {
        Drag.resetDragging();
        super.close();
    }
}