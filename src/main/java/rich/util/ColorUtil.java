package rich.util;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import rich.util.math.MathUtils;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 *  © 2025 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

@Getter
@UtilityClass
public class ColorUtil {
    public static int colorForRectsCustom$() {
        return new Color(91, 63, 212,255).getRGB();
    }
    public static int colorForRectsBlack$() {
        return new Color(26, 26, 26,255).getRGB();
    }
    public static int colorForTextWhite$() {
        return new Color(255,255,255,255).getRGB();
    }
    public static int colorForTextCustom$() {
        return  new Color(130, 100, 210,255).getRGB();
    }
    public static final int green = new Color(64, 255, 64).getRGB();
    public static final int yellow = new Color(255, 255, 64).getRGB();
    public static final int orange = new Color(255, 128, 32).getRGB();
    public static final int red = new Color(255, 64, 64).getRGB();

    private final long CACHE_EXPIRATION_TIME = 60 * 1000;
    private final ConcurrentHashMap<ColorKey, CacheEntry> colorCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheCleaner = Executors.newScheduledThreadPool(1);
    private final DelayQueue<CacheEntry> cleanupQueue = new DelayQueue<>();
    public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");
    public Char2IntArrayMap colorCodes = new Char2IntArrayMap() {{
        put('0', 0x000000);
        put('1', 0x0000AA);
        put('2', 0x00AA00);
        put('3', 0x00AAAA);
        put('4', 0xAA0000);
        put('5', 0xAA00AA);
        put('6', 0xFFAA00);
        put('7', 0xAAAAAA);
        put('8', 0x555555);
        put('9', 0x5555FF);
        put('A', 0x55FF55);
        put('B', 0x55FFFF);
        put('C', 0xFF5555);
        put('D', 0xFF55FF);
        put('E', 0xFFFF55);
        put('F', 0xFFFFFF);
    }};

