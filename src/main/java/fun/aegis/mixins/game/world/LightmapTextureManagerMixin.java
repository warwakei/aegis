package fun.aegis.mixins.game.world;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.aegis.features.impl.render.NoRender;
import fun.aegis.features.impl.render.WorldTweaks;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object injectXRayFullBright(Object original) {
        WorldTweaks tweaks = WorldTweaks.getInstance();
        if (tweaks.isState() && tweaks.modeSetting.isSelected("Bright")) {
            return Math.max((double) original, tweaks.brightSetting.getValue() * 10);
        }
        return original;
    }

    @Inject(method = "getDarknessFactor", at = @At("HEAD"), cancellable = true)
    private void removeDarkness(float delta, CallbackInfoReturnable<Float> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void removeDarknessEffect(CallbackInfoReturnable<Float> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/LightmapTextureManager;getDarknessFactor(F)F"), cancellable = true)
    private void cancelDarknessInUpdate(float delta, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            return;
        }
    }
}
