package fun.aegis.mixins.player.entity;

import fun.aegis.utils.features.aura.warp.TurnsConnection;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInteractItemC2SPacket.class)
public class PlayerInteractItemC2SPacketMixin {

    @Mutable
    @Shadow
    @Final
    private float yaw;

    @Mutable
    @Shadow
    @Final
    private float pitch;

    @Inject(method = "<init>(Lnet/minecraft/util/Hand;IFF)V", at = @At("RETURN"))
    private void modifyRotation(Hand hand, int sequence, float yaw, float pitch, CallbackInfo ci) {

        this.yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        this.pitch = TurnsConnection.INSTANCE.getRotation().getPitch();
    }

}
