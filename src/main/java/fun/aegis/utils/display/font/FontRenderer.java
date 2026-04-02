package fun.aegis.utils.display.font;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.font.entry.DrawEntry;
import fun.aegis.utils.display.font.glyph.Glyph;
import fun.aegis.utils.display.font.glyph.GlyphMap;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.events.render.TextFactoryEvent;

import java.awt.*;

import static net.minecraft.client.render.VertexFormat.DrawMode.*;
import static net.minecraft.client.render.VertexFormats.*;

@Setter
@Accessors(chain = true)
public class FontRenderer implements QuickImports {
    private final Object2ObjectMap<Identifier, ObjectList<DrawEntry>> GLYPH_PAGE_CACHE = new Object2ObjectOpenHashMap<>();
    private final ObjectList<GlyphMap> maps = new ObjectArrayList<>();
    @Getter
    private Font font;

    public FontRenderer(Font font, float sizePx) {
        init(font, sizePx);
    }

    private void init(Font font, float sizePx) {
        this.font = font.deriveFont(sizePx * 2);
    }

    private GlyphMap generateMap(char from, char to) {
        GlyphMap glyphMap = new GlyphMap(from, to, this.font, randomIdentifier(), 5);
        maps.add(glyphMap);
        return glyphMap;
    }

    private Glyph locateGlyph(char glyph) {
        for (GlyphMap map : maps) {
            if (map.contains(glyph)) {
                return map.getGlyph(glyph);
            }
        }

        char base = (char) Calculate.floorNearestMulN(glyph, 128);
        return generateMap(base, (char) (base + 128)).getGlyph(glyph);
    }

    public void drawText(MatrixStack matrix, Text text, double x, double y) {
        StringBuilder sb = new StringBuilder();
        findStyle(sb,text);
        drawString(matrix, sb.toString(), x, y, ColorAssist.getText());
    }

    public void findStyle(StringBuilder sb, Text component) {
        Style style = component.getStyle();
        if (component.getSiblings().isEmpty()) {
            if (style.getColor() != null) sb.append(ColorAssist.formatting(style.getColor().getRgb()));
            sb.append(component.getString()).append(Formatting.RESET);
        } else component.getWithStyle(style).forEach(text -> findStyle(sb,text));
    }

    public void drawStringWithScroll(MatrixStack matrix, java.lang.String text, double x, double y, float width, int color) {
        java.lang.String separation = "  ·  ";
        float textWidth = getStringWidth(text + separation);

        if (textWidth - width < 10) {
            drawString(matrix,text,x,y,color);
            return;
        }

        drawString(matrix,text + separation + text, x - Calculate.textScrolling(textWidth), y, color);
    }

    public void drawString(MatrixStack matrix, java.lang.String text, double x, double y, int color) {
        TextFactoryEvent event = new TextFactoryEvent(text);
        EventManager.callEvent(event);
        text = event.getText();

        char[] chars = text.toCharArray();
        float xOffset = 0;
        float yOffset = 0;
        int lineStart = 0;
        StringBuilder stringColor = new StringBuilder();
        boolean colorFormat = false;
        boolean textColor = false;
        int clr = color;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '§') {
                colorFormat = true;
                continue;
            } else if (colorFormat) {
                colorFormat = false;
                char c1 = Character.toUpperCase(c);
                if (ColorAssist.colorCodes.containsKey(c1)) {
                    clr = new Color(ColorAssist.colorCodes.get(c1)).getRGB();
                } else if (c1 == 'R') {
                    clr = color;
                }
                continue;
            }

            if (c == '⏏') {
                if (textColor) {
                    clr = new Color(Integer.parseInt(stringColor.toString())).getRGB();
                    stringColor.setLength(0);
                }
                textColor = !textColor;
                continue;
            } else if (textColor) {
                stringColor.append(c);
                continue;
            }

            if (c == '\n') {
                yOffset += getStringHeight(text.substring(lineStart, i)) - 2;
                xOffset = 0;
                lineStart = i + 1;
                continue;
            }

