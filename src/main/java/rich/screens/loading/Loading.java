package rich.screens.loading;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class Loading {

    private static Loading instance;

    private static final int TEXT_COLOR_BRIGHT = 0xFFFFFFFF;
    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final String[] LOADING_TEXTS = {
            "Loading",
            "Preparing",
            "Initializing",
            "Almost ready"
    };

    private static final long TEXT_DISPLAY_DURATION = 2200L;
    private static final long LAST_TEXT_DISPLAY_DURATION = 2500L;
    private static final long TEXT_TRANSITION_DURATION = 400L;

    private static final float ZOOM_LEVEL = 1.08f;

    private float animatedProgress = 0f;
    private float targetProgress = 0f;
    private float pulseTime = 0f;
    private long lastRenderTime = 0L;
    private long startTime = 0L;
    private boolean initialized = false;

    private int currentTextIndex = 0;
    private float currentTextOffsetY = 0f;
    private float currentTextAlpha = 1f;
    private float newTextOffsetY = -12f;
    private float newTextAlpha = 0f;
    private long lastTextChangeTime = 0L;
    private boolean isTransitioning = false;
    private long transitionStartTime = 0L;

    private float backgroundAlpha = 0f;
    private float contentAlpha = 0f;
    private boolean isFadingOut = false;
    private boolean readyToClose = false;

    private boolean resourcesLoaded = false;
    private boolean allTextsShown = false;
    private long lastTextShownTime = 0L;

    public Loading() {
        instance = this;
        this.startTime = Util.getMeasuringTimeMs();
        this.lastTextChangeTime = this.startTime;
    }

    public static Loading getInstance() {
        if (instance == null) {
            instance = new Loading();
        }
        return instance;
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

    public void render(int width, int height, float opacity) {
        long currentTime = Util.getMeasuringTimeMs();

        if (!initialized) {
            lastRenderTime = currentTime;
            initialized = true;
        }

        float deltaTime = (currentTime - lastRenderTime) / 1000f;
        lastRenderTime = currentTime;
        deltaTime = MathHelper.clamp(deltaTime, 0.001f, 0.1f);

        updateAnimations(deltaTime, currentTime);

        int fixedWidth = getFixedScaledWidth();
        int fixedHeight = getFixedScaledHeight();

        Render2D.beginOverlay();

        Render2D.backgroundImage(backgroundAlpha * opacity, ZOOM_LEVEL);

        float finalContentAlpha = contentAlpha * opacity;

        if (finalContentAlpha > 0.001f) {
            renderLogo(fixedWidth, fixedHeight, finalContentAlpha);
            renderLoadingText(fixedWidth, fixedHeight, finalContentAlpha, currentTime);
        }

        Render2D.endOverlay();
    }

    private void updateAnimations(float deltaTime, long currentTime) {
        pulseTime += deltaTime * 2f;
        animatedProgress = MathHelper.lerp(deltaTime * 5f, animatedProgress, targetProgress);

        backgroundAlpha = MathHelper.lerp(deltaTime * 5f, backgroundAlpha, 1f);
        if (backgroundAlpha > 0.99f) {
            backgroundAlpha = 1f;
        }

        if (!isFadingOut) {
            contentAlpha = MathHelper.lerp(deltaTime * 3f, contentAlpha, 1f);
            if (contentAlpha > 0.99f) {
                contentAlpha = 1f;
            }
        } else {
            contentAlpha -= deltaTime * 2f;
            if (contentAlpha < 0f) {
                contentAlpha = 0f;
                readyToClose = true;
            }
        }

        if (!isFadingOut) {
            updateTextAnimation(currentTime, deltaTime);
        }

        if (allTextsShown && resourcesLoaded && !isFadingOut) {
            long elapsed = currentTime - lastTextShownTime;
            if (elapsed >= LAST_TEXT_DISPLAY_DURATION) {
                isFadingOut = true;
            }
        }
    }

    private void updateTextAnimation(long currentTime, float deltaTime) {
        if (allTextsShown) {
            return;
        }

        if (!isTransitioning) {
            long elapsed = currentTime - lastTextChangeTime;

            if (currentTextIndex >= LOADING_TEXTS.length - 1) {
                if (!allTextsShown) {
                    allTextsShown = true;
                    lastTextShownTime = currentTime;
                }
                return;
            }

            if (elapsed >= TEXT_DISPLAY_DURATION) {
                isTransitioning = true;
                transitionStartTime = currentTime;
            }
        }

        if (isTransitioning) {
            long elapsed = currentTime - transitionStartTime;
            float rawProgress = MathHelper.clamp((float) elapsed / TEXT_TRANSITION_DURATION, 0f, 1f);
            float easedProgress = easeOutQuad(rawProgress);

            currentTextOffsetY = 12f * easedProgress;
            currentTextAlpha = 1f - easedProgress * 1.5f;
            currentTextAlpha = MathHelper.clamp(currentTextAlpha, 0f, 1f);

            newTextOffsetY = -10f * (1f - easedProgress);
            newTextAlpha = easedProgress * 1.3f;
            newTextAlpha = MathHelper.clamp(newTextAlpha, 0f, 1f);

            if (rawProgress >= 1f) {
                isTransitioning = false;
                currentTextIndex++;
                currentTextOffsetY = 0f;
                currentTextAlpha = 1f;
                newTextOffsetY = -12f;
                newTextAlpha = 0f;
                lastTextChangeTime = currentTime;

                if (currentTextIndex >= LOADING_TEXTS.length - 1) {
                    allTextsShown = true;
                    lastTextShownTime = currentTime;
                }
            }
        }
    }

    private float easeOutQuad(float x) {
        return 1f - (1f - x) * (1f - x);
    }

    private void renderLogo(int width, int height, float opacity) {
        float centerX = width / 2f;
        float centerY = height / 2f - 20;
        renderLogoText(centerX, centerY, opacity);
    }

    private void renderLogoText(float centerX, float centerY, float opacity) {
        int textAlpha = (int) (opacity * 255);
        float fontSize = 40f;
        float breathe = (float) Math.sin(pulseTime * 1.3f) * 1.3f;

        String text = "A";
        float textWidth = Fonts.ICONS.getWidth(text, fontSize);
        float textHeight = Fonts.ICONS.getHeight(fontSize);

        int shadowColor = withAlpha(0xFF000000, textAlpha / 3);
        Fonts.ICONS.draw(text, centerX - textWidth / 2f + 2, centerY - textHeight / 2f + 2 + breathe, fontSize, shadowColor);

        int mainColor = withAlpha(new Color(255, 255, 255, 255).getRGB(), textAlpha);
        Fonts.ICONS.draw(text, centerX - textWidth / 2f, centerY - textHeight / 2f + breathe, fontSize, mainColor);
    }

    private void renderLoadingText(int width, int height, float opacity, long currentTime) {
        float fontSize = 11f;
        float baseY = height / 2f + 30;
        float centerX = width / 2f;

        if (currentTextAlpha > 0.01f && currentTextIndex < LOADING_TEXTS.length) {
            String currentText = LOADING_TEXTS[currentTextIndex];
            float currentWidth = Fonts.REGULARNEW.getWidth(currentText, fontSize);
            int alpha = (int) (opacity * currentTextAlpha * 255);

            Fonts.REGULARNEW.draw(
                    currentText,
                    centerX - currentWidth / 2f,
                    baseY + currentTextOffsetY,
                    fontSize,
                    withAlpha(TEXT_COLOR_BRIGHT, alpha)
            );
        }

        if (isTransitioning && newTextAlpha > 0.01f) {
            int nextIndex = currentTextIndex + 1;
            if (nextIndex < LOADING_TEXTS.length) {
                String nextText = LOADING_TEXTS[nextIndex];
                float nextWidth = Fonts.REGULARNEW.getWidth(nextText, fontSize);
                int alpha = (int) (opacity * newTextAlpha * 255);

                Fonts.REGULARNEW.draw(
                        nextText,
                        centerX - nextWidth / 2f,
                        baseY + newTextOffsetY,
                        fontSize,
                        withAlpha(TEXT_COLOR_BRIGHT, alpha)
                );
            }
        }
    }

    public void markComplete() {
        resourcesLoaded = true;
    }

    public boolean isContentFadedOut() {
        return isFadingOut && contentAlpha <= 0.01f;
    }

    public boolean isReadyToClose() {
        return readyToClose;
    }

    public boolean isComplete() {
        return allTextsShown && resourcesLoaded;
    }

    public boolean isFadingOut() {
        return isFadingOut;
    }

    public float getContentAlpha() {
        return contentAlpha;
    }

    public void setProgress(float progress) {
        this.targetProgress = MathHelper.clamp(progress, 0f, 1f);
    }

    public float getProgress() {
        return targetProgress;
    }

    public void reset() {
        animatedProgress = 0f;
        targetProgress = 0f;
        pulseTime = 0f;
        lastRenderTime = 0L;
        startTime = Util.getMeasuringTimeMs();
        initialized = false;
        currentTextIndex = 0;
        currentTextOffsetY = 0f;
        currentTextAlpha = 1f;
        newTextOffsetY = -12f;
        newTextAlpha = 0f;
        lastTextChangeTime = startTime;
        isTransitioning = false;
        transitionStartTime = 0L;
        backgroundAlpha = 0f;
        contentAlpha = 0f;
        isFadingOut = false;
        readyToClose = false;
        resourcesLoaded = false;
        allTextsShown = false;
        lastTextShownTime = 0L;
    }

    public long getStartTime() {
        return startTime;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }
}