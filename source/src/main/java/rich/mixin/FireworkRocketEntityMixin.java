package rich.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rich.IMinecraft;
import rich.events.api.EventManager;
import rich.events.impl.FireworkEvent;
import rich.modules.impl.combat.aura.AngleConnection;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin implements IMinecraft {

    @Shadow @Nullable private LivingEntity shooter;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d getRotationVectorHook(LivingEntity instance, Operation<Vec3d> original) {
        if (shooter == mc.player && shooter.isGliding()) {
            return AngleConnection.INSTANCE.getMoveRotation().toVector();
        }
        return original.call(instance);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    public void setVelocityHook(LivingEntity instance, Vec3d velocity, Operation<Void> original) {
        if (shooter == mc.player && shooter.isGliding()) {
            FireworkEvent event = new FireworkEvent(velocity);
            EventManager.callEvent(event);
            original.call(instance, event.getVector());
        } else {
            original.call(instance, velocity);
        }
    }
}