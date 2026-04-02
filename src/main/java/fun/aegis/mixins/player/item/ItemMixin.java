package fun.aegis.mixins.player.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

@Mixin(Item.class)
public class ItemMixin {

    @Redirect(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d raycastHook(PlayerEntity player, float pitch, float yaw) {
        return TurnsConnection.INSTANCE.getRotation().toVector();
    }
}
