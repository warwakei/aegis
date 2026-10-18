package rich.util.render.font;

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
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class FontPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("rich", "pipeline/msdf");
    private static final Identifier SHADER_ID = Identifier.of("rich", "core/msdf");

    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(SHADER_ID)
                    .withFragmentShader(SHADER_ID)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("FontData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final int[] LEGACY_COLORS = new int[32];

    static {
        for (int i = 0; i < 16; ++i) {
            int j = (i >> 3 & 1) * 85;
            int r = (i >> 2 & 1) * 170 + j;
            int g = (i >> 1 & 1) * 170 + j;
            int b = (i & 1) * 170 + j;
            if (i == 6) r += 85;
            LEGACY_COLORS[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            LEGACY_COLORS[i + 16] = ((r & 0xFCFCFC) >> 2 << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static final int MAX_CHARS = 256;
    private static final int BUFFER_SIZE = 64 + MAX_CHARS * 64;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    private final List<CharData> charBatch = new ArrayList<>();
    private FontAtlas currentAtlas = null;
    private float currentOutlineWidth = 0;
    private int currentOutlineColor = 0;

    private static class CharData {
        float x, y, width, height;
        float u0, v0, u1, v1;
        int color;
        float rotation;
        float pivotX, pivotY;
        float glyphScale;

        CharData(float x, float y, float w, float h, float u0, float v0, float u1, float v1,
                 int color, float rotation, float pivotX, float pivotY, float glyphScale) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.color = color;
            this.rotation = rotation;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.glyphScale = glyphScale;
        }
    }

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
                () -> "rich:font_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData);
        MemoryUtil.memFree(dummyData);

        initialized = true;
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color) {
        drawText(atlas, text, x, y, size, color, 0, 0, 0);
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color,
                         float outlineWidth, int outlineColor, float rotation) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth
                || currentOutlineColor != outlineColor)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        float cursorX = x;
        float cursorY = y;

        float textWidth = getTextWidth(atlas, text, size);
        float textHeight = getTextHeight(atlas, text, size);
        float pivotX = x + textWidth / 2;
        float pivotY = y + textHeight / 2;

        float rotationRad = (float) Math.toRadians(rotation);

        int currentColor = color;

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if ((codePoint == '§' || codePoint == '&') && i + charCount < text.length()) {
                int nextCodePoint = text.codePointAt(i + charCount);
                if (nextCodePoint == '#' && i + charCount + 6 < text.length()) {
                    try {
                        String hex = text.substring(i + charCount + 1, i + charCount + 7);
                        currentColor = (0xFF << 24) | Integer.parseInt(hex, 16);
                        i += charCount + 7;
                        continue;
                    } catch (Exception ignored) {}
                }
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase((char) nextCodePoint));
                if (code >= 0) {
                    if (code < 16) {
                        currentColor = LEGACY_COLORS[code];
                    } else if (code == 21) {
                        currentColor = color;
                    }
                    i += charCount + Character.charCount(nextCodePoint);
                    continue;
                }
            }

            if (codePoint == '\n') {
                cursorX = x;
                cursorY += atlas.getLineHeight() * scale;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph == null) {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    cursorX += fallback.xAdvance * scale;
                } else {
                    cursorX += size * 0.5f;
                }
                i += charCount;
                continue;
            }

            float glyphX = cursorX + glyph.xOffset * scale;
            float glyphY = cursorY + glyph.yOffset * scale;
            float glyphW = glyph.width * scale;
            float glyphH = glyph.height * scale;

            if (glyph.width > 0 && glyph.height > 0) {
                charBatch.add(new CharData(
                        glyphX, glyphY, glyphW, glyphH,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        currentColor, rotationRad, pivotX, pivotY, scale));
            }

            cursorX += glyph.xAdvance * scale;

            if (charBatch.size() >= MAX_CHARS) {
                flush();
            }

            i += charCount;
        }

        if (!charBatch.isEmpty() && currentAtlas != null) {
            flush();
        }
    }

    public void drawTextRotatedAroundPoint(FontAtlas atlas, String text, float x, float y, float size, int color,
                                           float outlineWidth, int outlineColor, float rotation, float pivotX, float pivotY) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth
                || currentOutlineColor != outlineColor)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        float cursorX = x;
        float cursorY = y;

        float rotationRad = (float) Math.toRadians(rotation);

        int currentColor = color;

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if ((codePoint == '§' || codePoint == '&') && i + charCount < text.length()) {
                int nextCodePoint = text.codePointAt(i + charCount);
                if (nextCodePoint == '#' && i + charCount + 6 < text.length()) {
                    try {
                        String hex = text.substring(i + charCount + 1, i + charCount + 7);
                        currentColor = (0xFF << 24) | Integer.parseInt(hex, 16);
                        i += charCount + 7;
                        continue;
                    } catch (Exception ignored) {}
                }
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase((char) nextCodePoint));
                if (code >= 0) {
                    if (code < 16) {
                        currentColor = LEGACY_COLORS[code];
                    } else if (code == 21) {
                        currentColor = color;
                    }
                    i += charCount + Character.charCount(nextCodePoint);
                    continue;
                }
            }

            if (codePoint == '\n') {
                cursorX = x;
                cursorY += atlas.getLineHeight() * scale;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph == null) {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    cursorX += fallback.xAdvance * scale;
                } else {
                    cursorX += size * 0.5f;
                }
                i += charCount;
                continue;
            }

            float glyphX = cursorX + glyph.xOffset * scale;
            float glyphY = cursorY + glyph.yOffset * scale;
            float glyphW = glyph.width * scale;
            float glyphH = glyph.height * scale;

            if (glyph.width > 0 && glyph.height > 0) {
                charBatch.add(new CharData(
                        glyphX, glyphY, glyphW, glyphH,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        currentColor, rotationRad, pivotX, pivotY, scale));
            }

            cursorX += glyph.xAdvance * scale;

            if (charBatch.size() >= MAX_CHARS) {
                flush();
            }

            i += charCount;
        }

        if (!charBatch.isEmpty() && currentAtlas != null) {
            flush();
        }
    }

    public void flush() {
        if (charBatch.isEmpty() || currentAtlas == null) {
            charBatch.clear();
            currentAtlas = null;
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) {
            charBatch.clear();
            currentAtlas = null;
            return;
        }

        AbstractTexture texture = client.getTextureManager().getTexture(currentAtlas.getTextureId());
        if (texture == null) {
            charBatch.clear();
            currentAtlas = null;
            return;
        }

        GpuTexture gpuTexture;
        try {
            gpuTexture = texture.getGlTexture();
        } catch (Exception e) {
            charBatch.clear();
            currentAtlas = null;
            return;
        }

        prepareUniformData(client, currentAtlas, currentOutlineWidth, currentOutlineColor);

        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) uniformBuffer.close();
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "rich:font_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size);
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuTextureView textureView = RenderSystem.getDevice().createTextureView(gpuTexture);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "rich:font_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("Sampler0", textureView, sampler);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("FontData", uniformBuffer);

            renderPass.draw(0, charBatch.size() * 6);
        }

        textureView.close();
        charBatch.clear();
        currentAtlas = null;
    }

    private void prepareUniformData(MinecraftClient client, FontAtlas atlas, float outlineWidth, int outlineColor) {
        dataBuffer.clear();

        float screenWidth = getFixedScaledWidth();
        float screenHeight = getFixedScaledHeight();
        float guiScale = FIXED_GUI_SCALE;

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(guiScale);
        dataBuffer.putFloat(outlineWidth);

        float oa = ((outlineColor >> 24) & 0xFF) / 255.0f;
        float or = ((outlineColor >> 16) & 0xFF) / 255.0f;
        float og = ((outlineColor >> 8) & 0xFF) / 255.0f;
        float ob = (outlineColor & 0xFF) / 255.0f;
        dataBuffer.putFloat(or);
        dataBuffer.putFloat(og);
        dataBuffer.putFloat(ob);
        dataBuffer.putFloat(oa);

        dataBuffer.putFloat(atlas.getAtlasWidth());
        dataBuffer.putFloat(atlas.getAtlasHeight());
        dataBuffer.putFloat(atlas.getDistanceRange());
        dataBuffer.putFloat(atlas.getFontSize());

        dataBuffer.putInt(charBatch.size());
        dataBuffer.putInt(0);
        dataBuffer.putInt(0);
        dataBuffer.putInt(0);

        for (CharData cd : charBatch) {
            dataBuffer.putFloat(cd.x);
            dataBuffer.putFloat(cd.y);
            dataBuffer.putFloat(cd.width);
            dataBuffer.putFloat(cd.height);

            dataBuffer.putFloat(cd.u0);
            dataBuffer.putFloat(cd.v0);
            dataBuffer.putFloat(cd.u1);
            dataBuffer.putFloat(cd.v1);

            float a = ((cd.color >> 24) & 0xFF) / 255.0f;
            float r = ((cd.color >> 16) & 0xFF) / 255.0f;
            float g = ((cd.color >> 8) & 0xFF) / 255.0f;
            float b = (cd.color & 0xFF) / 255.0f;
            dataBuffer.putFloat(r);
            dataBuffer.putFloat(g);
            dataBuffer.putFloat(b);
            dataBuffer.putFloat(a);

            dataBuffer.putFloat(cd.rotation);
            dataBuffer.putFloat(cd.pivotX);
            dataBuffer.putFloat(cd.pivotY);
            dataBuffer.putFloat(cd.glyphScale);
        }

        dataBuffer.flip();
    }

    public float getTextWidth(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();
        float width = 0;
        float maxWidth = 0;

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if ((codePoint == '§' || codePoint == '&') && i + charCount < text.length()) {
                int nextCodePoint = text.codePointAt(i + charCount);
                if (nextCodePoint == '#' && i + charCount + 6 < text.length()) {
                    i += charCount + 7;
                    continue;
                }
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase((char) nextCodePoint));
                if (code >= 0) {
                    i += charCount + Character.charCount(nextCodePoint);
                    continue;
                }
            }

            if (codePoint == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph != null) {
                width += glyph.xAdvance * scale;
            } else {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    width += fallback.xAdvance * scale;
                } else {
                    width += size * 0.5f;
                }
            }

            i += charCount;
        }

        return Math.max(maxWidth, width);
    }

    public float getTextHeight(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return lines * atlas.getLineHeight() * scale;
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