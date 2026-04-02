package rich.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.FovEvent;
import rich.events.impl.HotBarScrollEvent;
import rich.events.impl.KeyEvent;
import rich.events.impl.MouseRotationEvent;
import rich.screens.clickgui.ClickGui;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    private boolean cursorLocked;

    @Shadow
    private double x;

    @Shadow
    private double y;

    @Shadow
    private double cursorDeltaX;

    @Shadow
    private double cursorDeltaY;

    @Shadow
    private boolean hasResolutionChanged;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseButtonHook(long window, MouseInput input, int action, CallbackInfo ci) {
        if (input.button() != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.MOUSE, input.button(), action));
        }
    }

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        HotBarScrollEvent event = new HotBarScrollEvent(horizontal, vertical);
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void onLockCursor(CallbackInfo ci) {
        if (client.currentScreen instanceof ClickGui clickGui) {
            if (clickGui.isClosing()) {
                this.cursorLocked = true;
                this.cursorDeltaX = 0;
                this.cursorDeltaY = 0;
                this.x = client.getWindow().getWidth() / 2.0;
                this.y = client.getWindow().getHeight() / 2.0;
                this.hasResolutionChanged = true;
                ci.cancel();
            }
        }
    }

    @Inject(method = "updateMouse", at = @At(value = "HEAD"))
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        FovEvent event = new FovEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            double slowdown = (double) event.getFov() / client.options.getFov().getValue();
            this.cursorDeltaX *= slowdown;
            this.cursorDeltaY *= slowdown;
        }
    }

    @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
    private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        MouseRotationEvent event = new MouseRotationEvent((float) cursorDeltaX, (float) cursorDeltaY);
        EventManager.callEvent(event);
        if (event.isCancelled()) return false;
        instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
        return false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (client.currentScreen instanceof ClickGui clickGui) {
            if (clickGui.isClosing() && !this.cursorLocked) {
                this.cursorLocked = true;
                this.cursorDeltaX = 0;
                this.cursorDeltaY = 0;
                this.hasResolutionChanged = true;
            }
        }
    }
}