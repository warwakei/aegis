package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.IMinecraft;
import rich.events.api.EventManager;
import rich.events.impl.ChatEvent;
import rich.events.impl.GameLeftEvent;
import rich.events.impl.WorldChangeEvent;
import rich.modules.impl.render.Particles;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements IMinecraft {

    @Shadow
    private ClientWorld world;

    @Shadow
    private static ItemStack getActiveDeathProtector(PlayerEntity player) {
        return null;
    }

    @Unique
    private boolean worldNotNull;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        ChatEvent event = new ChatEvent(message);
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
        worldNotNull = world != null;
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
        if (worldNotNull) {
            EventManager.callEvent(GameLeftEvent.get());
        }
    }

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        EventManager.callEvent(WorldChangeEvent.get());
    }

    @Inject(method = "onPlayerRespawn", at = @At("RETURN"))
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        EventManager.callEvent(WorldChangeEvent.get());
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"), cancellable = true)
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() == 35) {
            Entity entity = packet.getEntity(mc.world);
            if (entity != null) {
                Particles particlesMod = Particles.getInstance();
                if (particlesMod != null && particlesMod.isState() && particlesMod.triggers.isSelected("Тотем")) {
                    particlesMod.onTotemPop(entity);

                    mc.world.playSoundClient(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_TOTEM_USE, entity.getSoundCategory(), 1.0F, 1.0F, false);

                    if (entity == mc.player) {
                        mc.gameRenderer.showFloatingItem(getActiveDeathProtector(mc.player));
                    }

                    ci.cancel();
                }
            }
        }
    }
}