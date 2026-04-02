package fun.aegis.mixins.player.entity;

import fun.aegis.features.impl.render.santa.SantaHatFeatureRenderer;
import fun.aegis.mixins.accessor.LivingEntityRendererAccessor;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
   
   @Inject(
      method = "<init>",
      at = @At("RETURN")
   )
   private void onInit(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
      PlayerEntityRenderer self = (PlayerEntityRenderer)(Object)this;
      ((LivingEntityRendererAccessor) self).invokeAddFeature(new SantaHatFeatureRenderer(self));
   }
}
