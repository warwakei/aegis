package rich.util.render.shader;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import rich.util.render.pipeline.GlassCompositePipeline;
import rich.util.render.pipeline.KawaseBlurPipeline;
import rich.util.render.pipeline.MaskDiffPipeline;

public class GlassHandsRenderer {

    private static GlassHandsRenderer instance;

    private final MinecraftClient client;
    private KawaseBlurPipeline kawaseBlur;
    private GlassCompositePipeline glassComposite;
    private MaskDiffPipeline maskDiff;

    private GpuTexture sceneBeforeTexture;
    private GpuTextureView sceneBeforeTextureView;
    private GpuTexture sceneAfterTexture;
    private GpuTextureView sceneAfterTextureView;
    private GpuTexture depthBeforeTexture;
    private GpuTextureView depthBeforeTextureView;
    private GpuTexture depthAfterTexture;
    private GpuTextureView depthAfterTextureView;
    private GpuTexture maskTexture;
    private GpuTextureView maskTextureView;

    private int lastWidth = 0;
    private int lastHeight = 0;

    private boolean capturing = false;
    private boolean enabled = false;
    private boolean initialized = false;

    private float blurRadius = 6.0f;
    private int blurIterations = 4;
    private float saturation = 1.0f;
    private boolean reflect = true;
    private int tintColor = 0x00000000;
    private float tintIntensity = 0.1f;
    private float edgeGlowIntensity = 0.3f;

    public GlassHandsRenderer() {
        this.client = MinecraftClient.getInstance();
        instance = this;
    }

    public static GlassHandsRenderer getInstance() {
        if (instance == null) {
            instance = new GlassHandsRenderer();
        }
        return instance;
    }

    public static void resetInstance() {
        if (instance != null) {
            instance.close();
            instance.initialized = false;
        }
    }

    private void ensureInitialized() {
        if (initialized) return;

        if (kawaseBlur != null) kawaseBlur.close();
        if (glassComposite != null) glassComposite.close();
        if (maskDiff != null) maskDiff.close();

        this.kawaseBlur = new KawaseBlurPipeline();
        this.glassComposite = new GlassCompositePipeline();
        this.maskDiff = new MaskDiffPipeline();

        lastWidth = 0;
        lastHeight = 0;

        initialized = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            ensureInitialized();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
    }

    public void setBlurIterations(int iterations) {
        this.blurIterations = Math.max(1, Math.min(8, iterations));
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setReflect(boolean reflect) {
        this.reflect = reflect;
    }

    public void setTintColor(int color) {
        this.tintColor = color;
    }

    public void setTintIntensity(float intensity) {
        this.tintIntensity = intensity;
    }

    public void setEdgeGlowIntensity(float intensity) {
        this.edgeGlowIntensity = intensity;
    }

    private void ensureTextures(int width, int height) {
        if (width == lastWidth && height == lastHeight && sceneBeforeTexture != null) return;

        cleanupTextures();

        sceneBeforeTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:glass_scene_before",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        sceneBeforeTextureView = RenderSystem.getDevice().createTextureView(sceneBeforeTexture);

        sceneAfterTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:glass_scene_after",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        sceneAfterTextureView = RenderSystem.getDevice().createTextureView(sceneAfterTexture);

        depthBeforeTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:glass_depth_before",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.DEPTH32,
                width, height, 1, 1
        );
        depthBeforeTextureView = RenderSystem.getDevice().createTextureView(depthBeforeTexture);

        depthAfterTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:glass_depth_after",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.DEPTH32,
                width, height, 1, 1
        );
        depthAfterTextureView = RenderSystem.getDevice().createTextureView(depthAfterTexture);

        maskTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:glass_mask",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        maskTextureView = RenderSystem.getDevice().createTextureView(maskTexture);

