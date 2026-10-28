package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.screens.loading.Loading;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    @Unique
    private Loading loadingScreen;

    @Unique
    private boolean resourcesMarkedComplete = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (this.reloading) {
            return;
        }

        ci.cancel();

        if (loadingScreen == null) {
            loadingScreen = new Loading();
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long currentTime = Util.getMeasuringTimeMs();

        if (this.reloadStartTime == -1L) {
            this.reloadStartTime = currentTime;
        }

        float reloadProgress = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + reloadProgress * 0.05F, 0.0F, 1.0F);

        loadingScreen.setProgress(this.progress);

        if (this.reload.isComplete() && !resourcesMarkedComplete) {
            resourcesMarkedComplete = true;
            loadingScreen.markComplete();
            if (this.reloadCompleteTime == -1L) {
                this.reloadCompleteTime = currentTime;
            }
        }

        loadingScreen.render(width, height, 1.0f);

        if (loadingScreen.isReadyToClose()) {
            this.client.setOverlay((Overlay) null);
            loadingScreen.reset();
            loadingScreen = null;
            resourcesMarkedComplete = false;
        }
    }
}