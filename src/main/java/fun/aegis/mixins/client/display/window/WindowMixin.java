package fun.aegis.mixins.client.display.window;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "logGlError", at = @At("HEAD"), cancellable = true)
    private void suppressInvalidKeyError(int error, long description, CallbackInfo ci) {
        if (error == 65539 && description != 0) {
            String desc = org.lwjgl.system.MemoryUtil.memUTF8(description);
            if ("Invalid key -1".equals(desc)) {
                ci.cancel();
            }
        }
    }
}
