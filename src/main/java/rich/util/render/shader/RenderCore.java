package rich.util.render.shader;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import rich.client.splash.LoadingStages;
import rich.client.splash.SplashScreenManager;
import rich.util.render.font.FontRenderer;
import rich.util.render.font.Fonts;
import rich.util.render.pipeline.*;

import java.util.ArrayList;
import java.util.List;

public class RenderCore {

    private final RectPipeline rectPipeline;
    private final OutlinePipeline outlinePipeline;
    private final GlowOutlinePipeline glowOutlinePipeline;
    private final HoloSheenPipeline holoSheenPipeline;
    private final IridescentOutlinePipeline iridescentOutlinePipeline;
    private final TexturePipeline texturePipeline;
    private final BlurPipeline blurPipeline;
    private final KawaseBlurPipeline kawaseBlurPipeline;
    private final GlassCompositePipeline glassCompositePipeline;
    private final GlassHandsRenderer glassHandsRenderer;
    private final FontRenderer fontRenderer;
    private final MaskDiffPipeline maskDiffPipeline;

    private boolean fontsLoaded = false;
    private boolean arcInitialized = false;
    private boolean arcOutlineInitialized = false;

    public RenderCore() {
        this.rectPipeline = new RectPipeline();
        this.outlinePipeline = new OutlinePipeline();
        this.glowOutlinePipeline = new GlowOutlinePipeline();
        this.holoSheenPipeline = new HoloSheenPipeline();
        this.iridescentOutlinePipeline = new IridescentOutlinePipeline();
        this.texturePipeline = new TexturePipeline();
        this.blurPipeline = new BlurPipeline();
        this.kawaseBlurPipeline = new KawaseBlurPipeline();
        this.glassCompositePipeline = new GlassCompositePipeline();
        this.glassHandsRenderer = new GlassHandsRenderer();
        this.maskDiffPipeline = new MaskDiffPipeline();
        this.fontRenderer = new FontRenderer();
    }

    public void init() {
        LoadingStages.COMPILING_CORE_SHADERS.update();
        compileAllPipelines();

        LoadingStages.LOADING_FONTS.update();
        ensureFontsLoaded();
        ensureArcInitialized();
        ensureArcOutlineInitialized();
    }

    private void compileAllPipelines() {
        List<CompileTask> tasks = buildCompileTasks();
        ShaderCompilationTracker.beginTracking(tasks.size());
        ShaderCompilationTracker.setCallback((compiled, total, currentShader, elapsedMs) -> {
            int progress = 20 + Math.round((compiled / (float) total) * 35f);
            SplashScreenManager.updateProgress(progress, "Компиляция шейдеров: " + currentShader + " (" + compiled + "/" + total + ")");
        });

        for (CompileTask task : tasks) {
            ShaderCompilationTracker.startCompilation(task.name());
            try {
                task.run();
            } catch (Exception ignored) {
                // Continue startup even if one warmup pass fails on a specific GPU/driver.
            }
            ShaderCompilationTracker.completeShader();
        }

        ShaderCompilationTracker.setCallback(null);
    }

    private List<CompileTask> buildCompileTasks() {
        List<CompileTask> tasks = new ArrayList<>();

        tasks.add(new CompileTask("Rect", () -> rectPipeline.drawRect(
                2f, 2f, 18f, 10f,
                new int[]{0x00000000},
                new float[]{2f, 2f, 2f, 2f}
        )));

        tasks.add(new CompileTask("Outline", () -> outlinePipeline.drawOutline(
                2f, 2f, 18f, 10f,
                new int[]{0x00000000},
                new float[]{1f},
                new float[]{2f, 2f, 2f, 2f},
                1f
        )));

        tasks.add(new CompileTask("GlowOutline", () -> glowOutlinePipeline.drawGlowOutline(
                2f, 2f, 18f, 10f,
                0x00000000,
                1f,
                new float[]{2f, 2f, 2f, 2f},
                0.5f,
                0f
        )));

        tasks.add(new CompileTask("IridescentOutline", () -> iridescentOutlinePipeline.drawOutline(
                2f, 2f, 18f, 10f,
                1f, 2f,
                0.5f, 0.8f, 1f, 0f
        )));

        tasks.add(new CompileTask("HoloSheen", () -> holoSheenPipeline.drawSheen(
                2f, 2f, 18f, 10f,
                2f, 0x00000000,
                0.5f, 0.5f, 0f, 0f
        )));

        tasks.add(new CompileTask("Texture", () -> texturePipeline.drawTexture(
                Identifier.of("rich", "icon"),
                2f, 2f, 18f, 10f,
                0f, 0f, 1f, 1f,
                new int[]{0x00FFFFFF},
                new float[]{2f, 2f, 2f, 2f},
                1f
        )));

        tasks.add(new CompileTask("Blur", () -> blurPipeline.drawBlur(
                2f, 2f, 18f, 10f,
                1.2f,
                new float[]{2f, 2f, 2f, 2f},
                0x00FFFFFF
        )));

        tasks.add(new CompileTask("Kawase", this::warmupKawase));
        tasks.add(new CompileTask("MaskDiff", this::warmupMaskDiff));
        tasks.add(new CompileTask("GlassComposite", this::warmupGlassComposite));

        return tasks;
    }

