package fun.aegis.mixins.client.screen.ingame;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.Aegis;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.events.render.DrawEvent;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.features.impl.render.CrossHair;
import fun.aegis.features.impl.render.Hud;

import java.util.ConcurrentModificationException;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements QuickImports {
    @Unique
    private final Hud hud = Hud.getInstance();

    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    protected abstract void renderStatusBars(DrawContext context);

    @Shadow
    protected abstract void renderMountHealth(DrawContext context);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LayeredDrawer;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        blur.setup();
        DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickDelta(false));
        EventManager.callEvent(event);
        Render2D.onRender(context);
        fun.aegis.utils.display.ComboEffect.draw(context);

        boolean debugHudVisible = client.getDebugHud().shouldShowDebugHud();
        boolean tabVisible = client.options.playerListKey.isPressed();

        if (!client.options.hudHidden && !debugHudVisible) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 400.0F);

            Aegis.getInstance().getDraggableRepository().draggable().forEach(draggable -> {
                if (draggable.canDraw(hud, draggable))
                    draggable.startAnimation();
                else
                    draggable.stopAnimation();

                float scale = draggable.getScaleAnimation().getOutput().floatValue();
                if (!draggable.isCloseAnimationFinished()) {
                    draggable.validPosition();
                    try {
                        Calculate.setAlpha(scale, () -> draggable.drawDraggable(context));
                    } catch (ConcurrentModificationException ignored) {
                    }
                }
            });

            context.getMatrices().pop();
        }
    }

    @Inject(method = "renderCrosshair", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"), cancellable = true)
    public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrossHair crossHair = CrossHair.getInstance();
        if (crossHair.isState()) {
            crossHair.onRenderCrossHair();
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Potions")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Скорборд")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Хотбар")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At(value = "HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Хотбар")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At(value = "HEAD"), cancellable = true)
    private void renderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Хотбар")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At(value = "HEAD"), cancellable = true)
    private void renderExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Хотбар")) {
            ci.cancel();
        }
    }

}
