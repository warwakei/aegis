package fun.aegis.mixins.game.render;

import fun.aegis.features.impl.misc.SelfDestruct;
import fun.aegis.features.impl.render.BetterMinecraft;
import fun.aegis.utils.display.widget.SliderRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin extends ClickableWidget {

    @Shadow protected double value;

    @Unique
    private static final SliderRender RENDER = new SliderRender();

    public SliderWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;
        if (BetterMinecraft.getInstance().isState() && BetterMinecraft.getInstance().getBetterButton().isValue()) {

            ci.cancel();
            RENDER.renderSlider(
                    context,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    this.value,
                    this.active,
                    getMessage() != null ? getMessage().getString() : ""
            );
        }
    }
}
