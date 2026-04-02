package fun.aegis.mixins.game.tracker;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.util.math.EulerAngle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataTracker.class)
public abstract class DataTrackerMixin {

    @Inject(method = "set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true)
    private <T> void onSet(TrackedData<T> data, T value, CallbackInfo ci) {
        TrackedDataHandler<T> serializer = data.dataType();
        if (serializer == TrackedDataHandlerRegistry.BYTE && !(value instanceof Byte)) {
            ci.cancel();
        }
        if (serializer == TrackedDataHandlerRegistry.ROTATION && !(value instanceof EulerAngle)) {
            ci.cancel();
        }
    }
}
