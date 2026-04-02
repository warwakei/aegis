package rich.util.color;

import com.mojang.blaze3d.systems.RenderSystem;
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

@Getter
@UtilityClass
public class ColorAssist {
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

    public int[] getRGB(int c) {
        return new int[]{red(c), green(c), blue(c)};
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

    public static int applyOpacity(int hex, float opacity) {
        return ColorHelper.getArgb((int) (ColorHelper.getAlpha(hex) * (opacity / 255)), ColorHelper.getRed(hex), ColorHelper.getGreen(hex), ColorHelper.getBlue(hex));
    }

    public static int calculateHuyDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long calculatedValue = (currentTime / divisor + offset) % 360L;
        return (int) calculatedValue;
    }

    public static int reAlphaInt(final int color, final int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }

    public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
        float hueStep = 360.0f / 4.0f;
        float basaHuy = (float) calculateHuyDegrees(speed, index);
        float huy = (basaHuy + index * hueStep) % 360.0f;

        huy = huy / 360.0f;

        saturation = Math.clamp(saturation, 0.0f, 1.0f);
        brightness = Math.clamp(brightness, 0.0f, 1.0f);

        int rgb = Color.HSBtoRGB(huy, saturation, brightness);
        int Ialpha = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));

        return reAlphaInt(rgb, Ialpha);
    }

    public static int toColor(String hexColor) {
        int argb = Integer.parseInt(hexColor.substring(1), 16);
        return setAlpha(argb, 255);
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
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

    public int overCol(int color1, int color2, float percent01) {
        final float percent = MathHelper.clamp(percent01, 0F, 1F);
        return getColor(
                MathHelper.lerp(percent, red(color1), red(color2)),
                MathHelper.lerp(percent, green(color1), green(color2)),
                MathHelper.lerp(percent, blue(color1), blue(color2)),
                MathHelper.lerp(percent, alpha(color1), alpha(color2))
        );
    }

    public int multRedAndAlpha(int color, float red, float alpha) {
        return getColor(red(color),Math.min(255, Math.round(green(color) / red)), Math.min(255, Math.round(blue(color) / red)), Math.round(alpha(color) * alpha));
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

}