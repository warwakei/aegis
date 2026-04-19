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
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class HoloSheenPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("rich", "pipeline/holo_sheen");
    private static final Identifier VERTEX_SHADER = Identifier.of("rich", "core/holo_sheen");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("rich", "core/holo_sheen");

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final float FIXED_GUI_SCALE = 2.0f;
    private static final int BUFFER_SIZE = 96; // rect(16) + screen(16) + radii(16) + tintColor(16) + params0(16) + params1(16) = 96

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("SheenData", UniformType.UNIFORM_BUFFER)
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
                    () -> "minecraft:holo_sheen_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX,
                    dummyData
            );
        } finally {
            MemoryUtil.memFree(dummyData);
        }

        initialized = true;
    }

    public void drawSheen(float x, float y, float width, float height, float radius, int tintColor,
                          float intensity, float speed, float angleRadians, float grain) {
        drawSheen(x, y, width, height, radius, tintColor, intensity, speed, angleRadians, grain, null, 0);
    }

    public void drawSheen(float x, float y, float width, float height, float radius, int tintColor,
                          float intensity, float speed, float angleRadians, float grain,
                          Identifier noiseTextureId, int blendMode) {
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
                radius,
                tintColor,
                time, intensity, speed,
                angleRadians, grain,
                blendMode
        );

        int noiseTextureGlId = 0;
        GpuTextureView noiseTextureView = null;
        if (noiseTextureId != null) {
            AbstractTexture texture = client.getTextureManager().getTexture(noiseTextureId);
            if (texture != null) {
                try {
                    GpuTexture gpuTexture = texture.getGlTexture();
                    if (gpuTexture != null) {
                        noiseTextureGlId = getTextureGlId(gpuTexture);
                        noiseTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        uploadAndDraw(client, noiseTextureGlId, noiseTextureView);
    }

    private void prepareUniformData(float x, float y, float width, float height,
                                    float screenWidth, float screenHeight,
                                    float guiScale,
                                    float radius,
                                    int tintColor,
                                    float timeSeconds, float intensity, float speed,
                                    float angleRadians, float grain,
                                    int blendMode) {
        dataBuffer.clear();

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(guiScale);
        dataBuffer.putFloat(0f);

        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(radius);

        float a = ((tintColor >> 24) & 0xFF) / 255.0f;
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;

        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(a);

        dataBuffer.putFloat(timeSeconds);
        dataBuffer.putFloat(intensity);
        dataBuffer.putFloat(speed);
        dataBuffer.putFloat(angleRadians);

        dataBuffer.putFloat(grain);
        dataBuffer.putFloat(blendMode); // New blendMode
        dataBuffer.putFloat(0f); // Padding
        dataBuffer.putFloat(0f); // Padding

        dataBuffer.flip();
    }

    private void uploadAndDraw(MinecraftClient client, int noiseTextureGlId, GpuTextureView noiseTextureView) {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) uniformBuffer.close();
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:holo_sheen_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "minecraft:holo_sheen_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("SheenData", uniformBuffer);
            if (noiseTextureView != null) {
                renderPass.bindTexture("NoiseSampler", noiseTextureView, sampler);
            }

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

    private int getTextureGlId(GpuTexture gpuTexture) {
        try {
            var field = gpuTexture.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return field.getInt(gpuTexture);
        } catch (Exception e1) {
            try {
                var field = gpuTexture.getClass().getDeclaredField("glId");
                field.setAccessible(true);
                return field.getInt(gpuTexture);
            } catch (Exception e2) {
                try {
                    for (var f : gpuTexture.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            int value = f.getInt(gpuTexture);
                            if (value > 0) {
                                return value;
                            }
                        }
                    }
                } catch (Exception e3) {
                }
            }
        }
        return 0;
    }
}

