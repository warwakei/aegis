package rich.mixin;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.modules.impl.render.NoRender;

@Mixin(StatusEffectFogModifier.class)
public abstract class StatusEffectFogModifierMixin {

    @Shadow
    public abstract RegistryEntry<StatusEffect> getStatusEffect();

    @Inject(method = "shouldApply", at = @At("HEAD"), cancellable = true)
    private void onShouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity, CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = NoRender.getInstance();
        if (!noRender.isState()) return;

        RegistryEntry<StatusEffect> effect = this.getStatusEffect();

        if (noRender.modeSetting.isSelected("Bad Effects") && effect == StatusEffects.BLINDNESS) {
            cir.setReturnValue(false);
        }

        if (noRender.modeSetting.isSelected("Darkness") && effect == StatusEffects.DARKNESS) {
            cir.setReturnValue(false);
        }
    }
}