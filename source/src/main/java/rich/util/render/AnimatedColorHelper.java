package rich.util.render;

import net.minecraft.util.math.MathHelper;
import rich.util.ColorUtil;

import java.awt.*;

/**
 * утиль для переливов
 */
public class AnimatedColorHelper {

    /**
     * режимы цвета
     */
    public enum AnimationMode {
        /** градиент туда сюда */
        PING_PONG,
        /** градиент по кругу */
        CIRCULAR,
        /** пульс одним цветом */
        PULSE,
        /** рандом между цветами */
        RANDOM
    }

    /**
     * градиент для аутлайна
     */
    public static int[] getAnimatedOutlineGradient(int color1, int color2, float speed, long time, float progress, AnimationMode mode) {
        int[] colors = new int[4];

        switch (mode) {
            case PING_PONG -> {
                float angle = (float) ((Math.sin(time * 0.001 * speed) + 1) / 2.0);
                float offset = angle * progress;

                for (int i = 0; i < 4; i++) {
                    float t = (i / 3.0f + offset) % 1.0f;
                    colors[i] = ColorUtil.interpolateColor(color1, color2, t);
                }
            }
            case CIRCULAR -> {
                float angle = (float) ((time * 0.001 * speed) % (Math.PI * 2));

                for (int i = 0; i < 4; i++) {
                    float cornerAngle = angle + (i * (float)Math.PI / 2.0f);
                    float t = (float) ((Math.sin(cornerAngle) + 1.0) / 2.0);
                    colors[i] = ColorUtil.interpolateColor(color1, color2, t);
                }
            }
            case PULSE -> {
                float pulse = (float) ((Math.sin(time * 0.002 * speed) + 1) / 2.0);
                int animated = ColorUtil.interpolateColor(color1, color2, pulse);
                for (int i = 0; i < 4; i++) {
                    colors[i] = animated;
                }
            }
            case RANDOM -> {
                long seed = (long) (time * 0.001 * speed);
                for (int i = 0; i < 4; i++) {
                    float t = (float) Math.abs(Math.sin(seed + i * 127.1)) ;
                    colors[i] = ColorUtil.interpolateColor(color1, color2, t);
                }
            }
        }

        return colors;
    }

    /**
     * один цвет для аутлайна
     */
    public static int getAnimatedOutlineColor(int baseColor, float speed, long time, AnimationMode mode) {
        switch (mode) {
            case PING_PONG, CIRCULAR -> {
                float pulse = (float) ((Math.sin(time * 0.002 * speed) + 1) / 2.0);
                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;

                float brighten = 0.8f + pulse * 0.4f;
                r = Math.min(255, (int) (r * brighten));
                g = Math.min(255, (int) (g * brighten));
                b = Math.min(255, (int) (b * brighten));

                int alpha = (baseColor >> 24) & 0xFF;
                return (alpha << 24) | (r << 16) | (g << 8) | b;
            }
            case PULSE -> {
                float pulse = (float) ((Math.sin(time * 0.003 * speed) + 1) / 2.0);
                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;
                int alpha = (baseColor >> 24) & 0xFF;

                float factor = 0.7f + pulse * 0.6f;
                r = Math.min(255, (int) (r * factor));
                g = Math.min(255, (int) (g * factor));
                b = Math.min(255, (int) (b * factor));

                return (alpha << 24) | (r << 16) | (g << 8) | b;
            }
            case RANDOM -> {
                long seed = (long) (time * 0.001 * speed);
                float t = (float) Math.abs(Math.sin(seed * 127.1));
                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;
                int alpha = (baseColor >> 24) & 0xFF;

                int variation = (int) (40 * t);
                r = Math.min(255, Math.max(0, r + variation - 20));
                g = Math.min(255, Math.max(0, g + variation - 20));
                b = Math.min(255, Math.max(0, b + variation - 20));

                return (alpha << 24) | (r << 16) | (g << 8) | b;
            }
            default -> {
                return baseColor;
            }
        }
    }

