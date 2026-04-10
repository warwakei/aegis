package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.GameLeftEvent;
import rich.events.impl.HotBarUpdateEvent;
import rich.events.impl.SetScreenEvent;
import rich.modules.impl.combat.NoInteract;
import rich.modules.impl.render.Hud;
import rich.screens.clickgui.ClickGui;
import rich.screens.menu.MainMenuScreen;
import rich.util.Version;
import rich.util.config.ConfigSystem;
import rich.util.render.font.FontRenderer;
import rich.util.session.SessionChanger;
import rich.util.window.WindowStyle;
import antidaunleak.api.UserProfile;

import static rich.IMinecraft.mc;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Shadow
    public ClientWorld world;

    private static boolean fontsInitialized = false;

    @Shadow
    @Mutable
    private Session session;

    private void setSession(Session newSession) {
        this.session = newSession;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        new Initialization().init();
        SessionChanger.setSessionSetter(this::setSession);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        ConfigSystem configSystem = ConfigSystem.getInstance();
        if (configSystem != null) {
            configSystem.shutdown();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!fontsInitialized && screen != null) {
            try {
                FontRenderer fontRenderer = Initialization.getInstance().getManager().getRenderCore().getFontRenderer();
                if (fontRenderer != null && !fontRenderer.isInitialized()) {
                    fontRenderer.initialize();
                    fontsInitialized = true;
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void redirectTitleScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen && !(screen instanceof MainMenuScreen)) {
            ci.cancel();
            ((MinecraftClient)(Object)this).setScreen(new MainMenuScreen());
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo info) {
        if (world != null) {
            EventManager.callEvent(GameLeftEvent.get());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Hud hud = Hud.getInstance();
        if (hud != null && hud.isState()) {
            if (Initialization.getInstance() != null
                    && Initialization.getInstance().getManager() != null
                    && Initialization.getInstance().getManager().getHudManager() != null) {
                Initialization.getInstance().getManager().getHudManager().tick();
            }
        }
    }

    @Inject(method = "setScreen", at = @At(value = "HEAD"), cancellable = true)
    public void setScreenHook(Screen screen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (client.currentScreen instanceof ClickGui clickGui) {
            if (clickGui.isClosing() && screen == null) {
                ci.cancel();
                return;
            }
        }

        SetScreenEvent event = new SetScreenEvent(screen);
        EventManager.callEvent(event);

        Initialization instance = Initialization.getInstance();

        Screen eventScreen = event.getScreen();
        if (screen != eventScreen) {
            mc.setScreen(eventScreen);
            ci.cancel();
        }
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        rich.util.TitleManager titleManager = rich.util.TitleManager.getInstance();
        String customTitle = titleManager.getWindowTitle();
        if (customTitle != null) {
            cir.setReturnValue(customTitle);
        }
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void handleInputEventsHook(CallbackInfo ci) {
        HotBarUpdateEvent event = new HotBarUpdateEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"), cancellable = true)
    public void doItemUseHook(CallbackInfo ci) {
        if (NoInteract.getInstance().isState()) {
            for (Hand hand : Hand.values()) {
                if (player.getStackInHand(hand).isEmpty()) continue;
                ActionResult result = interactionManager.interactItem(player, hand);
                if (result.isAccepted()) {
                    if (result instanceof ActionResult.Success success && success.swingSource().equals(ActionResult.SwingSource.CLIENT)) {
                        gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                        player.swingHand(hand);
                    }
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void applyDarkMode(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        WindowStyle.setDarkMode(client.getWindow().getHandle());
    }
}