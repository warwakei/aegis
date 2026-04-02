package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.api.types.EventType;
import rich.events.impl.*;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.util.move.MoveUtil;

import static rich.IMinecraft.mc;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    @Shadow
    protected abstract void autoJump(float dx, float dz);

    @Shadow
    public abstract boolean isUsingItem();

    @Final
    @Shadow
    protected MinecraftClient client;

    @Shadow
    public Input input;

    private double prevX = 0.0;
    private double prevZ = 0.0;
    private float prevBodyYaw = 0.0f;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo info) {
        if (client.player != null && client.world != null) {
            EventManager.callEvent(new TickEvent());
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick()V", shift = At.Shift.AFTER))
    private void onInputTick(CallbackInfo ci) {
        if (mc.player == null)
            return;
        PlayerTravelEvent event = new PlayerTravelEvent(Vec3d.ZERO, false);
        EventManager.callEvent(event);
    }

    @Redirect(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;", ordinal = 1))
    private Vec2f cancelItemSlowdown(Vec2f vec2f, float multiplier) {
        UsingItemEvent event = new UsingItemEvent(EventType.ON);
        EventManager.callEvent(event);

        if (event.isCancelled() && this.isUsingItem() && !this.hasVehicle()) {
            return vec2f.multiply(1.0F);
        }

        return vec2f.multiply(multiplier);
    }

    @Inject(method = "closeHandledScreen", at = @At(value = "HEAD"), cancellable = true)
    private void closeHandledScreenHook(CallbackInfo info) {
        CloseScreenEvent event = new CloseScreenEvent(client.currentScreen);
        EventManager.callEvent(event);
        if (event.isCancelled())
            info.cancel();
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushEvent event = new PushEvent(PushEvent.Type.BLOCK);
        EventManager.callEvent(event);
        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement);
        EventManager.callEvent(event);
        double d = this.getX();
        double e = this.getZ();
        super.move(movementType, event.getMovement());
        this.autoJump((float) (this.getX() - d), (float) (this.getZ() - e));
        ci.cancel();
    }

    @ModifyExpressionValue(method = { "sendMovementPackets",
            "tick" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float hookSilentRotationYaw(float original) {
        if (mc.player != null && AngleConnection.INSTANCE.getRotation() != null) {
            float currentYaw = AngleConnection.INSTANCE.getRotation().getYaw();
            float newBodyYaw = MoveUtil.calculateBodyYaw(
                    currentYaw,
                    prevBodyYaw,
                    prevX,
                    prevZ,
                    mc.player.getX(),
                    mc.player.getZ(),
                    mc.player.handSwingProgress);

            prevBodyYaw = newBodyYaw;
            prevX = mc.player.getX();
            prevZ = mc.player.getZ();

            mc.player.setBodyYaw(newBodyYaw);
            return currentYaw;
        }

        return original;
    }

    @ModifyExpressionValue(method = { "sendMovementPackets",
            "tick" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float hookSilentRotationPitch(float original) {
        if (AngleConnection.INSTANCE.getRotation() != null) {
            return AngleConnection.INSTANCE.getRotation().getPitch();
        }
        return original;
    }
}