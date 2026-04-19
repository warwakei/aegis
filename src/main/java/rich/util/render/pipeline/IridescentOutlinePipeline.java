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

public class IridescentOutlinePipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("rich", "pipeline/iridescent_outline");
    private static final Identifier VERTEX_SHADER = Identifier.of("rich", "core/iridescent_outline");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("rich", "core/iridescent_outline");

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final float FIXED_GUI_SCALE = 2.0f;
    private static final int BUFFER_SIZE = 128;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("IriOutlineData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    private void ensureInitialized() {
        if (initialized) return;

        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        try {
            dummyData.putInt(0);
            dummyData.flip();
            this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:iridescent_outline_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX,
                    dummyData
            );
        } finally {
            MemoryUtil.memFree(dummyData);
        }

        initialized = true;
    }

    public void drawOutline(float x, float y, float width, float height, float thickness, float radius,
                            float speed, float saturation, float value, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;

        ensureInitialized();

        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        float fixedScreenWidth = framebufferWidth / FIXED_GUI_SCALE;
        float fixedScreenHeight = framebufferHeight / FIXED_GUI_SCALE;

        float time = (System.currentTimeMillis() % 1_000_000L) / 1000.0f;

        prepareUniformData(
                x, y, width, height,
                fixedScreenWidth, fixedScreenHeight,
                FIXED_GUI_SCALE,
                thickness,
                radius,
                time,
                speed,
                saturation,
                value,
                alpha
        );

        uploadAndDraw(client);
    }

    private void prepareUniformData(float x, float y, float width, float height,
                                    float screenWidth, float screenHeight,
                                    float guiScale,
                                    float thickness,
                                    float radius,
                                    float timeSeconds,
                                    float speed,
                                    float saturation,
                                    float value,
                                    float alpha) {
        dataBuffer.clear();

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(guiScale);
        dataBuffer.putFloat(thickness);

        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(radius);

        dataBuffer.putFloat(timeSeconds);
        dataBuffer.putFloat(speed);
        dataBuffer.putFloat(saturation);
        dataBuffer.putFloat(value);

        dataBuffer.putFloat(alpha);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);

        dataBuffer.flip();
    }

    private void uploadAndDraw(MinecraftClient client) {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) uniformBuffer.close();
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:iridescent_outline_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "minecraft:iridescent_outline_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("IriOutlineData", uniformBuffer);

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

