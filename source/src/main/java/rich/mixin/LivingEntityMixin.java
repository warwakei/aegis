package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.events.api.EventManager;
import rich.events.impl.JumpEvent;
import rich.events.impl.PushEvent;
import rich.events.impl.SwingDurationEvent;
import rich.modules.impl.combat.aura.AngleConnection;

import java.lang.reflect.Method;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);
    @Shadow @Nullable
    public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);
    @Shadow public float bodyYaw;
    @Shadow public abstract boolean isInSwimmingPose();
    @Shadow protected abstract double getEffectiveGravity();
    @Unique private final MinecraftClient client = MinecraftClient.getInstance();

    @Unique private static boolean baritoneChecked = false;
    @Unique private static boolean baritoneAvailable = false;
    @Unique private static Method getProviderMethod;
    @Unique private static Method getPrimaryBaritoneMethod;
    @Unique private static Method getPathingBehaviorMethod;
    @Unique private static Method isPathingMethod;

    @Unique
    private boolean isBaritonePathing() {
        try {
            if (!baritoneChecked) {
                baritoneChecked = true;
                try {
                    Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
                    getProviderMethod = apiClass.getMethod("getProvider");
                    Class<?> providerClass = Class.forName("baritone.api.IBaritoneProvider");
                    getPrimaryBaritoneMethod = providerClass.getMethod("getPrimaryBaritone");
                    Class<?> baritoneClass = Class.forName("baritone.api.IBaritone");
                    getPathingBehaviorMethod = baritoneClass.getMethod("getPathingBehavior");
                    Class<?> pathingClass = Class.forName("baritone.api.behavior.IPathingBehavior");
                    isPathingMethod = pathingClass.getMethod("isPathing");
                    baritoneAvailable = true;
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    baritoneAvailable = false;
                }
            }
            if (!baritoneAvailable) {
                return false;
            }
            Object provider = getProviderMethod.invoke(null);
            if (provider == null) return false;
            Object baritone = getPrimaryBaritoneMethod.invoke(provider);
            if (baritone == null) return false;
            Object pathingBehavior = getPathingBehaviorMethod.invoke(baritone);
            if (pathingBehavior == null) return false;
            Object result = isPathingMethod.invoke(pathingBehavior);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private boolean shouldApplyRichMoveCorrection() {
        var rotationManager = AngleConnection.INSTANCE;
        var rotation = rotationManager.getRotation();
        var configurable = rotationManager.getCurrentRotationPlan();
        return rotation != null && configurable != null && configurable.isMoveCorrection();
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void isPushable(CallbackInfoReturnable<Boolean> infoReturnable) {
        PushEvent event = new PushEvent(PushEvent.Type.COLLISION);
        EventManager.callEvent(event);
        if (event.isCancelled()) infoReturnable.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(Vec3d original) {
        if ((Object) this != client.player) {
            return original;
        }
        if (isBaritonePathing()) {
            return original;
        }
        if (!shouldApplyRichMoveCorrection()) {
            return original;
        }
        float yaw = AngleConnection.INSTANCE.getMoveRotation().getYaw() * 0.017453292F;
        return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jump(CallbackInfo info) {
        if ((Object) this instanceof ClientPlayerEntity player) {
            if (isBaritonePathing()) {
                return;
            }
            JumpEvent event = new JumpEvent(player);
            EventManager.callEvent(event);
            if (event.isCancelled()) info.cancel();
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != client.player) {
            return;
        }
        SwingDurationEvent event = new SwingDurationEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(client.player)) animation *= (6 - (1 + StatusEffectUtil.getHasteAmplifier(client.player)));
            else animation *= (hasStatusEffect(StatusEffects.MINING_FATIGUE) ? 6 + (1 + getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6);
            cir.setReturnValue((int) animation);
        }
    }

    @Inject(method = "calcGlidingVelocity", at = @At("HEAD"), cancellable = true)
    private void calcGlidingVelocityFull(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this != client.player) {
            return;
        }
        if (isBaritonePathing()) {
            return;
        }
        var rotationManager = AngleConnection.INSTANCE;
        var rotation = rotationManager.getRotation();
        var configurable = rotationManager.getCurrentRotationPlan();
        if (rotation == null || configurable == null || !configurable.isMoveCorrection() || configurable.isChangeLook()) {
            return;
        }
        Vec3d vec3d = rotation.toVector();
        float f = rotation.getPitch() * (float) (Math.PI / 180.0);
        double d = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e = oldVelocity.horizontalLength();
        double g = this.getEffectiveGravity();
        double h = MathHelper.square(Math.cos(f));
        oldVelocity = oldVelocity.add(0.0, g * (-1.0 + h * 0.75), 0.0);
        if (oldVelocity.y < 0.0 && d > 0.0) {
            double i = oldVelocity.y * -0.1 * h;
            oldVelocity = oldVelocity.add(vec3d.x * i / d, i, vec3d.z * i / d);
        }
        if (f < 0.0F && d > 0.0) {
            double i = e * -MathHelper.sin(f) * 0.04;
            oldVelocity = oldVelocity.add(-vec3d.x * i / d, i * 3.2, -vec3d.z * i / d);
        }
        if (d > 0.0) {
            oldVelocity = oldVelocity.add((vec3d.x / d * e - oldVelocity.x) * 0.1, 0.0, (vec3d.z / d * e - oldVelocity.z) * 0.1);
        }
        cir.setReturnValue(oldVelocity.multiply(0.99F, 0.98F, 0.99F));
        cir.cancel();
    }
}