            Glyph glyph = locateGlyph(c);
            if (glyph != null) {
                if (glyph.value() != ' ') {
                    Identifier i1 = glyph.owner().bindToTexture;
                    DrawEntry entry = new DrawEntry(xOffset, yOffset, clr, glyph);
                    GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList<>()).add(entry);
                }
                xOffset += glyph.width();
            }
        }

        if (!GLYPH_PAGE_CACHE.isEmpty()) drawGlyphs(matrix, x, y);
        GLYPH_PAGE_CACHE.clear();
    }

    public void drawGradientString(MatrixStack matrix, java.lang.String text, double x, double y, int colorStart, int colorEnd) {
        TextFactoryEvent event = new TextFactoryEvent(text);
        EventManager.callEvent(event);
        text = event.getText();

        char[] chars = text.toCharArray();
        float xOffset = 0;
        float yOffset = 0;
        int lineStart = 0;
        int textLength = text.length();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\n') {
                yOffset += getStringHeight(text.substring(lineStart, i)) - 2;
                xOffset = 0;
                lineStart = i + 1;
                continue;
            }
            Glyph glyph = locateGlyph(c);
            if (glyph != null) {
                if (glyph.value() != ' ') {
                    float t = (float) i / (textLength - 1);
                    int color = interpolateColor(colorStart, colorEnd, t);
                    Identifier i1 = glyph.owner().bindToTexture;
                    DrawEntry entry = new DrawEntry(xOffset, yOffset, color, glyph);
                    GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList<>()).add(entry);
                }
                xOffset += glyph.width();
            }
        }

        if (!GLYPH_PAGE_CACHE.isEmpty()) drawGlyphs(matrix, x, y);
        GLYPH_PAGE_CACHE.clear();
    }

    private void drawGlyphs(MatrixStack matrix, double x, double y) {
        matrix.push();
        matrix.translate(x, y - 3F, 0);
        matrix.scale(0.5F, 0.5F, 1);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        for (Identifier identifier : GLYPH_PAGE_CACHE.keySet()) {
            RenderSystem.setShaderTexture(0, identifier);
            BufferBuilder buffer = tessellator.begin(QUADS, POSITION_TEXTURE_COLOR);
            for (DrawEntry drawEntry : GLYPH_PAGE_CACHE.get(identifier)) {
                float x1 = drawEntry.atX();
                float y1 = drawEntry.atY();

                Glyph glyph = drawEntry.toDraw();
                GlyphMap glyphMap = glyph.owner();

                float width = glyph.width();
                float height = glyph.height();

                float u1 = (float) glyph.u() / glyphMap.width;
                float v1 = (float) glyph.v() / glyphMap.height;
                float u2 = (float) (glyph.u() + glyph.width()) / glyphMap.width;
                float v2 = (float) (glyph.v() + glyph.height()) / glyphMap.height;

                int color = drawEntry.color();

                buffer.vertex(matrix4f, x1 + 0, y1 + height, 0).texture(u1, v2).color(color);
                buffer.vertex(matrix4f, x1 + width, y1 + height, 0).texture(u2, v2).color(color);
                buffer.vertex(matrix4f, x1 + width, y1 + 0, 0).texture(u2, v1).color(color);
                buffer.vertex(matrix4f, x1 + 0, y1 + 0, 0).texture(u1, v1).color(color);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        RenderSystem.disableBlend();
        matrix.pop();
    }

    public void drawCenteredString(MatrixStack stack, java.lang.String s, double x, double y, int color) {
        drawString(stack, s, (int) (x - getStringWidth(s) / 2f), (float) y, color);
    }

    public float getStringWidth(Text text) {
        return text != null ? getStringWidth(text.getString()) : 0;
    }

    public float getStringWidth(java.lang.String text) {
        TextFactoryEvent event = new TextFactoryEvent(text);
        EventManager.callEvent(event);
        text = event.getText();

        float currentLine = 0;
        float maxPreviousLines = 0;
        boolean ignore = false;

        for (char c : text.toCharArray()) {
            if (ignore) {
                ignore = false;
                continue;
            } else if (c == '§') {
                ignore = true;
                continue;
            } else if (c == '\n') {
                maxPreviousLines = Math.max(currentLine, maxPreviousLines);
                currentLine = 0;
                continue;
            }

            Glyph glyph = locateGlyph(c);
            currentLine += glyph == null ? 0 : glyph.width();
        }

        return Math.max(currentLine, maxPreviousLines) / 2;
    }

    public float getStringHeight(Text text) {
        return getStringHeight(text.getString());
    }

    public float getStringHeight(java.lang.String text) {
        float currentLine = 0;
        float previous = 0;

        for (char c : (text.isEmpty() ? " " : text).toCharArray()) {
            if (c == '\n') {
                currentLine = (currentLine == 0 ? locateGlyph(' ').height() : currentLine);
                previous += currentLine;
                currentLine = 0;
                continue;
            }
            Glyph glyph = locateGlyph(c);
            currentLine = Math.max(glyph == null ? 0 : glyph.height(), currentLine);
        }

        return currentLine + previous;
    }

    public java.lang.String trimToWidth(java.lang.String text, int maxWidth, boolean backwards) {
        return backwards ? trimToWidthBackwards(text, maxWidth) : trimToWidth(text, maxWidth);
    }

    public java.lang.String trimToWidth(java.lang.String text, int maxWidth) {
        return text.substring(0, getTrimmedLength(text, maxWidth));
    }

    private int getTrimmedLength(java.lang.String text, int maxWidth) {
        WidthLimitingVisitor visitor = new WidthLimitingVisitor(maxWidth);
        visitForwards(text, visitor);
        return visitor.getLength();
    }

    public java.lang.String trimToWidthBackwards(java.lang.String text, int maxWidth) {
        MutableFloat widthLeft = new MutableFloat(maxWidth);
        MutableInt trimmedLength = new MutableInt(text.length());

        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            Glyph glyph = locateGlyph(c);
            if (glyph != null) {
                widthLeft.subtract(glyph.width());
                if (widthLeft.floatValue() < 0) {
                    trimmedLength.setValue(i + 1);
                    break;
                }
            }
        }

        return text.substring(trimmedLength.intValue());
    }

    private boolean visitForwards(java.lang.String text, CharacterVisitor visitor) {
        int length = text.length();

        for (int i = 0; i < length; ++i) {
            char c = text.charAt(i);
            if (!visitor.accept(i, c)) {
                return false;
            }
        }

        return true;
    }

    private int interpolateColor(int colorStart, int colorEnd, float t) {
        float startAlpha = (colorStart >> 24 & 255) / 255.0F;
        float startRed = (colorStart >> 16 & 255) / 255.0F;
        float startGreen = (colorStart >> 8 & 255) / 255.0F;
        float startBlue = (colorStart & 255) / 255.0F;

        float endAlpha = (colorEnd >> 24 & 255) / 255.0F;
        float endRed = (colorEnd >> 16 & 255) / 255.0F;
        float endGreen = (colorEnd >> 8 & 255) / 255.0F;
        float endBlue = (colorEnd & 255) / 255.0F;

        float alpha = startAlpha + t * (endAlpha - startAlpha);
        float red = startRed + t * (endRed - startRed);
        float green = startGreen + t * (endGreen - startGreen);
        float blue = startBlue + t * (endBlue - startBlue);

        return ((int) (alpha * 255.0F) << 24) | ((int) (red * 255.0F) << 16) | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
    }

    private class WidthLimitingVisitor implements CharacterVisitor {
        private float widthLeft;
        @Getter
        private int length;

        public WidthLimitingVisitor(float maxWidth) {
            this.widthLeft = maxWidth;
        }

        @Override
        public boolean accept(int index, char c) {
            Glyph glyph = locateGlyph(c);
            if (glyph != null) {
                widthLeft -= glyph.width();
                if (widthLeft >= 0) {
                    length = index + 1;
                    return true;
                }
            }
            return false;
        }

    }

    @FunctionalInterface
    public interface CharacterVisitor {
        boolean accept(int index, char codePoint);
    }

    @Contract(value = "-> new", pure = true)
    public static @NotNull Identifier randomIdentifier() {
        return Identifier.of("aegis", "temp/" + StringHelper.randomString(32));
    }

    @Contract(value = "_ -> new", pure = true)
    public static int @NotNull [] RGBIntToRGB(int in) {
        int red = in >> 8 * 2 & 0xFF;
        int green = in >> 8 & 0xFF;
        int blue = in & 0xFF;
        return new int[]{red, green, blue};
    }
}