        lastWidth = width;
        lastHeight = height;
    }

    private void cleanupTextures() {
        if (sceneBeforeTextureView != null) {
            sceneBeforeTextureView.close();
            sceneBeforeTextureView = null;
        }
        if (sceneBeforeTexture != null) {
            sceneBeforeTexture.close();
            sceneBeforeTexture = null;
        }
        if (sceneAfterTextureView != null) {
            sceneAfterTextureView.close();
            sceneAfterTextureView = null;
        }
        if (sceneAfterTexture != null) {
            sceneAfterTexture.close();
            sceneAfterTexture = null;
        }
        if (depthBeforeTextureView != null) {
            depthBeforeTextureView.close();
            depthBeforeTextureView = null;
        }
        if (depthBeforeTexture != null) {
            depthBeforeTexture.close();
            depthBeforeTexture = null;
        }
        if (depthAfterTextureView != null) {
            depthAfterTextureView.close();
            depthAfterTextureView = null;
        }
        if (depthAfterTexture != null) {
            depthAfterTexture.close();
            depthAfterTexture = null;
        }
        if (maskTextureView != null) {
            maskTextureView.close();
            maskTextureView = null;
        }
        if (maskTexture != null) {
            maskTexture.close();
            maskTexture = null;
        }
    }

    public void captureSceneBeforeHands() {
        if (!enabled) return;

        ensureInitialized();

        Framebuffer fb = client.getFramebuffer();
        if (fb == null || fb.getColorAttachment() == null) return;

        int width = fb.textureWidth;
        int height = fb.textureHeight;

        ensureTextures(width, height);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.copyTextureToTexture(
                fb.getColorAttachment(),
                sceneBeforeTexture,
                0, 0, 0, 0, 0,
                width, height
        );

        if (fb.getDepthAttachment() != null) {
            encoder.copyTextureToTexture(
                    fb.getDepthAttachment(),
                    depthBeforeTexture,
                    0, 0, 0, 0, 0,
                    width, height
            );
        }

        capturing = true;
    }

    public void captureSceneAfterHands() {
        if (!enabled || !capturing) return;

        Framebuffer fb = client.getFramebuffer();
        if (fb == null || fb.getColorAttachment() == null) return;

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.copyTextureToTexture(
                fb.getColorAttachment(),
                sceneAfterTexture,
                0, 0, 0, 0, 0,
                lastWidth, lastHeight
        );

        if (fb.getDepthAttachment() != null) {
            encoder.copyTextureToTexture(
                    fb.getDepthAttachment(),
                    depthAfterTexture,
                    0, 0, 0, 0, 0,
                    lastWidth, lastHeight
            );
        }
    }

    public void renderGlassEffect() {
        if (!enabled || !capturing) return;

        Framebuffer fb = client.getFramebuffer();
        if (fb == null || fb.getColorAttachment() == null) {
            capturing = false;
            return;
        }

        maskDiff.createMask(
                maskTextureView,
                sceneBeforeTextureView,
                sceneAfterTextureView,
                depthBeforeTextureView,
                depthAfterTextureView,
                lastWidth, lastHeight
        );

        GpuTextureView blurredView = kawaseBlur.blur(
                sceneBeforeTexture, sceneBeforeTextureView,
                lastWidth, lastHeight,
                blurIterations, blurRadius
        );

        if (blurredView == null) {
            capturing = false;
            return;
        }

        glassComposite.composite(
                fb.getColorAttachmentView(),
                sceneBeforeTextureView,
                blurredView,
                maskTextureView,
                lastWidth, lastHeight,
                saturation,
                reflect,
                tintColor,
                tintIntensity,
                edgeGlowIntensity
        );

        capturing = false;
    }

    public boolean isCapturing() {
        return capturing;
    }

    public void invalidate() {
        cleanupTextures();
        if (kawaseBlur != null) kawaseBlur.close();
        if (glassComposite != null) glassComposite.close();
        if (maskDiff != null) maskDiff.close();
        kawaseBlur = null;
        glassComposite = null;
        maskDiff = null;
        lastWidth = 0;
        lastHeight = 0;
        initialized = false;
        capturing = false;
    }

    public void close() {
        cleanupTextures();
        if (kawaseBlur != null) {
            kawaseBlur.close();
            kawaseBlur = null;
        }
        if (glassComposite != null) {
            glassComposite.close();
            glassComposite = null;
        }
        if (maskDiff != null) {
            maskDiff.close();
            maskDiff = null;
        }
        lastWidth = 0;
        lastHeight = 0;
        initialized = false;
    }
}