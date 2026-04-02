package fun.aegis.mixins.client.screen.ingame;

import fun.aegis.features.impl.misc.SelfDestruct;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
    @Redirect(
            method = "getLeftText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getYaw()F"
            )
    )
    private float redirectYaw(Entity entity) {
        if (SelfDestruct.unhooked) return entity.getYaw();
        return TurnsConnection.INSTANCE.getRotation().getYaw();
    }

    @Redirect(
            method = "getLeftText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getPitch()F"
            )
    )
    private float redirectPitch(Entity entity) {
        if (SelfDestruct.unhooked) return entity.getPitch();
        return TurnsConnection.INSTANCE.getRotation().getPitch();
    }
}
