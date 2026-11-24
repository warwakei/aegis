package rich.screens.clickgui.impl.feedback;

import net.minecraft.client.gui.DrawContext;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Тосты внутри ClickGui: включение модуля, бинды, избранное.
 * Живут в координатах GUI (fixed scale 2), рисуются поверх панелей,
 * но под карточкой профиля. Стек растёт вверх от нижнего края окна.
 */
public class GuiToastManager {

    public enum Kind {
        ENABLED(62, 200, 140),
        DISABLED(150, 158, 178),
        BIND(106, 156, 255),
        FAVORITE(235, 190, 80),
        INFO(82, 128, 184);

        final int r, g, b;

        Kind(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final float HEIGHT = 16f;
    private static final float GAP = 4f;
    private static final float MAX_WIDTH = 230f;
    private static final float TEXT_SIZE = 5.5f;
    private static final long DEFAULT_DURATION = 1800;
    private static final long SLIDE_IN_MS = 180;
    private static final long FADE_OUT_MS = 260;
    private static final int MAX_TOASTS = 4;

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static long lastRenderTime = System.currentTimeMillis();

    private static final class Toast {
        final String text;
        final Kind kind;
        final long createdAt;
        final long duration;
        float animY = -1f;

        Toast(String text, Kind kind, long createdAt, long duration) {
            this.text = text;
            this.kind = kind;
            this.createdAt = createdAt;
            this.duration = duration;
        }
    }

    private GuiToastManager() {
    }

    public static void show(String text, Kind kind) {
        show(text, kind, DEFAULT_DURATION);
    }

    public static void show(String text, Kind kind, long duration) {
        if (text == null || text.isEmpty()) return;

        // повтор того же текста не плодит дубликаты — перезапускаем таймер
        for (Toast toast : TOASTS) {
            if (toast.text.equals(text) && toast.kind == kind) {
                TOASTS.remove(toast);
                TOASTS.add(new Toast(text, kind, System.currentTimeMillis(), duration));
                return;
            }
        }

        TOASTS.add(new Toast(text, kind, System.currentTimeMillis(), duration));
        while (TOASTS.size() > MAX_TOASTS) {
            TOASTS.removeFirst();
        }
    }

    public static void clear() {
        TOASTS.clear();
        lastRenderTime = System.currentTimeMillis();
    }

    public static void render(DrawContext context, float centerX, float bottomY, float alphaMultiplier) {
        if (alphaMultiplier <= 0.01f) return;

        long now = System.currentTimeMillis();
        float deltaTime = Math.min((now - lastRenderTime) / 1000f, 0.1f);
        lastRenderTime = now;

        TOASTS.removeIf(t -> now - t.createdAt > t.duration + FADE_OUT_MS);
        if (TOASTS.isEmpty()) return;

        float stackBottom = bottomY;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            Toast toast = TOASTS.get(i);

            long age = now - toast.createdAt;
            float inProgress = easeOutCubic(Math.min(1f, age / (float) SLIDE_IN_MS));
            float outProgress = age <= toast.duration ? 0f
                    : Math.min(1f, (age - toast.duration) / (float) FADE_OUT_MS);
            float alpha = inProgress * (1f - outProgress) * alphaMultiplier;

            // схлопывающийся тост освобождает место в стеке
            float slotHeight = (HEIGHT + GAP) * (1f - outProgress);
            float targetY = stackBottom - HEIGHT;
            if (toast.animY < 0) {
                toast.animY = targetY + 8f;
            }
            toast.animY += (targetY - toast.animY) * Math.min(1f, deltaTime * 14f);

            float slideX = (1f - inProgress) * 14f + outProgress * 10f;

            if (alpha > 0.01f) {
                renderToast(toast, centerX + slideX, toast.animY, alpha);
            }

            stackBottom -= slotHeight;
        }
    }

    private static void renderToast(Toast toast, float centerX, float y, float alpha) {
        String text = toast.text;
        float textWidth = Fonts.BOLD.getWidth(text, TEXT_SIZE);
        boolean trimmed = false;
        while (textWidth > MAX_WIDTH - 24 && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
            textWidth = Fonts.BOLD.getWidth(text + "…", TEXT_SIZE);
            trimmed = true;
        }
        if (trimmed) text += "…";

        float width = textWidth + 24;
        float x = centerX - width / 2f;

        Kind kind = toast.kind;

        Render2D.rect(x, y, width, HEIGHT, new Color(10, 12, 20, clamp((int) (235 * alpha))).getRGB(), 5);
        Render2D.outline(x, y, width, HEIGHT, 0.5f,
                new Color(kind.r, kind.g, kind.b, clamp((int) (110 * alpha))).getRGB(), 5);

        // акцентная полоска слева
        Render2D.rect(x + 3, y + 4, 1.5f, HEIGHT - 8,
                new Color(kind.r, kind.g, kind.b, clamp((int) (220 * alpha))).getRGB(), 1);

        // кружок-статус
        Render2D.rect(x + 8, y + HEIGHT / 2f - 2, 4, 4,
                new Color(kind.r, kind.g, kind.b, clamp((int) (200 * alpha))).getRGB(), 2);

        Fonts.BOLD.draw(text, x + 16, y + HEIGHT / 2f - Fonts.BOLD.getHeight(TEXT_SIZE) / 2f, TEXT_SIZE,
                new Color(226, 230, 244, clamp((int) (235 * alpha))).getRGB());
    }

    private static float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}