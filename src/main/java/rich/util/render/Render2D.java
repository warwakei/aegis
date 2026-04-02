package rich.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import rich.Initialization;
import rich.util.ColorUtil;
import rich.util.render.pipeline.Arc2D;
import rich.util.render.pipeline.ArcOutline2D;

import java.util.ArrayList;
import java.util.List;

public class Render2D {

    private static boolean inOverlayMode = false;
    private static boolean savedDepthTest = false;
    private static boolean savedDepthMask = false;
    private static boolean savedBlend = false;

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("rich", "textures/menu/backmenu.png");

    private static final List<Runnable> OVERRIDE_TASKS = new ArrayList<>();
    private static final float Z_OVERRIDE = 0.0f;
    private static final float FIXED_GUI_SCALE = 2.0f;

    public static int getFixedScaledWidth() {
        var window = MinecraftClient.getInstance().getWindow();
        return (int) Math.ceil((double) window.getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    public static int getFixedScaledHeight() {
        var window = MinecraftClient.getInstance().getWindow();
        return (int) Math.ceil((double) window.getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    public static float getFixedGuiScale() {
        return FIXED_GUI_SCALE;
    }

    public static float getScaleMultiplier() {
        MinecraftClient client = MinecraftClient.getInstance();
        float currentScale = (float) client.getWindow().getScaleFactor();
        return FIXED_GUI_SCALE / currentScale;
    }

    public static void beginOverlay() {
        inOverlayMode = true;

        savedDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void endOverlay() {
        if (savedDepthMask) {
            GL11.glDepthMask(true);
        }
        if (savedDepthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        if (!savedBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }

        inOverlayMode = false;
    }

    public static void clearDepth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null) {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }
    }

    public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public static void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public static void depthMask(boolean mask) {
        GL11.glDepthMask(mask);
    }

    public static void backgroundImage(float opacity) {
        backgroundImage(opacity, 1.0f);
    }

    public static void backgroundImage(float opacity, float zoom) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float zoomedWidth = screenWidth * zoom;
        float zoomedHeight = screenHeight * zoom;
        float offsetX = (screenWidth - zoomedWidth) / 2f;
        float offsetY = (screenHeight - zoomedHeight) / 2f;

        int alpha = (int) (opacity * 255);
        int color = (alpha << 24) | 0xFFFFFF;
        texture(BACKGROUND_TEXTURE, offsetX, offsetY, zoomedWidth, zoomedHeight, color);
    }

    public static void backgroundImage(float x, float y, float width, float height, float opacity) {
        int alpha = (int) (opacity * 255);
        int color = (alpha << 24) | 0xFFFFFF;
        texture(BACKGROUND_TEXTURE, x, y, width, height, color);
    }

    public static void rect(float x, float y, float width, float height, int color) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void rect(float x, float y, float width, float height, int color, float radius) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void rect(float x, float y, float width, float height, int color,
                            float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float radius) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float topLeft, float topRight,
                                    float bottomRight, float bottomLeft) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int topLeft, int topCenter, int topRight,
                                     int leftCenter, int center, int rightCenter,
                                     int bottomLeft, int bottomCenter, int bottomRight,
                                     float radius) {
        int[] colors = {topLeft, topCenter, topRight, leftCenter, center, rightCenter, bottomLeft, bottomCenter, bottomRight};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int[] colors9, float radius) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors9, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int[] colors9, float topLeft, float topRight,
                                     float bottomRight, float bottomLeft) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors9, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int topLeft, int topCenter, int topRight,
                                     int leftCenter, int center, int rightCenter,
                                     int bottomLeft, int bottomCenter, int bottomRight,
                                     float radius, float innerBlur) {
        int[] colors = {topLeft, topCenter, topRight, leftCenter, center, rightCenter, bottomLeft, bottomCenter, bottomRight};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii, innerBlur);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int[] colors9, float radius, float innerBlur) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors9, radii, innerBlur);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color, float radius) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color,
                               float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void gradientOutline(float x, float y, float width, float height, float thickness,
                                       int[] colors, float radius) {
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void blur(float x, float y, float width, float height, float blurRadius, int tintColor) {
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getBlurPipeline()
                .drawBlur(x, y, width, height, blurRadius, radii, tintColor);
    }

    public static void blur(float x, float y, float width, float height, float blurRadius, float cornerRadius, int tintColor) {
        float[] radii = {cornerRadius, cornerRadius, cornerRadius, cornerRadius};
        Initialization.getInstance().getManager().getRenderCore().getBlurPipeline()
                .drawBlur(x, y, width, height, blurRadius, radii, tintColor);
    }

    public static void blur(float x, float y, float width, float height, float blurRadius,
                            float topLeft, float topRight, float bottomRight, float bottomLeft, int tintColor) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getBlurPipeline()
                .drawBlur(x, y, width, height, blurRadius, radii, tintColor);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, 1f, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, float smoothness, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, smoothness, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, float smoothness, float radius, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, smoothness, radius);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color) {
        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color, float radius) {
        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, radius);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color, float smoothness, float radius) {
        int[] colors = {color, color, color, color};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(id, x, y, width, height, u0, v0, u1, v1, colors, radii, smoothness);
    }

    public static void drawTexture(DrawContext context, Identifier id,
                                   float x, float y, float width, float height,
                                   float u, float v, float regionWidth, float regionHeight,
                                   float textureWidth, float textureHeight,
                                   int color) {
        float u0 = u / textureWidth;
        float v0 = v / textureHeight;
        float u1 = (u + regionWidth) / textureWidth;
        float v1 = (v + regionHeight) / textureHeight;

        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, 0f);
    }

    public static void drawTexture(DrawContext context, Identifier id,
                                   float x, float y, float width, float height,
                                   float u, float v, float regionWidth, float regionHeight,
                                   float textureWidth, float textureHeight,
                                   int color, float radius) {
        float u0 = u / textureWidth;
        float v0 = v / textureHeight;
        float u1 = (u + regionWidth) / textureWidth;
        float v1 = (v + regionHeight) / textureHeight;

        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, radius);
    }

    public static void drawSprite(Sprite sprite, float x, float y, float width, float height, int color) {
        drawSprite(sprite, x, y, width, height, color, true);
    }

    public static void drawSprite(Sprite sprite, float x, float y, float width, float height, int color, boolean pixelPerfect) {
        if (sprite == null || width == 0 || height == 0) return;

        float smoothness = pixelPerfect ? 1f : 0f;
        texture(sprite.getAtlasId(), x, y, width, height,
                sprite.getMinU(), sprite.getMinV(),
                sprite.getMaxU(), sprite.getMaxV(),
                color, smoothness, 0f);
    }

    public static void drawSpriteSmooth(Sprite sprite, float x, float y, float width, float height, int color) {
        drawSprite(sprite, x, y, width, height, color, false);
    }

    public static void drawFramebufferTexture(int textureId, float x, float y, float width, float height,
                                              float r, float g, float b, float a) {
        int color = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        int[] colors = {color, color, color, color};
        float[] radii = {0, 0, 0, 0};

        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawFramebufferTexture(textureId, x, y, width, height, colors, radii, a);
    }

    public static void glowOutline(float x, float y, float width, float height, float thickness,
                                   int color, float radius, float progress, float baseAlpha) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getGlowOutlinePipeline()
                .drawGlowOutline(x, y, width, height, color, thickness, radii, progress, baseAlpha);
    }

    public static void glowOutline(float x, float y, float width, float height, float thickness,
                                   int color, float topLeft, float topRight, float bottomRight, float bottomLeft,
                                   float progress, float baseAlpha) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getGlowOutlinePipeline()
                .drawGlowOutline(x, y, width, height, color, thickness, radii, progress, baseAlpha);
    }

    public static Matrix4f createProjection() {
        int width = getFixedScaledWidth();
        int height = getFixedScaledHeight();
        return new Matrix4f().ortho(0, width, height, 0, -1000, 1000);
    }

    public static void arc(DrawContext context, float x, float y, float size, float thickness, float degree,
                           float rotation, int color, boolean overrideContext) {
        arc(createProjection(), x, y, size, thickness, degree, rotation, color, overrideContext);
    }

    public static void arc(DrawContext context, float x, float y, float size, float thickness, float degree,
                           float rotation, boolean overrideContext, int... colors) {
        arc(createProjection(), x, y, size, thickness, degree, rotation, overrideContext, colors);
    }

    public static void arc(Matrix4f matrix, float x, float y, float size, float thickness, float degree, float rotation,
                           int color, boolean overrideContext) {
        if (overrideContext) {
            OVERRIDE_TASKS.add(() -> Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, Z_OVERRIDE, color));
            return;
        }
        Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, Z_OVERRIDE, color);
    }

    public static void arc(Matrix4f matrix, float x, float y, float size, float thickness, float degree, float rotation,
                           boolean overrideContext, int... colors) {
        if (overrideContext) {
            OVERRIDE_TASKS.add(() -> Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, Z_OVERRIDE, colors));
            return;
        }
        Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, Z_OVERRIDE, colors);
    }

    public static void arc(float x, float y, float size, float thickness, float degree, float rotation, int color) {
        Arc2D.draw(createProjection(), x, y, size, thickness, degree, rotation, Z_OVERRIDE, color);
    }

    public static void arc(float x, float y, float size, float thickness, float degree, float rotation, int... colors) {
        Arc2D.draw(createProjection(), x, y, size, thickness, degree, rotation, Z_OVERRIDE, colors);
    }

    public static void arcOutline(float x, float y, float size, float arcThickness, float degree,
                                  float rotation, float outlineThickness, int fillColor, int outlineColor) {
        ArcOutline2D.draw(createProjection(), x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, Z_OVERRIDE);
    }

    public static void arcOutline(DrawContext context, float x, float y, float size, float arcThickness, float degree,
                                  float rotation, float outlineThickness, int fillColor, int outlineColor, boolean overrideContext) {
        Matrix4f matrix = createProjection();
        if (overrideContext) {
            OVERRIDE_TASKS.add(() -> ArcOutline2D.draw(matrix, x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, Z_OVERRIDE));
            return;
        }
        ArcOutline2D.draw(matrix, x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, Z_OVERRIDE);
    }

    public static void arcOutline(Matrix4f matrix, float x, float y, float size, float arcThickness, float degree,
                                  float rotation, float outlineThickness, int fillColor, int outlineColor) {
        ArcOutline2D.draw(matrix, x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, Z_OVERRIDE);
    }

    public static void flushOverrideTasks() {
        for (Runnable task : OVERRIDE_TASKS) {
            task.run();
        }
        OVERRIDE_TASKS.clear();
    }

    public static boolean isInOverlayMode() {
        return inOverlayMode;
    }

    public static void cleanup() {
        OVERRIDE_TASKS.clear();
        Arc2D.shutdown();
        ArcOutline2D.shutdown();
    }
}