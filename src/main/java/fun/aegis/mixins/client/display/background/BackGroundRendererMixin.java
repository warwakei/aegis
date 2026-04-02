package fun.aegis.mixins.client.display.background;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.events.render.FogEvent;
import fun.aegis.features.impl.render.NoRender;

@Mixin(BackgroundRenderer.class)
public class BackGroundRendererMixin {

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.isState() && noRender.modeSetting.isSelected("Bad Effects")) info.setReturnValue(null);
        if (noRender.isState() && noRender.modeSetting.isSelected("Darkness") && entity instanceof LivingEntity) {
            info.setReturnValue(null);
        }
    }

    @Inject(method = "getFogColor", at = @At(value = "HEAD"), cancellable = true)
    private static void getFogColorHook(Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        FogEvent event = new FogEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Vector4f(ColorAssist.redf(color), ColorAssist.greenf(color), ColorAssist.bluef(color), ColorAssist.alphaf(color)));
        }
    }

    @Inject(method = "applyFog", at = @At(value = "HEAD"), cancellable = true)
    private static void modifyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f vector4f, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.isState() && noRender.modeSetting.isSelected("NoFog")) {
            cir.setReturnValue(new Fog(Float.MAX_VALUE, Float.MAX_VALUE, FogShape.CYLINDER, 0.0f, 0.0f, 0.0f, 0.0f));
            return;
        }
        
        FogEvent event = new FogEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Fog(2.0F, event.getDistance(), FogShape.CYLINDER, ColorAssist.redf(color), ColorAssist.greenf(color), ColorAssist.bluef(color), ColorAssist.alphaf(color)));
        }
    }
}
