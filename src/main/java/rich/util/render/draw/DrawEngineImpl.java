package rich.util.render.draw;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4i;
import rich.IMinecraft;

import static com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrawEngineImpl implements DrawEngine, IMinecraft {
    @Override
    public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height) {
        buffer.vertex(matrix4f, x, y, 0);
        buffer.vertex(matrix4f, x, y + height, 0);
        buffer.vertex(matrix4f, x + width, y + height, 0);
        buffer.vertex(matrix4f, x + width, y, 0);
    }

    @Override
    public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
        buffer.vertex(matrix4f, x, y, 0).color(color);
        buffer.vertex(matrix4f, x, y + height, 0).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0).color(color);
        buffer.vertex(matrix4f, x + width, y, 0).color(color);
    }

    @Override
    public void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
        BufferBuilder buffer = tessellator.begin(QUADS, POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix4f, x, y + height, 0).texture(0, 0).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0).texture(0, 1).color(color);
        buffer.vertex(matrix4f, x + width, y, 0).texture(1, 1).color(color);
        buffer.vertex(matrix4f, x, y, 0).texture(1, 0).color(color);

    }

    @Override
    public void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0).texture(0, 0).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0).texture(0, 1).color(color.y);
        buffer.vertex(entry, x + width, y, 0).texture(1, 1).color(color.w);
        buffer.vertex(entry, x, y, 0).texture(1, 0).color(color.z);
    }
}