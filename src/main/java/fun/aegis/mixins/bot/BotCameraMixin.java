package fun.aegis.mixins.bot;

import fun.aegis.features.bot.BotCameraManager;
import fun.aegis.features.bot.BotManager;
import fun.aegis.features.bot.Bot;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.events.render.CameraPositionEvent;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.utils.client.managers.event.EventManager;

@Mixin(Camera.class)
public abstract class BotCameraMixin {

    @Shadow private Vec3d pos;
    @Shadow @Final private BlockPos.Mutable blockPos;
    @Shadow private float yaw;
    @Shadow private float pitch;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void onCameraUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        BotCameraManager cameraManager = BotCameraManager.getInstance();
        if (!cameraManager.isViewingBot()) return;

        int botId = cameraManager.getActiveBotId();
        Bot bot = BotManager.getInstance().getBot(botId);
        if (bot == null || !bot.isConnected()) {
            cameraManager.stopViewing();
            return;
        }

        TurnsConnection.INSTANCE.setRotation(new Turns(yaw, pitch));
    }

    @Inject(method = "setPos(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void onSetPos(Vec3d newPos, CallbackInfo ci) {
        BotCameraManager cameraManager = BotCameraManager.getInstance();
        if (!cameraManager.isViewingBot()) return;

        CameraPositionEvent event = new CameraPositionEvent(newPos);
        EventManager.callEvent(event);
        this.pos = event.getPos();
        blockPos.set(pos.x, pos.y, pos.z);
        ci.cancel();
    }
}
