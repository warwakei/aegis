package fun.aegis.mixins.player.cape;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {
    @Shadow @Final private GameProfile profile;

    @Unique
    private static final Identifier CUSTOM_CAPE = Identifier.of("minecraft", "textures/cape/cape.png");
//
//    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
//    private void injectCustomCape(CallbackInfoReturnable<SkinTextures> cir) {
//        SkinTextures original = cir.getReturnValue();
//
//        SkinTextures modified = new SkinTextures(
//                original.texture(),
//                original.textureUrl(),
//                CUSTOM_CAPE,
//                CUSTOM_CAPE,
//                original.model(),
//                original.secure()
//        );
//
//        cir.setReturnValue(modified);
//    }
}
