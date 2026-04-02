package rich.util.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
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

public class TexturePipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("rich", "pipeline/texture");
    private static final Identifier VERTEX_SHADER = Identifier.of("rich", "core/texture");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("rich", "core/texture");

    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("TextureData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final int BUFFER_SIZE = 256;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    public TexturePipeline() {
    }

    private void ensureInitialized() {
        if (initialized)
            return;

        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "minecraft:texture_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData);
        MemoryUtil.memFree(dummyData);

        initialized = true;
    }

    public void drawTexture(Identifier textureId, float x, float y, float width, float height,
                            float u0, float v0, float u1, float v1,
                            int[] colors, float[] radii, float smoothness) {
        drawTexture(textureId, x, y, width, height, u0, v0, u1, v1, colors, radii, smoothness, 0f);
    }

    public void drawTexture(Identifier textureId, float x, float y, float width, float height,
                            float u0, float v0, float u1, float v1,
                            int[] colors, float[] radii, float smoothness, float rotation) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) {
            return;
        }

        AbstractTexture texture = client.getTextureManager().getTexture(textureId);
        if (texture == null) {
            return;
        }

        int textureGlId;
        try {
            GpuTexture gpuTexture = texture.getGlTexture();
            if (gpuTexture == null) {
                return;
            }
            textureGlId = getTextureGlId(gpuTexture);
            if (textureGlId <= 0) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        ensureInitialized();

        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        float fixedScreenWidth = framebufferWidth / FIXED_GUI_SCALE;
        float fixedScreenHeight = framebufferHeight / FIXED_GUI_SCALE;

        prepareUniformData(x, y, width, height, u0, v0, u1, v1,
                fixedScreenWidth, fixedScreenHeight, FIXED_GUI_SCALE,
                colors, radii, smoothness, rotation);
        uploadAndDraw(client, textureGlId);
    }

    public void drawFramebufferTexture(int textureId, float x, float y, float width, float height,
                                       int[] colors, float[] radii, float alpha) {
        if (textureId <= 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) {
            return;
        }

        ensureInitialized();

        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        float fixedScreenWidth = framebufferWidth / FIXED_GUI_SCALE;
        float fixedScreenHeight = framebufferHeight / FIXED_GUI_SCALE;

        prepareUniformData(x, y, width, height, 0, 0, 1, 1,
                fixedScreenWidth, fixedScreenHeight, FIXED_GUI_SCALE,
                colors, radii, 1f, 0f);
        uploadAndDraw(client, textureId);
    }

    private void prepareUniformData(float x, float y, float w, float h,
                                    float u0, float v0, float u1, float v1,
                                    float screenWidth, float screenHeight, float guiScale,
                                    int[] colors, float[] radii, float smoothness, float rotation) {
        dataBuffer.clear();

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(smoothness);
        dataBuffer.putFloat(guiScale);

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(w);
        dataBuffer.putFloat(h);

        dataBuffer.putFloat(u0);
        dataBuffer.putFloat(v0);
        dataBuffer.putFloat(u1);
        dataBuffer.putFloat(v1);

        dataBuffer.putFloat(radii[0]);
        dataBuffer.putFloat(radii[1]);
        dataBuffer.putFloat(radii[2]);
        dataBuffer.putFloat(radii[3]);

        float rotationRadians = (float) Math.toRadians(rotation);
        dataBuffer.putFloat(rotationRadians);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);

        for (int i = 0; i < 4; i++) {
            int color = i < colors.length ? colors[i] : colors[colors.length - 1];
            float a = ((color >> 24) & 0xFF) / 255.0f;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            dataBuffer.putFloat(r);
            dataBuffer.putFloat(g);
            dataBuffer.putFloat(b);
            dataBuffer.putFloat(a);
        }

        dataBuffer.flip();
    }

    private void uploadAndDraw(MinecraftClient client, int textureGlId) {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:texture_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size);
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(),
                        COLOR_MODULATOR,
                        MODEL_OFFSET,
                        TEXTURE_MATRIX);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureGlId);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "minecraft:texture_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("TextureData", uniformBuffer);

            renderPass.draw(0, 6);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
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

    public void flush() {
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