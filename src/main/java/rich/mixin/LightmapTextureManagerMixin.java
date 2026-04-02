package rich.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.Initialization;
import rich.modules.impl.render.FullBright;
import rich.modules.impl.render.NoRender;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float leet$getValue(Double instance) {
        if (Initialization.getInstance().getManager().getModuleProvider().get(FullBright.class).isState()) {
            return 200F;
        }
        return instance.floatValue();
    }

    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void removeDarknessEffect(CallbackInfoReturnable<Float> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(0.0F);
        }
    }
}