    static {
        cacheCleaner.scheduleWithFixedDelay(() -> {
            CacheEntry entry = cleanupQueue.poll();
            while (entry != null) {
                if (entry.isExpired()) {
                    colorCache.remove(entry.getKey());
                }
                entry = cleanupQueue.poll();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public final int RED = getColor(255, 0, 0);
    public final int GREEN = getColor(0, 255, 0);
    public final int BLUE = getColor(0, 0, 255);
    public final int YELLOW = getColor(255, 255, 0);
    public final int WHITE = getColor(255);
    public final int BLACK = getColor(0);
    public final int HALF_BLACK = getColor(0,0.5F);
    public final int LIGHT_RED = getColor(255, 85, 85);

    public static int applyAlpha(int color, int alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int originalAlpha = (color >> 24) & 0xFF;
        int newAlpha = (originalAlpha * alpha) / 255;
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    public static int applyAlpha(int color, float alpha) {
        return applyAlpha(color, (int)(alpha * 255));
    }

    public int red(int c) {return (c >> 16) & 0xFF;}

    public int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public int blue(int c) {
        return c & 0xFF;
    }

    public int alpha(int c) {
        return (c >> 24) & 0xFF;
    }

    public float redf(int c) {
        return red(c) / 255.0f;
    }

    public float greenf(int c) {
        return green(c) / 255.0f;
    }

    public float bluef(int c) {
        return blue(c) / 255.0f;
    }

    public float alphaf(int c) {
        return alpha(c) / 255.0f;
    }

    public int[] getRGBA(int c) {
        return new int[]{red(c), green(c), blue(c), alpha(c)};
    }

    public int[] getRGB(int c) {
        return new int[]{red(c), green(c), blue(c)};
    }

    public float[] getRGBAf(int c) {
        return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
    }

    public float[] getRGBf(int c) {
        return new float[]{redf(c), greenf(c), bluef(c)};
    }

    public int getColor(float red, float green, float blue, float alpha) {
        return getColor(Math.round(red * 255), Math.round(green * 255), Math.round(blue * 255), Math.round(alpha * 255));
    }

    public int getColor(int red, int green, int blue, float alpha) {
        return getColor(red, green, blue, Math.round(alpha * 255));
    }

    public int getColor(float red, float green, float blue) {
        return getColor(red, green, blue, 1.0F);
    }

    public int getColor(int brightness, int alpha) {
        return getColor(brightness, brightness, brightness, alpha);
    }

    public int getColor(int brightness, float alpha) {
        return getColor(brightness, Math.round(alpha * 255));
    }

    public int getColor(int brightness) {
        return getColor(brightness, brightness, brightness);
    }

    public int replAlpha(int color, int alpha) {
        return getColor(red(color), green(color), blue(color), alpha);
    }

    public int replAlpha(int color, float alpha) {
        return getColor(red(color), green(color), blue(color), alpha);
    }

    public int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }
    public static int applyOpacity(int hex, int percent) {
        return applyOpacity(hex, 2.55f * Math.min(percent, 100));
    }

    public static int applyOpacity(int hex, float opacity) {
        return ColorHelper.getArgb((int) (ColorHelper.getAlpha(hex) * (opacity / 255)), ColorHelper.getRed(hex), ColorHelper.getGreen(hex), ColorHelper.getBlue(hex));
    }

    public static int lerp(float value, int from, int to) {
        return ColorHelper.lerp(value, from, to);
    }

    public static int pixelColor(int x, int y) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);

        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

        return ColorHelper.getArgb(getRed(byteBuffer.get()), getGreen(byteBuffer.get()), getBlue(byteBuffer.get()));
    }

    public static int MathUtilHuyDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long MathUtildValue = (currentTime / divisor + offset) % 360L;
        return (int) MathUtildValue;
    }

    public static int reAlphaInt(final int color, final int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }

    public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
        float hueStep = 360.0f / 4.0f;
        float basaHuy = (float) MathUtilHuyDegrees(speed, index);
        float huy = (basaHuy + index * hueStep) % 360.0f;

        huy = huy / 360.0f;

        saturation = Math.clamp(saturation, 0.0f, 1.0f);
        brightness = Math.clamp(brightness, 0.0f, 1.0f);

        int rgb = Color.HSBtoRGB(huy, saturation, brightness);
        int Ialpha = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));

        return reAlphaInt(rgb, Ialpha);
    }

    public static class IntColor {

        public static float[] rgb(final int color) {
            return new float[]{
                    (color >> 16 & 0xFF) / 255f,
                    (color >> 8 & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    (color >> 24 & 0xFF) / 255f
            };
        }

        public static int rgba(final int r,
                               final int g,
                               final int b,
                               final int a) {
            return a << 24 | r << 16 | g << 8 | b;
        }

        public static int rgb(int r, int g, int b) {
            return 255 << 24 | r << 16 | g << 8 | b;
        }


        public static int getRed(final int hex) {
            return hex >> 16 & 255;
        }

        public static int getGreen(final int hex) {
            return hex >> 8 & 255;
        }

        public static int getBlue(final int hex) {
            return hex & 255;
        }

        public static int getAlpha(final int hex) {
            return hex >> 24 & 255;
        }

    }

    public static int rgb(int r, int g, int b) {
        return 255 << 24 | r << 16 | g << 8 | b;
    }

    public static int toColor(String hexColor) {
        int argb = Integer.parseInt(hexColor.substring(1), 16);
        return setAlpha(argb, 255);
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static int gradient(int start, int end, int index, int speed) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int color = interpolate(start, end, Math.clamp(angle / 180f - 1, 0, 1));
        float[] hs = rgba(color);
        float[] hsb = Color.RGBtoHSB((int) (hs[0] * 255), (int) (hs[1] * 255), (int) (hs[2] * 255), null);

        hsb[1] *= 1.5F;
        hsb[1] = Math.min(hsb[1], 1.0f);

        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolate(color1, color2, angle / 360f * colors.length - colorIndex);
    }


    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = interpolateInt(red1, red2, amount);
        int interpolatedGreen = interpolateInt(green1, green2, amount);
        int interpolatedBlue = interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }

    public static Double interpolateD(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolateD(oldValue, newValue, (float) interpolationValue).intValue();
    }

    public int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int getRed(final int hex) {
        return hex >> 16 & 255;
    }

    public static int withAlphahud(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int withAlpha(int color, float alpha) {
        return withAlphahud(color, (int) (alpha * 255));
    }

    public static int getGreen(final int hex) {
        return hex >> 8 & 255;
    }

    public static int getBlue(final int hex) {
        return hex & 255;
    }

    public static int getAlpha(final int hex) {
        return hex >> 24 & 255;
    }

    public int multColor(int colorStart, int colorEnd, float progress) {
        return getColor(Math.round(red(colorStart) * (redf(colorEnd) * progress)), Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
                Math.round(blue(colorStart) * (bluef(colorEnd) * progress)), Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress)));
    }

    public int multRed(int colorStart, int colorEnd, float progress) {
        return getColor(Math.round(red(colorStart) * (redf(colorEnd) * progress)), Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
                Math.round(blue(colorStart) * (bluef(colorEnd) * progress)), Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress)));
    }

    public int multDark(int color, float percent01) {
        return getColor(
                Math.round(red(color) * percent01),
                Math.round(green(color) * percent01),
                Math.round(blue(color) * percent01),
                alpha(color)
        );
    }

    public int multBright(int color, float percent01) {
        return getColor(
                Math.min(255, Math.round(red(color) / percent01)),
                Math.min(255, Math.round(green(color) / percent01)),
                Math.min(255, Math.round(blue(color) / percent01)),
                alpha(color)
        );
    }

    public int overCol(int color1, int color2, float percent01) {
        final float percent = MathHelper.clamp(percent01, 0F, 1F);
        return getColor(
                MathHelper.lerp(percent, red(color1), red(color2)),
                MathHelper.lerp(percent, green(color1), green(color2)),
                MathHelper.lerp(percent, blue(color1), blue(color2)),
                MathHelper.lerp(percent, alpha(color1), alpha(color2))
        );
    }

    public Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
        return new Vector4i(multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha));
    }

    public int multRedAndAlpha(int color, float red, float alpha) {
        return getColor(red(color),Math.min(255, Math.round(green(color) / red)), Math.min(255, Math.round(blue(color) / red)), Math.round(alpha(color) * alpha));
    }

    public int multRed(int color, float percent01) {
        return getColor(red(color),Math.min(255, Math.round(green(color) / percent01)), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
    }

    public int multGreen(int color, float percent01) {
        return getColor(Math.min(255, Math.round(green(color) / percent01)), green(color), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
    }

    public int rgba(int red, int green, int blue, int alpha) {
        return getColor(red, green, blue, alpha);
    }

    public int[] genGradientForText(int color1, int color2, int length) {
        int[] gradient = new int[length];
        for (int i = 0; i < length; i++) {
            float pc = (float) i / (length - 1);
            gradient[i] = overCol(color1, color2, pc);
        }
        return gradient;
    }

    public static float[] rgba(final int color) {
        return new float[]{
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static int interpolate(int start, int end, float value) {
        float[] startColor = rgba(start);
        float[] endColor = rgba(end);

        return rgba((int) MathUtils.interpolate(startColor[0] * 255, endColor[0] * 255, value),
                (int) MathUtils.interpolate(startColor[1] * 255, endColor[1] * 255, value),
                (int) MathUtils.interpolate(startColor[2] * 255, endColor[2] * 255, value),
                (int) MathUtils.interpolate(startColor[3] * 255, endColor[3] * 255, value));
    }

    public static int[] solid(int color) {
        return new int[]{color, color, color, color, color, color, color, color, color};
    }

    public static int[] solid8(int color) {
        return new int[]{color, color, color, color, color, color, color, color};
    }

    public int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float hue = angle / 360f;
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return getColor(red(color), green(color), blue(color), Math.round(opacity * 255));
    }

    public int fade(int speed, int index, int first, int second) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = angle >= 180 ? 360 - angle : angle;
        return overCol(first, second, angle / 180f);
    }

    public int rgbaFloat(float r, float g, float b, float a) {
        return (int) (MathHelper.clamp(a,0,1) * 255) << 24 | (int) (MathHelper.clamp(r,0,1) * 255) << 16 | (int) (MathHelper.clamp(g,0,1) * 255) << 8 | (int) (MathHelper.clamp(b,0,1) * 255);
    }

    public int getColor(int red, int green, int blue, int alpha) {
        ColorKey key = new ColorKey(red, green, blue, alpha);
        CacheEntry cacheEntry = colorCache.computeIfAbsent(key, k -> {
            CacheEntry newEntry = new CacheEntry(k, computeColor(red, green, blue, alpha), CACHE_EXPIRATION_TIME);
            cleanupQueue.offer(newEntry);
            return newEntry;
        });
        return cacheEntry.getColor();
    }

    public int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    private int computeColor(int red, int green, int blue, int alpha) {
        return ((MathHelper.clamp(alpha, 0, 255) << 24) |
                (MathHelper.clamp(red, 0, 255) << 16) |
                (MathHelper.clamp(green, 0, 255) << 8) |
                MathHelper.clamp(blue, 0, 255));
    }

    private String generateKey(int red, int green, int blue, int alpha) {
        return red + "," + green + "," + blue + "," + alpha;
    }

    public String formatting(int color) {
        return "⏏" + color + "⏏";
    }

    public int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;

        float r, g, b;
        if (h < 1f/6f) { r = c; g = x; b = 0; }
        else if (h < 2f/6f) { r = x; g = c; b = 0; }
        else if (h < 3f/6f) { r = 0; g = c; b = x; }
        else if (h < 4f/6f) { r = 0; g = x; b = c; }
        else if (h < 5f/6f) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        int ri = (int)((r + m) * 255);
        int gi = (int)((g + m) * 255);
        int bi = (int)((b + m) * 255);

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    public static int hsvToRgb(float h, float s, float v, float alpha) {
        int rgb = hsvToRgb(h, s, v);
        int a = (int)(alpha * 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class ColorKey {
        final int red, green, blue, alpha;
    }

    @Getter
    private static class CacheEntry implements Delayed {
        private final ColorKey key;
        private final int color;
        private final long expirationTime;

        CacheEntry(ColorKey key, int color, long ttl) {
            this.key = key;
            this.color = color;
            this.expirationTime = System.currentTimeMillis() + ttl;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = expirationTime - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof CacheEntry) {
                return Long.compare(this.expirationTime, ((CacheEntry) other).expirationTime);
            }
            return 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

    }

    public String removeFormatting(String text) {
        return text == null || text.isEmpty() ? null : FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }

    public int getMainGuiColor() {return new Color(20, 20, 24, 255).getRGB();}

    public int getGuiRectColor(float alpha) {return multAlpha(new Color(0x1A1A1F).getRGB(),alpha);}
    public int getGuiRectColor2(float alpha) {return multAlpha(new Color(0x1E1E26).getRGB(),alpha);}

    public int getRect(float alpha) {return multAlpha(new Color(0,0,0,228).getRGB(),alpha);}

    public int getRectDarker(float alpha) {
        return multAlpha(new Color(0x18181E).getRGB(),alpha);
    }

    public int getText(float alpha) {return multAlpha(getText(),alpha);}

    public int getText() {return new Color(255,255,255,255).getRGB();}

    public int getText2() {return new Color(175, 175, 175,255).getRGB();}

    public int getFriendColor() {
        return new Color(0x55FF55).getRGB();
    }

    public int getOutline(float alpha, float bright) {return multBright(multAlpha(getOutline(),alpha),bright);}

    public int getOutline(float alpha) {return multAlpha(getOutline(), alpha);}

    public int getOutline() {
        return new Color(0x373746).getRGB();
    }
}