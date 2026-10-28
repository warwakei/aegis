package rich.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.IMinecraft;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.DrawEvent;
import rich.events.impl.HotbarItemRenderEvent;
import rich.modules.impl.render.Hud;
import rich.modules.impl.render.NoRender;
import rich.screens.clickgui.ClickGui;
import rich.util.render.Render2D;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements IMinecraft {

    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private int richCurrentHotbarIndex = 0;

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void onRenderHotbarStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        richCurrentHotbarIndex = 0;
    }

    @WrapOperation(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V"
            )
    )
    private void onRenderHotbarItem(InGameHud instance, DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original) {
        int hotbarIndex = richCurrentHotbarIndex;

        if (richCurrentHotbarIndex < 9) {
            richCurrentHotbarIndex++;
        }

        HotbarItemRenderEvent event = new HotbarItemRenderEvent(stack, hotbarIndex);
        EventManager.callEvent(event);
        original.call(instance, context, x, y, tickCounter, player, event.getStack(), seed);
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderNauseaOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Nausea")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Scoreboard")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("BossBar")) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderCustomHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.options.hudHidden) return;
        if (client.world == null || client.player == null) return;
        if (client.getOverlay() != null) return;

        Screen screen = client.currentScreen;

        if (isLoadingScreen(screen)) return;

        context.createNewRootLayer();
        Render2D.beginOverlay();
        context.getMatrices().pushMatrix();

        DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickProgress(false));
        EventManager.callEvent(event);

        context.getMatrices().popMatrix();

        if (shouldRenderHud(screen)) {
            int mouseX = (int) client.mouse.getScaledX(client.getWindow());
            int mouseY = (int) client.mouse.getScaledY(client.getWindow());
            float tickDelta = tickCounter.getTickProgress(false);

            Hud hud = Hud.getInstance();
            if (hud != null && hud.isState() && Initialization.getInstance() != null
                    && Initialization.getInstance().getManager() != null
                    && Initialization.getInstance().getManager().getHudManager() != null) {
                Initialization.getInstance().getManager().getHudManager().render(context, tickDelta, mouseX, mouseY);
            }
        }

        Render2D.endOverlay();
    }

    @Unique
    private boolean shouldRenderHud(Screen screen) {
        if (screen == null) return true;
        if (screen instanceof ClickGui) return false;
        if (screen instanceof ChatScreen) return false;
        if (isLoadingScreen(screen)) return false;
        return true;
    }

    @Unique
    private boolean isLoadingScreen(Screen screen) {
        if (screen == null) return false;
        String className = screen.getClass().getSimpleName().toLowerCase();
        String fullName = screen.getClass().getName().toLowerCase();
        if (className.contains("loading")) return true;
        if (className.contains("progress")) return true;
        if (className.contains("connecting")) return true;
        if (className.contains("downloading")) return true;
        if (className.contains("terrain")) return true;
        if (className.contains("generating")) return true;
        if (className.contains("saving")) return true;
        if (className.contains("reload")) return true;
        if (className.contains("resource")) return true;
        if (className.contains("pack")) return true;
        if (fullName.contains("mojang")) return true;
        return false;
    }
}