    /**
     * градиент для прямоугольника
     */
    public static int[] getAnimatedRectGradient(int color1, int color2, float speed, long time, AnimationMode mode) {
        int[] colors = new int[9];

        switch (mode) {
            case PING_PONG -> {
                float wave = (float) ((Math.sin(time * 0.001 * speed) + 1) / 2.0);

                for (int i = 0; i < 9; i++) {
                    int row = i / 3;
                    int col = i % 3;
                    float t = wave * ((row + col) / 4.0f);
                    colors[i] = ColorUtil.interpolateColor(color1, color2, t);
                }
            }
            case CIRCULAR -> {
                float angle = (float) (time * 0.001 * speed);

                for (int i = 0; i < 9; i++) {
                    int row = i / 3;
                    int col = i % 3;
                    float cornerAngle = angle + (row + col) * 0.5f;
                    float t = (float) ((Math.sin(cornerAngle) + 1) / 2.0);
                    colors[i] = ColorUtil.interpolateColor(color1, color2, t);
                }
            }
            case PULSE -> {
                float pulse = (float) ((Math.sin(time * 0.002 * speed) + 1) / 2.0);
                int animated = ColorUtil.interpolateColor(color1, color2, pulse);
                for (int i = 0; i < 9; i++) {
                    colors[i] = animated;
                }
            }
            case RANDOM -> {
                long seed = (long) (time * 0.001 * speed);
                for (int i = 0; i < 9; i++) {
                    float t = (float) Math.abs(Math.sin(seed + i * 127.1));
                    colors[i] = ColorUtil.interpolateColor(color1, color2, t);
                }
            }
        }

        return colors;
    }

    /**
     * радуга для аутлайна
     */
    public static int[] getRainbowOutlineGradient(float speed, long time, float saturation, float brightness, int alpha) {
        int[] colors = new int[4];
        for (int i = 0; i < 4; i++) {
            float hue = ((time * 0.001f * speed + i * 0.25f) % 1.0f);
            colors[i] = ColorUtil.hsvToRgb(hue, saturation, brightness, alpha / 255.0f);
        }
        return colors;
    }

    /**
     * радуга для прямоугольника
     */
    public static int[] getRainbowRectGradient(float speed, long time, float saturation, float brightness, int alpha) {
        int[] colors = new int[4];
        for (int i = 0; i < 4; i++) {
            float hue = ((time * 0.001f * speed + i * 0.25f) % 1.0f);
            colors[i] = ColorUtil.hsvToRgb(hue, saturation, brightness, alpha / 255.0f);
        }
        return colors;
    }

    /**
     * пульс яркостью
     */
    public static int getPulsingColor(int baseColor, float speed, long time) {
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int alpha = (baseColor >> 24) & 0xFF;

        float pulse = (float) ((Math.sin(time * 0.003 * speed) + 1) / 2.0);
        float brightness = 0.75f + pulse * 0.5f;

        r = Math.min(255, (int) (r * brightness));
        g = Math.min(255, (int) (g * brightness));
        b = Math.min(255, (int) (b * brightness));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * волна через цвета
     */
    public static int[] getWaveGradient(int color1, int color2, int color3, float speed, long time) {
        int[] colors = new int[4];
        float t = (float) ((time * 0.001f * speed) % 3.0f);

        for (int i = 0; i < 4; i++) {
            float localT = (t + i * 0.5f) % 3.0f;
            if (localT < 1.0f) {
                colors[i] = ColorUtil.interpolateColor(color1, color2, localT);
            } else if (localT < 2.0f) {
                colors[i] = ColorUtil.interpolateColor(color2, color3, localT - 1.0f);
            } else {
                colors[i] = ColorUtil.interpolateColor(color3, color1, localT - 2.0f);
            }
        }
        return colors;
    }

    /**
     * цвет для трассеров
     */
    public static int getTracerColor(int color1, int color2, float speed, long time, float entityProgress) {
        float animT = (float) ((Math.sin(time * 0.002 * speed) + 1) / 2.0);
        int baseColor = ColorUtil.interpolateColor(color1, color2, animT);

        float brightness = 0.8f + entityProgress * 0.4f;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int alpha = (baseColor >> 24) & 0xFF;

        r = Math.min(255, (int) (r * brightness));
        g = Math.min(255, (int) (g * brightness));
        b = Math.min(255, (int) (b * brightness));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * мерцалка шиммер
     */
    public static int getShimmerColor(int baseColor, float speed, long time, float intensity) {
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int alpha = (baseColor >> 24) & 0xFF;

        float shimmer = (float) Math.sin(time * 0.005 * speed) * intensity;
        int shimmerValue = (int) (shimmer * 50);

        r = Math.min(255, Math.max(0, r + shimmerValue));
        g = Math.min(255, Math.max(0, g + shimmerValue));
        b = Math.min(255, Math.max(0, b + shimmerValue));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
}
