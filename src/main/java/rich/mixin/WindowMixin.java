package rich.mixin;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

@Mixin(Window.class)
public class WindowMixin {
//    @Shadow @Final private long handle;

//    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
//    private void onSetIcon(CallbackInfo ci) {
//        ci.cancel();
//
//        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
//            GLFWImage.Buffer buffer = GLFWImage.malloc(1, memoryStack);
//            try (NativeImage nativeImage = NativeImage.read(WindowMixin.class.getResourceAsStream("/assets/minecraft/images/elements/logo.png"))) {
//                ByteBuffer byteBuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
//                byteBuffer.asIntBuffer().put(nativeImage.copyPixelsAbgr());
//                buffer.position(0);
//                buffer.width(nativeImage.getWidth());
//                buffer.height(nativeImage.getHeight());
//                buffer.pixels(byteBuffer);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            GLFW.glfwSetWindowIcon(this.handle, buffer.position(0));
//        }
//    }
//
//    @Inject(method = "logGlError", at = @At("HEAD"), cancellable = true)
//    private void onGlError(int error, long description, CallbackInfo ci) {
//        ci.cancel();
//    }
}