    private void warmupKawase() {
        Framebuffer fb = getClient().getFramebuffer();
        if (fb == null || fb.getColorAttachment() == null || fb.getColorAttachmentView() == null) {
            return;
        }
        kawaseBlurPipeline.blur(fb.getColorAttachment(), fb.getColorAttachmentView(), fb.textureWidth, fb.textureHeight, 1, 0.7f);
    }

    private void warmupMaskDiff() {
        Framebuffer fb = getClient().getFramebuffer();
        if (fb == null || fb.getColorAttachmentView() == null || fb.getDepthAttachmentView() == null) {
            return;
        }
        maskDiffPipeline.createMask(
                fb.getColorAttachmentView(),
                fb.getColorAttachmentView(),
                fb.getColorAttachmentView(),
                fb.getDepthAttachmentView(),
                fb.getDepthAttachmentView(),
                fb.textureWidth,
                fb.textureHeight
        );
    }

    private void warmupGlassComposite() {
        Framebuffer fb = getClient().getFramebuffer();
        if (fb == null || fb.getColorAttachmentView() == null) {
            return;
        }

        GpuTextureView view = fb.getColorAttachmentView();
        glassCompositePipeline.composite(
                view,
                view,
                view,
                view,
                fb.textureWidth,
                fb.textureHeight,
                1f,
                true,
                0x00000000,
                0f,
                0f
        );
    }

    private void ensureFontsLoaded() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        fontRenderer.loadAllFonts(Fonts.getRegistry());
    }

    private void ensureArcInitialized() {
        if (arcInitialized) return;
        arcInitialized = true;
        Arc2D.init();
    }

    private void ensureArcOutlineInitialized() {
        if (arcOutlineInitialized) return;
        arcOutlineInitialized = true;
        ArcOutline2D.init();
    }

    public void setupOverlayState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void restoreState() {
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void clearDepthBuffer() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void initArc() {
        ensureArcInitialized();
    }

    public void initArcOutline() {
        ensureArcOutlineInitialized();
    }

    public RectPipeline getRectPipeline() {
        return rectPipeline;
    }

    public OutlinePipeline getOutlinePipeline() {
        return outlinePipeline;
    }

    public GlowOutlinePipeline getGlowOutlinePipeline() {
        return glowOutlinePipeline;
    }

    public HoloSheenPipeline getHoloSheenPipeline() {
        return holoSheenPipeline;
    }

    public IridescentOutlinePipeline getIridescentOutlinePipeline() {
        return iridescentOutlinePipeline;
    }

    public TexturePipeline getTexturePipeline() {
        return texturePipeline;
    }

    public BlurPipeline getBlurPipeline() {
        return blurPipeline;
    }

    public KawaseBlurPipeline getKawaseBlurPipeline() {
        return kawaseBlurPipeline;
    }

    public GlassCompositePipeline getGlassCompositePipeline() {
        return glassCompositePipeline;
    }

    public GlassHandsRenderer getGlassHandsRenderer() {
        return glassHandsRenderer;
    }

    public FontRenderer getFontRenderer() {
        ensureFontsLoaded();
        return fontRenderer;
    }

    public MaskDiffPipeline getMaskDiffPipeline() {
        return maskDiffPipeline;
    }

    public MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }

    public void close() {
        rectPipeline.close();
        outlinePipeline.close();
        glowOutlinePipeline.close();
        holoSheenPipeline.close();
        iridescentOutlinePipeline.close();
        texturePipeline.close();
        blurPipeline.close();
        kawaseBlurPipeline.close();
        glassCompositePipeline.close();
        glassHandsRenderer.close();
        maskDiffPipeline.close();
        fontRenderer.close();
        Arc2D.shutdown();
        ArcOutline2D.shutdown();
    }

    private record CompileTask(String name, Runnable action) {
        void run() {
            action.run();
        }
    }
}
