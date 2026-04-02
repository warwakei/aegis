package rich.util.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
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

public class BlurPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("rich", "pipeline/blur");
    private static final Identifier VERTEX_SHADER = Identifier.of("rich", "core/blur");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("rich", "core/blur");

    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final int BUFFER_SIZE = 128;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;

    private GpuTexture copyTexture;
    private GpuTextureView copyTextureView;
    private int lastWidth = 0;
    private int lastHeight = 0;

    private boolean initialized = false;

    private int getFixedScaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return 960;
        return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return 540;
        return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    private void ensureInitialized() {
        if (initialized) return;

        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "minecraft:blur_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        initialized = true;
    }

    private void ensureCopyTexture(int width, int height) {
        if (copyTexture == null || lastWidth != width || lastHeight != height) {
            if (copyTextureView != null) {
                copyTextureView.close();
                copyTextureView = null;
            }
            if (copyTexture != null) {
                copyTexture.close();
                copyTexture = null;
            }

            copyTexture = RenderSystem.getDevice().createTexture(
                    () -> "minecraft:blur_copy",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8,
                    width, height, 1, 1
            );
            copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);

            lastWidth = width;
            lastHeight = height;
        }
    }

    public void drawBlur(float x, float y, float width, float height,
                         float radius, float[] radii, int color) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (client.getFramebuffer().getColorAttachment() == null) return;

        ensureInitialized();

        int fbWidth = client.getFramebuffer().textureWidth;
        int fbHeight = client.getFramebuffer().textureHeight;

        ensureCopyTexture(fbWidth, fbHeight);

        int fixedScreenWidth = getFixedScaledWidth();
        int fixedScreenHeight = getFixedScaledHeight();

        prepareUniformData(x, y, width, height,
                fixedScreenWidth,
                fixedScreenHeight,
                fbWidth, fbHeight,
                FIXED_GUI_SCALE, radius, radii, color);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.copyTextureToTexture(
                client.getFramebuffer().getColorAttachment(),
                copyTexture,
                0, 0, 0, 0, 0,
                fbWidth, fbHeight
        );

        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(),
                        COLOR_MODULATOR,
                        MODEL_OFFSET,
                        TEXTURE_MATRIX);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "minecraft:blur_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("Sampler0", copyTextureView, sampler);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("BlurData", uniformBuffer);

            renderPass.draw(0, 6);
        }
    }

    private void prepareUniformData(float x, float y, float width, float height,
                                    float screenWidth, float screenHeight,
                                    int fbWidth, int fbHeight,
                                    float guiScale, float blurRadius,
                                    float[] radii, int color) {
        dataBuffer.clear();

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(guiScale);
        dataBuffer.putFloat(blurRadius);

        dataBuffer.putFloat(fbWidth);
        dataBuffer.putFloat(fbHeight);
        dataBuffer.putFloat(0);
        dataBuffer.putFloat(0);

        dataBuffer.putFloat(radii[0]);
        dataBuffer.putFloat(radii[1]);
        dataBuffer.putFloat(radii[2]);
        dataBuffer.putFloat(radii[3]);

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(a);

        dataBuffer.flip();

        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:blur_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
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
        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }
        lastWidth = 0;
        lastHeight = 0;
        initialized = false;
    }
}