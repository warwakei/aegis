package fun.aegis.mixins.client.screen.mainmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingCubeMapRenderer.class)
public class RotatingCubeMapRendererMixin {
    private static final CubeMapRenderer CUSTOM_PANORAMA_RENDERER = new CubeMapRenderer(Identifier.of("minecraft", "panorama/panorama"));
    private static float customPitch = 0.0F;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderCustomPanorama(DrawContext context, int width, int height, float alpha, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        float f = client.getRenderTickCounter().getLastDuration();
        float g = (float)((double)f * (Double)client.options.getPanoramaSpeed().getValue());
        customPitch = wrapOnce(customPitch + g * 0.1F, 360.0F);
        context.draw();
        CUSTOM_PANORAMA_RENDERER.draw(client, 10.0F, -customPitch, alpha);
        context.draw();
        ci.cancel();
    }

    private static float wrapOnce(float a, float b) {
        return a > b ? a - b : a;
    }
}
