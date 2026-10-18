package rich.util.render.draw;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4i;

public interface DrawEngine {

    void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height);

    void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color);
    void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color);

    void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color);
}
