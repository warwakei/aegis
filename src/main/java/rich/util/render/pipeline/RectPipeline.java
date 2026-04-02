package rich.util.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class RectPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("rich", "pipeline/rect");
    private static final Identifier VERTEX_SHADER = Identifier.of("rich", "core/rect");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("rich", "core/rect");

    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("RectData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final int BUFFER_SIZE = 256;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    public RectPipeline() {
    }

    private void ensureInitialized() {
        if (initialized) return;

        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "minecraft:dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        initialized = true;
    }

    public void drawRect(float x, float y, float width, float height,
                         int[] colors, float[] radii) {
        drawRect(x, y, width, height, colors, radii, 0f);
    }

    public void drawRect(float x, float y, float width, float height,
                         int[] colors, float[] radii, float innerBlur) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;

        ensureInitialized();

        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        float fixedScreenWidth = framebufferWidth / FIXED_GUI_SCALE;
        float fixedScreenHeight = framebufferHeight / FIXED_GUI_SCALE;

        int[] colors9 = convertTo9Colors(colors);

        prepareUniformData(x, y, width, height,
                fixedScreenWidth,
                fixedScreenHeight,
                FIXED_GUI_SCALE, innerBlur,
                colors9, radii);

        uploadAndDraw(client);
    }

    private int[] convertTo9Colors(int[] colors) {
        int[] result = new int[9];

        if (colors.length == 1) {
            for (int i = 0; i < 9; i++) {
                result[i] = colors[0];
            }
        } else if (colors.length == 4) {
            result[0] = colors[0];
            result[1] = blendColors(colors[0], colors[1]);
            result[2] = colors[1];
            result[3] = blendColors(colors[0], colors[3]);
            result[4] = blendColors(colors[0], colors[1], colors[2], colors[3]);
            result[5] = blendColors(colors[1], colors[2]);
            result[6] = colors[3];
            result[7] = blendColors(colors[3], colors[2]);
            result[8] = colors[2];
        } else if (colors.length >= 9) {
            System.arraycopy(colors, 0, result, 0, 9);
        } else {
            for (int i = 0; i < 9; i++) {
                result[i] = colors[i % colors.length];
            }
        }

        return result;
    }

    private int blendColors(int... colors) {
        int r = 0, g = 0, b = 0, a = 0;
        for (int color : colors) {
            a += (color >> 24) & 0xFF;
            r += (color >> 16) & 0xFF;
            g += (color >> 8) & 0xFF;
            b += color & 0xFF;
        }
        int count = colors.length;
        return ((a / count) << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
    }

    private void prepareUniformData(float x, float y, float width, float height,
                                    float screenWidth, float screenHeight,
                                    float guiScale, float innerBlur,
                                    int[] colors, float[] radii) {
        dataBuffer.clear();

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(guiScale);
        dataBuffer.putFloat(innerBlur);

        dataBuffer.putFloat(radii[0]);
        dataBuffer.putFloat(radii[1]);
        dataBuffer.putFloat(radii[2]);
        dataBuffer.putFloat(radii[3]);

        for (int i = 0; i < 9; i++) {
            int color = colors[i];
            float a = ((color >> 24) & 0xFF) / 255.0f;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float bl = (color & 0xFF) / 255.0f;

            dataBuffer.putFloat(r);
            dataBuffer.putFloat(g);
            dataBuffer.putFloat(bl);
            dataBuffer.putFloat(a);
        }

        dataBuffer.flip();
    }

    private void uploadAndDraw(MinecraftClient client) {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:rect_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(),
                        COLOR_MODULATOR,
                        MODEL_OFFSET,
                        TEXTURE_MATRIX);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "minecraft:rect_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("RectData", uniformBuffer);

            renderPass.draw(0, 6);
        }
    }

    public void close() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        initialized = false;
    }
}