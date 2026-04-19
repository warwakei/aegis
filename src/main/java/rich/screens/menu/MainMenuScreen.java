package rich.screens.menu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import rich.Initialization;
import rich.screens.account.AccountEntry;
import rich.screens.account.AccountRenderer;
import rich.util.Version;
import rich.util.config.impl.account.AccountConfig;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.sounds.SoundManager;
import rich.util.animations.Easings;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainMenuScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("rich", "textures/menu/backmenu.png");
    private static final float FIXED_GUI_SCALE = 2.0f;
    private static final int BUTTON_SIZE = 42;
    private static final int BUTTON_SPACING = 16;
    private static final String[] BUTTON_ICONS = {"a", "b", "x", "s", "i"};

    private enum View { MAIN_MENU, ALT_SCREEN }
    private enum TransitionPhase { NONE, FADE_OUT, FADE_IN }

    private View currentView = View.MAIN_MENU;
    private View targetView = View.MAIN_MENU;
    private TransitionPhase transitionPhase = TransitionPhase.NONE;
    private long transitionStart = 0L;
    private long screenStartTime = 0L;
    private boolean initialized = false;
    private long lastRenderTime = 0L;

    private float[] buttonScales = new float[5];
    private float[] buttonHoverProgress = new float[5];
    private float[] buttonGlowProgress = new float[5];
    private int hoveredButton = -1;
    private float exitButtonRedProgress = 0f;

    private boolean welcomeSoundPlayed = false;
    private boolean isUnlocked = false;
    private long unlockTime = 0L;
    private float unlockTextPulse = 0f;
    private float currentZoom = 1.08f;
    private float targetZoom = 1.08f;

    private final AccountRenderer accountRenderer;
    private final AccountConfig accountConfig;
    private String nicknameText = "";
    private boolean nicknameFieldFocused = false;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    private final List<MenuParticle> menuParticles = new ArrayList<>();
    private final Random particleRandom = new Random();

    private static final long UNLOCK_FADE_DURATION = 300L;
    private static final long MENU_APPEAR_DURATION = 800L;
    private static final long MENU_APPEAR_DELAY = 200L;
    private static final long VIEW_FADE_OUT_DURATION = 200L;
    private static final long VIEW_FADE_IN_DURATION = 250L;
    private static final float LEFT_PANEL_WIDTH = 100;
    private static final float LEFT_PANEL_TOP_HEIGHT = 100;
    private static final float LEFT_PANEL_BOTTOM_HEIGHT = 58;
    private static final float RIGHT_PANEL_WIDTH = 300;
    private static final float RIGHT_PANEL_HEIGHT = 165;
    private static final float SLIDE_DISTANCE = 40f;
    private static final float ZOOM_SPEED = 3f;
    /** Совпадает с AccountRenderer.renderLeftPanelTop (поле ника + кнопка +) */
    private static final float NICK_FIELD_Y_OFF = 38f;
    private static final float NICK_FIELD_HEIGHT = 14f;
    private static final float NICK_ADD_BTN_SIZE = 14f;
    private static final float NICK_ADD_GAP = 3f;

    public MainMenuScreen() {
        super(Text.literal("Main Menu"));
        for (int i = 0; i < 5; i++) {
            buttonScales[i] = 1f;
            buttonHoverProgress[i] = 0f;
            buttonGlowProgress[i] = 0f;
        }
        this.accountRenderer = new AccountRenderer();
        this.accountConfig = AccountConfig.getInstance();
        this.accountConfig.load();
        initParticles();
    }

    @Override
    protected void init() {
        initialized = false;
        initParticles();
    }

    private void initParticles() {
        menuParticles.clear();
        for (int i = 0; i < 50; i++) {
            menuParticles.add(new MenuParticle(
                    particleRandom.nextFloat() * 1000, particleRandom.nextFloat() * 600,
                    particleRandom.nextFloat() * 0.5f + 0.2f, particleRandom.nextFloat() * 30f + 20f,
                    particleRandom.nextFloat() * 0.3f + 0.1f, particleRandom.nextFloat() * 360f));
        }
    }

    private int getFixedScaledWidth() {
        return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    private float toFixedCoord(double coord) {
        float currentScale = (float) client.getWindow().getScaleFactor();
        return (float) (coord * currentScale / FIXED_GUI_SCALE);
    }

    private void unlock() {
        if (!isUnlocked) {
            isUnlocked = true;
            unlockTime = Util.getMeasuringTimeMs();
            targetZoom = 1.0f;
        }
    }

    private float getUnlockTextAlpha(long currentTime) {
        if (!isUnlocked) return 1f;
        long elapsed = currentTime - unlockTime;
        return 1f - MathHelper.clamp((float) elapsed / UNLOCK_FADE_DURATION, 0f, 1f);
    }

    private float getMenuProgress(long currentTime) {
        if (!isUnlocked) return 0f;
        long elapsed = currentTime - unlockTime - MENU_APPEAR_DELAY;
        if (elapsed < 0) return 0f;
        return MathHelper.clamp((float) elapsed / MENU_APPEAR_DURATION, 0f, 1f);
    }

    private float easeOutCubic(float x) { return 1f - (float) Math.pow(1f - x, 3); }
    private float easeInCubic(float x) { return x * x * x; }
    private float easeOutQuart(float x) { return 1f - (float) Math.pow(1f - x, 4); }

    private void switchToView(View view) {
        if (currentView != view && transitionPhase == TransitionPhase.NONE) {
            if (view == View.MAIN_MENU) {
                nicknameFieldFocused = false;
            }
            targetView = view;
            transitionPhase = TransitionPhase.FADE_OUT;
            transitionStart = Util.getMeasuringTimeMs();
        }
    }

    private void updateTransition(long currentTime) {
        if (transitionPhase == TransitionPhase.NONE) return;
        long elapsed = currentTime - transitionStart;
        if (transitionPhase == TransitionPhase.FADE_OUT) {
            if (elapsed >= VIEW_FADE_OUT_DURATION) {
                currentView = targetView;
                transitionPhase = TransitionPhase.FADE_IN;
                transitionStart = currentTime;
            }
        } else if (elapsed >= VIEW_FADE_IN_DURATION) {
            transitionPhase = TransitionPhase.NONE;
        }
    }

    private float getMainMenuAlpha(long currentTime) {
        if (currentView == View.ALT_SCREEN && transitionPhase == TransitionPhase.NONE) return 0f;
        if (currentView == View.MAIN_MENU && transitionPhase == TransitionPhase.NONE) return 1f;
        long elapsed = currentTime - transitionStart;
        if (transitionPhase == TransitionPhase.FADE_OUT) {
            return currentView == View.MAIN_MENU ? 1f - easeInCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_OUT_DURATION, 0f, 1f)) : 0f;
        } else if (transitionPhase == TransitionPhase.FADE_IN) {
            return currentView == View.MAIN_MENU ? easeOutCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_IN_DURATION, 0f, 1f)) : 0f;
        }
        return currentView == View.MAIN_MENU ? 1f : 0f;
    }

    private float getAltScreenAlpha(long currentTime) {
        if (currentView == View.MAIN_MENU && transitionPhase == TransitionPhase.NONE) return 0f;
        if (currentView == View.ALT_SCREEN && transitionPhase == TransitionPhase.NONE) return 1f;
        long elapsed = currentTime - transitionStart;
        if (transitionPhase == TransitionPhase.FADE_OUT) {
            return currentView == View.ALT_SCREEN ? 1f - easeInCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_OUT_DURATION, 0f, 1f)) : 0f;
        } else if (transitionPhase == TransitionPhase.FADE_IN) {
            return currentView == View.ALT_SCREEN ? easeOutCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_IN_DURATION, 0f, 1f)) : 0f;
        }
        return currentView == View.ALT_SCREEN ? 1f : 0f;
    }

    /**
     * Фон меню: {@link Initialization} → {@link rich.util.render.shader.RenderCore#getTexturePipeline()}
     * (текстура на GL-quad), дальше тон/виньетка/частицы через {@link Render2D} → RectPipeline (кастомный пайплайн, не чистый Blaze3D UI).
     */
    private void drawBackground(float zoom) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();
        float zoomedWidth = screenWidth * zoom;
        float zoomedHeight = screenHeight * zoom;
        float offsetX = (screenWidth - zoomedWidth) / 2f;
        float offsetY = (screenHeight - zoomedHeight) / 2f;
        int[] colors = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
        float[] radii = {0, 0, 0, 0};

        Initialization init = Initialization.getInstance();
        if (init != null && init.getManager() != null && init.getManager().getRenderCore() != null) {
            init.getManager().getRenderCore().getTexturePipeline()
                    .drawTexture(BACKGROUND_TEXTURE, offsetX, offsetY, zoomedWidth, zoomedHeight, 0, 0, 1, 1, colors, radii, 1f);
        }

        long currentTime = Util.getMeasuringTimeMs();
        renderMenuColorGrade(screenWidth, screenHeight, currentTime);
        renderMenuVignette(screenWidth, screenHeight);
        renderBackgroundParticles(screenWidth, screenHeight, zoom);
        renderMenuTopBar(screenWidth, currentTime);
    }

    private void renderMenuColorGrade(int screenWidth, int screenHeight, long currentTime) {
        float pulse = (float) Math.sin(currentTime * 0.0008) * 0.5f + 0.5f;
        int baseTint = (int) (12 + pulse * 7);
        Render2D.rect(0, 0, screenWidth, screenHeight, withAlpha(0x080c14, baseTint), 0);
        int g = (int) (22 + pulse * 10);
        Render2D.gradientRect(0, 0, screenWidth, screenHeight, new int[]{
                withAlpha(0x152238, g),
                withAlpha(0x0e1420, (int) (g * 0.65f)),
                withAlpha(0x060810, (int) (g * 0.4f)),
                withAlpha(0x101828, (int) (g * 0.55f))
        }, 0);
    }

    private void renderMenuVignette(int screenWidth, int screenHeight) {
        float ew = Math.min(168f, screenWidth * 0.24f);
        float eh = Math.min(128f, screenHeight * 0.2f);
        int edge = 118;
        Render2D.gradientRect(0, 0, ew, screenHeight, new int[]{
                withAlpha(0x000000, edge), withAlpha(0x000000, 0), withAlpha(0x000000, 0), withAlpha(0x000000, edge)}, 0);
        Render2D.gradientRect(screenWidth - ew, 0, ew, screenHeight, new int[]{
                withAlpha(0x000000, 0), withAlpha(0x000000, edge), withAlpha(0x000000, edge), withAlpha(0x000000, 0)}, 0);
        Render2D.gradientRect(0, 0, screenWidth, eh, new int[]{
                withAlpha(0x000000, 88), withAlpha(0x000000, 88), withAlpha(0x000000, 0), withAlpha(0x000000, 0)}, 0);
        Render2D.gradientRect(0, screenHeight - eh, screenWidth, eh, new int[]{
                withAlpha(0x000000, 0), withAlpha(0x000000, 0), withAlpha(0x000000, 82), withAlpha(0x000000, 82)}, 0);
    }

    private void renderMenuTopBar(int screenWidth, long currentTime) {
        // Slightly tighter header: closer to the top and with less vertical padding.
        float h = 18f;
        float pulse = (float) Math.sin(currentTime * 0.0012) * 0.06f + 0.94f;
        int barA = (int) (42 * pulse);
        Render2D.gradientRect(0, 0, screenWidth, h, new int[]{
                withAlpha(0x0e121a, barA),
                withAlpha(0x0e121a, barA),
                withAlpha(0x080a10, (int) (barA * 0.88f)),
                withAlpha(0x080a10, (int) (barA * 0.88f))
        }, 0);
        Render2D.rect(0, h - 1f, screenWidth, 1f, withAlpha(0x4A6FA5, (int) (105 * pulse)), 0);
        String rightTag = "Main Menu";
        float rw = Fonts.REGULARNEW.getWidth(rightTag, 5f);
        Fonts.REGULARNEW.draw(rightTag, screenWidth - rw - 14f, 5.8f, 5f, withAlpha(0x8898a8, (int) (195 * pulse)));
    }

    private void renderBackgroundParticles(int screenWidth, int screenHeight, float zoom) {
        long currentTime = Util.getMeasuringTimeMs();
        float deltaTime = 0.016f;
        for (MenuParticle p : menuParticles) {
            p.x += Math.cos(Math.toRadians(p.angle)) * p.speed * deltaTime * 20f;
            p.y += Math.sin(Math.toRadians(p.angle)) * p.speed * deltaTime * 20f;
            float scaledWidth = screenWidth / zoom;
            float scaledHeight = screenHeight / zoom;
            if (p.x < 0) p.x = scaledWidth; if (p.x > scaledWidth) p.x = 0;
            if (p.y < 0) p.y = scaledHeight; if (p.y > scaledHeight) p.y = 0;
            float alphaPulse = (float) Math.sin(currentTime * 0.002 + p.x * 0.01f) * 0.3f + 0.7f;
            float alpha = p.baseAlpha * alphaPulse;
            int particleAlpha = (int) (alpha * 100);
            float size = p.size * (0.8f + alphaPulse * 0.4f);
            Render2D.rect(p.x, p.y, size, size, withAlpha(0x5078c0, particleAlpha), size / 2f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = Util.getMeasuringTimeMs();
        if (!initialized) { screenStartTime = currentTime; lastRenderTime = currentTime; initialized = true; }
        float deltaTime = (currentTime - lastRenderTime) / 1000f;
        lastRenderTime = currentTime;
        deltaTime = MathHelper.clamp(deltaTime, 0f, 0.1f);
        updateTransition(currentTime);
        unlockTextPulse += deltaTime * 3f;
        currentZoom = MathHelper.lerp(deltaTime * ZOOM_SPEED, currentZoom, targetZoom);
        float scrollSpeed = 12f;
        scrollOffset += (targetScrollOffset - scrollOffset) * Math.min(1f, deltaTime * scrollSpeed);
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.1f) scrollOffset = targetScrollOffset;
        float unlockTextAlpha = getUnlockTextAlpha(currentTime);
        float menuProgress = easeOutQuart(getMenuProgress(currentTime));
        float mainAlpha = getMainMenuAlpha(currentTime);
        float altAlpha = getAltScreenAlpha(currentTime);
        if (!welcomeSoundPlayed && menuProgress > 0.1f) { SoundManager.playSoundDirect(SoundManager.WELCOME, 1.0f, 1.0f); welcomeSoundPlayed = true; }
        float scaledMouseX = toFixedCoord(mouseX);
        float scaledMouseY = toFixedCoord(mouseY);
        int fixedWidth = getFixedScaledWidth();
        int fixedHeight = getFixedScaledHeight();
        boolean canInteractMain = currentView == View.MAIN_MENU && transitionPhase == TransitionPhase.NONE && menuProgress > 0.8f;
        hoveredButton = canInteractMain ? getHoveredButton(scaledMouseX, scaledMouseY, fixedWidth, fixedHeight, menuProgress) : -1;
        updateButtonAnimations(deltaTime);
        Render2D.beginOverlay();
        drawBackground(currentZoom);
        if (mainAlpha > 0.01f) renderMainMenuContent(fixedWidth, fixedHeight, scaledMouseX, scaledMouseY, menuProgress, mainAlpha, unlockTextAlpha, currentTime);
        if (altAlpha > 0.01f) renderAltScreenContent(fixedWidth, fixedHeight, scaledMouseX, scaledMouseY, altAlpha, currentTime);
        int footLine1 = withAlpha(0x8a95a8, 150);
        int footLine2 = withAlpha(0x5c6578, 118);
        Fonts.REGULARNEW.drawCentered(Version.NAME, fixedWidth / 2f, fixedHeight - 15, 5f, footLine1);
        Fonts.REGULARNEW.drawCentered("© All Rights Reserved", fixedWidth / 2f, fixedHeight - 6, 4f, footLine2);
        Render2D.endOverlay();
    }

    private void renderMainMenuContent(int screenWidth, int screenHeight, float mouseX, float mouseY, float menuProgress, float alpha, float unlockTextAlpha, long currentTime) {
        float slideOffset = (1f - alpha) * 20f;
        if (unlockTextAlpha > 0.01f && alpha > 0.5f) renderUnlockText(unlockTextAlpha * alpha, screenWidth, screenHeight);
        if (menuProgress > 0.01f) {
            renderTime(menuProgress * alpha, screenWidth, screenHeight, menuProgress, slideOffset);
            renderButtons(mouseX, mouseY, menuProgress * alpha, screenWidth, screenHeight, menuProgress, slideOffset, currentTime);
        }
    }

    private void renderAltScreenContent(int screenWidth, int screenHeight, float mouseX, float mouseY, float alpha, long currentTime) {
        float totalWidth = LEFT_PANEL_WIDTH + 5 + RIGHT_PANEL_WIDTH;
        float totalHeight = LEFT_PANEL_TOP_HEIGHT + 5 + LEFT_PANEL_BOTTOM_HEIGHT;
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - totalHeight / 2f;
        float accountPanelOffsetX = (1f - alpha) * -SLIDE_DISTANCE;
        if (alpha > 0.01f) accountRenderer.renderLeftPanelTop(startX + accountPanelOffsetX, startY, LEFT_PANEL_WIDTH, LEFT_PANEL_TOP_HEIGHT, alpha, nicknameText, nicknameFieldFocused, mouseX - accountPanelOffsetX, mouseY, currentTime);
        float leftPanelBottomY = startY + LEFT_PANEL_TOP_HEIGHT + 5;
        float activeSessionOffsetY = (1f - alpha) * SLIDE_DISTANCE;
        if (alpha > 0.01f) accountRenderer.renderLeftPanelBottom(startX, leftPanelBottomY + activeSessionOffsetY, LEFT_PANEL_WIDTH, LEFT_PANEL_BOTTOM_HEIGHT, alpha, accountConfig.getActiveAccountName(), accountConfig.getActiveAccountDate(), accountConfig.getActiveAccountSkin());
        float rightPanelX = startX + LEFT_PANEL_WIDTH + 5;
        List<AccountEntry> sortedAccounts = accountConfig.getSortedAccounts();
        float accountsListOffsetX = (1f - alpha) * SLIDE_DISTANCE;
        if (alpha > 0.01f) accountRenderer.renderRightPanel(rightPanelX + accountsListOffsetX, startY, RIGHT_PANEL_WIDTH, RIGHT_PANEL_HEIGHT, alpha, sortedAccounts, scrollOffset, mouseX - accountsListOffsetX, mouseY, 1f, 2);
    }

    private void updateButtonAnimations(float deltaTime) {
        for (int i = 0; i < 5; i++) {
            float targetHover = (hoveredButton == i) ? 1f : 0f;
            buttonHoverProgress[i] = (float) Easings.SPRING.ease(Math.min(1f, (buttonHoverProgress[i] + (targetHover - buttonHoverProgress[i]) * deltaTime * 12f)));
            float targetScale = (hoveredButton == i) ? 1.08f : 1f;
            buttonScales[i] = MathHelper.lerp(deltaTime * 12f, buttonScales[i], targetScale);
            float targetGlow = (hoveredButton == i) ? 1f : 0f;
            buttonGlowProgress[i] = MathHelper.lerp(deltaTime * 8f, buttonGlowProgress[i], targetGlow);
        }
        float targetRed = (hoveredButton == 4) ? 1f : 0f;
        exitButtonRedProgress = MathHelper.lerp(deltaTime * 8f, exitButtonRedProgress, targetRed);
    }

    private void renderUnlockText(float opacity, int screenWidth, int screenHeight) {
        if (opacity < 0.01f) return;
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        String text = "Press any key to continue";
        float fontSize = 14f;
        float pulse = (float) Math.sin(unlockTextPulse) * 0.15f + 0.85f;
        int textAlpha = (int) (opacity * 255 * pulse);
        Fonts.REGULARNEW.drawCentered(text, centerX, centerY - 5, fontSize, withAlpha(0xFFFFFF, textAlpha));
        float arrowY = centerY + 25;
        float arrowBounce = (float) Math.sin(unlockTextPulse * 1.5f) * 3f;
        int arrowAlpha = (int) (opacity * 200 * pulse);
        Fonts.REGULARNEW.drawCentered("▼", centerX, arrowY + arrowBounce, fontSize, withAlpha(0xFFFFFF, arrowAlpha));
    }

    private void renderTime(float opacity, int screenWidth, int screenHeight, float menuProgress, float extraSlideOffset) {
        float centerX = screenWidth / 2f;
        float slideOffset = (1f - menuProgress) * 40f + extraSlideOffset;
        float centerY = screenHeight / 2f - 55 + slideOffset;
        LocalTime now = LocalTime.now();
        String timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        int textAlpha = (int) (opacity * 255);
        float fontSize = 48f;
        float textHeight = Fonts.BOLD.getHeight(fontSize);
        long currentTime = Util.getMeasuringTimeMs();
        float timePulse = (float) Math.sin(currentTime * 0.003) * 0.1f + 0.9f;
        int timeColor = withAlpha(0xFFFFFF, (int)(textAlpha * timePulse));
        Fonts.BOLD.drawCentered(timeText, centerX, centerY - textHeight / 2f, fontSize, timeColor);
        String dateText = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.ENGLISH));
        int dateAlpha = (int) (opacity * 220);
        float dateY = centerY + textHeight / 2f + 4;
        float dateWidth = Fonts.BOLD.getWidth(dateText, 12f);
        float dateX = centerX - dateWidth / 2f;
        float lineWidth = dateWidth + 20;
        float lineY = dateY - 3f;
        int lineAlpha = (int)(dateAlpha * 0.4f);
        int lineColor = withAlpha(0x80a0c0, lineAlpha);
        Render2D.gradientRect(centerX - lineWidth / 2f, lineY, lineWidth, 0.5f, new int[]{withAlpha(0x406080, 0), lineColor, lineColor, withAlpha(0x406080, 0)}, 0);
        Fonts.BOLD.draw(dateText, dateX, dateY, 12f, withAlpha(0xc0d0e0, dateAlpha));
        // Intentionally no extra centered subtitle here (keeps main menu clean).
    }

    private void renderButtons(float mouseX, float mouseY, float opacity, int screenWidth, int screenHeight, float menuProgress, float extraSlideOffset, long currentTime) {
        float totalWidth = BUTTON_SIZE * 5 + BUTTON_SPACING * 4;
        float startX = (screenWidth - totalWidth) / 2f;
        float slideOffset = (1f - menuProgress) * 60f + extraSlideOffset;
        float centerY = screenHeight / 2f + 30 + slideOffset;
        for (int i = 0; i < 5; i++) {
            float buttonDelay = i * 0.12f;
            float buttonProgress = MathHelper.clamp((menuProgress - buttonDelay) / (1f - buttonDelay * 0.5f), 0f, 1f);
            float easedProgress = easeOutCubic(buttonProgress);
            float buttonX = startX + i * (BUTTON_SIZE + BUTTON_SPACING);
            renderCircleButton(i, buttonX, centerY, opacity * easedProgress, currentTime);
        }
    }

    private void renderCircleButton(int index, float x, float y, float opacity, long currentTime) {
        if (opacity < 0.01f) return;
        float scaleVal = buttonScales[index];
        float hoverProgress = buttonHoverProgress[index];
        float glowProgress = buttonGlowProgress[index];
        float size = BUTTON_SIZE * scaleVal;
        float centerX = x + BUTTON_SIZE / 2f;
        float centerYPos = y + BUTTON_SIZE / 2f;
        float drawX = centerX - size / 2f;
        float drawY = centerYPos - size / 2f;
        float radius = size / 2f;
        int bgAlpha = (int) (opacity * 140);
        int headerAlpha = (int) (opacity * (170 + hoverProgress * 60));
        int outlineAlpha = (int) (opacity * (180 + hoverProgress * 75));
        int bgTopLeft, bgTopRight, bgBottomLeft, bgBottomRight, outlineColor, iconColor;
        if (index == 4) {
            float redLerp = exitButtonRedProgress;
            int rBg = (int) MathHelper.lerp(redLerp, 0x18, 0x30);
            int gBg = (int) MathHelper.lerp(redLerp, 0x1a, 0x18);
            int bBg = (int) MathHelper.lerp(redLerp, 0x22, 0x1a);
            bgTopLeft = withAlpha((rBg << 16) | (gBg << 8) | bBg, headerAlpha);
            bgTopRight = withAlpha(((rBg + 6) << 16) | ((gBg + 6) << 8) | bBg + 6, headerAlpha);
            bgBottomLeft = withAlpha(((rBg - 6) << 16) | ((gBg - 6) << 8) | bBg - 6, headerAlpha);
            bgBottomRight = withAlpha((rBg << 16) | (gBg << 8) | bBg, headerAlpha);
            int outR = (int) MathHelper.lerp(redLerp, 0x2a, 0x6a);
            int outG = (int) MathHelper.lerp(redLerp, 0x2e, 0x40);
            int outB = (int) MathHelper.lerp(redLerp, 0x3a, 0x40);
            outlineColor = withAlpha((outR << 16) | (outG << 8) | outB, outlineAlpha);
            int iconR = 255;
            int iconG = (int) MathHelper.lerp(redLerp, 255, 150);
            int iconB = (int) MathHelper.lerp(redLerp, 255, 150);
            iconColor = withAlpha((iconR << 16) | (iconG << 8) | iconB, (int) (opacity * 255));
        } else {
            float pulse = (float) Math.sin(currentTime * 0.002 + index * 0.5) * 0.15f + 0.85f;
            int baseR = 0x16 + (int)(hoverProgress * 8);
            int baseG = 0x19 + (int)(hoverProgress * 8);
            int baseB = 0x24 + (int)(hoverProgress * 12);
            bgTopLeft = withAlpha(((baseR + 4) << 16) | ((baseG + 6) << 8) | (baseB + 8), headerAlpha);
            bgTopRight = withAlpha(((baseR + 8) << 16) | ((baseG + 10) << 8) | (baseB + 12), headerAlpha);
            bgBottomLeft = withAlpha((baseR << 16) | (baseG << 8) | baseB, headerAlpha);
            bgBottomRight = withAlpha(((baseR + 4) << 16) | ((baseG + 4) << 8) | (baseB + 4), headerAlpha);
            int outR = 0x28 + (int)(hoverProgress * 20);
            int outG = 0x2d + (int)(hoverProgress * 20);
            int outB = 0x3a + (int)(hoverProgress * 25);
            outlineColor = withAlpha((outR << 16) | (outG << 8) | outB, outlineAlpha);
            int iconBright = (int)(200 + hoverProgress * 55);
            iconColor = withAlpha(0xFFFFFF, (int) (opacity * iconBright * pulse));
        }
        Render2D.gradientRect(drawX, drawY, size, size, new int[]{bgTopLeft, bgTopRight, bgBottomRight, bgBottomLeft}, radius);
        Render2D.outline(drawX, drawY, size, size, 1f + hoverProgress * 0.5f, outlineColor, radius);
        float iconSize = 17f * scaleVal;
        String icon = BUTTON_ICONS[index];
        float iconWidth = Fonts.MAINMENUSCREEN.getWidth(icon, iconSize);
        float iconHeight = Fonts.MAINMENUSCREEN.getHeight(iconSize);
        Fonts.MAINMENUSCREEN.draw(icon, centerX - iconWidth / 2f + 0.5f, centerYPos - iconHeight / 2f, iconSize, iconColor);
    }

    private int getHoveredButton(float mouseX, float mouseY, int screenWidth, int screenHeight, float menuProgress) {
        float totalWidth = BUTTON_SIZE * 5 + BUTTON_SPACING * 4;
        float startX = (screenWidth - totalWidth) / 2f;
        float slideOffset = (1f - menuProgress) * 60f;
        float centerY = screenHeight / 2f + 30 + slideOffset;
        for (int i = 0; i < 5; i++) {
            float buttonX = startX + i * (BUTTON_SIZE + BUTTON_SPACING);
            float buttonCenterX = buttonX + BUTTON_SIZE / 2f;
            float buttonCenterY = centerY + BUTTON_SIZE / 2f;
            float dx = mouseX - buttonCenterX;
            float dy = mouseY - buttonCenterY;
            if (dx * dx + dy * dy <= (BUTTON_SIZE / 2f) * (BUTTON_SIZE / 2f)) return i;
        }
        return -1;
    }

    public void handleButtonClick(int index) {
        switch (index) {
            case 0: // Singleplayer
                client.setScreen(new net.minecraft.client.gui.screen.world.SelectWorldScreen(this)); break;
            case 1: // Multiplayer
                client.setScreen(new MultiplayerScreen(this)); break;
            case 2: // AltManager (пользователь)
                switchToView(View.ALT_SCREEN); break;
            case 3: // Settings - открываем настройки майнкрафта
                client.setScreen(new net.minecraft.client.gui.screen.option.OptionsScreen(this, client.options)); break;
            case 4: // Exit
                client.scheduleStop(); break;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!isUnlocked) { unlock(); return true; }
        float scaledMouseX = toFixedCoord(click.x());
        float scaledMouseY = toFixedCoord(click.y());
        if (currentView == View.MAIN_MENU) {
            int hovered = getHoveredButton(scaledMouseX, scaledMouseY, getFixedScaledWidth(), getFixedScaledHeight(), getMenuProgress(Util.getMeasuringTimeMs()));
            if (hovered >= 0) { handleButtonClick(hovered); return true; }
        } else if (currentView == View.ALT_SCREEN && transitionPhase == TransitionPhase.NONE) {
            return handleAltManagerClick(scaledMouseX, scaledMouseY);
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean isOverNicknameField(float mouseX, float mouseY, float startX, float startY) {
        float fieldX = startX + 5;
        float fieldY = startY + NICK_FIELD_Y_OFF;
        float fieldW = LEFT_PANEL_WIDTH - 10 - NICK_ADD_BTN_SIZE - NICK_ADD_GAP;
        return mouseX >= fieldX && mouseX <= fieldX + fieldW
                && mouseY >= fieldY && mouseY <= fieldY + NICK_FIELD_HEIGHT;
    }

    private boolean handleAltManagerClick(float mouseX, float mouseY) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float totalWidth = LEFT_PANEL_WIDTH + 5 + RIGHT_PANEL_WIDTH;
        float totalHeight = LEFT_PANEL_TOP_HEIGHT + 5 + LEFT_PANEL_BOTTOM_HEIGHT;
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - totalHeight / 2f;

        if (isOverNicknameField(mouseX, mouseY, startX, startY)) {
            nicknameFieldFocused = true;
            return true;
        }
        nicknameFieldFocused = false;

        // Левая панель верх — кнопка «+» (координаты как в AccountRenderer)
        float fieldX = startX + 5;
        float fieldW = LEFT_PANEL_WIDTH - 10 - NICK_ADD_BTN_SIZE - NICK_ADD_GAP;
        float addBtnX = fieldX + fieldW + NICK_ADD_GAP;
        float addBtnY = startY + NICK_FIELD_Y_OFF;
        float addBtnSize = NICK_ADD_BTN_SIZE;
        if (mouseX >= addBtnX && mouseX <= addBtnX + addBtnSize &&
            mouseY >= addBtnY && mouseY <= addBtnY + addBtnSize) {
            if (!nicknameText.isEmpty()) {
                String date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                accountConfig.addAccount(new AccountEntry(nicknameText, date, null, false, accountConfig.getAccounts().size()));
                accountConfig.save();
                nicknameText = "";
            }
            return true;
        }

        // Кнопка Random
        float randomBtnX = startX + 5;
        float randomBtnY = addBtnY + NICK_FIELD_HEIGHT + 6;
        float randomBtnWidth = LEFT_PANEL_WIDTH - 10;
        float randomBtnHeight = 16;
        if (mouseX >= randomBtnX && mouseX <= randomBtnX + randomBtnWidth &&
            mouseY >= randomBtnY && mouseY <= randomBtnY + randomBtnHeight) {
            String[] randomNames = {"Steve", "Alex", "Herobrine", "Notch", "jeb_", "Dinnerbone"};
            nicknameText = randomNames[new java.util.Random().nextInt(randomNames.length)] + new java.util.Random().nextInt(1000);
            return true;
        }

        // Кнопка Clear All
        float clearBtnX = startX + 5;
        float clearBtnY = randomBtnY + 21;
        float clearBtnWidth = LEFT_PANEL_WIDTH - 10;
        float clearBtnHeight = 16;
        if (mouseX >= clearBtnX && mouseX <= clearBtnX + clearBtnWidth &&
            mouseY >= clearBtnY && mouseY <= clearBtnY + clearBtnHeight) {
            accountConfig.clearAllAccounts();
            return true;
        }

        // Правая панель - кнопки аккаунтов
        float rightPanelX = startX + LEFT_PANEL_WIDTH + 5;
        List<AccountEntry> sortedAccounts = accountConfig.getSortedAccounts();
        float accountListX = rightPanelX + 5;
        float accountListY = startY + 28;
        float accountListWidth = RIGHT_PANEL_WIDTH - 10;
        float accountListHeight = RIGHT_PANEL_HEIGHT - 31;

        float cardWidth = (accountListWidth - 5) / 2f;
        float cardHeight = 40;
        float cardGap = 5;

        for (int i = 0; i < sortedAccounts.size(); i++) {
            int col = i % 2;
            int row = i / 2;

            float cardX = accountListX + col * (cardWidth + cardGap);
            float cardY = accountListY + row * (cardHeight + cardGap) - scrollOffset;

            if (cardY + cardHeight < accountListY - 10 || cardY > accountListY + accountListHeight + 10) continue;

            float buttonSize = 12;
            float buttonYPos = cardY + cardHeight - buttonSize - 5;
            float pinBtnX = cardX + cardWidth - buttonSize * 2 - 8;
            float delBtnX = cardX + cardWidth - buttonSize - 5;

            AccountEntry account = sortedAccounts.get(i);

            // Pin button
            if (mouseX >= pinBtnX && mouseX <= pinBtnX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize) {
                account.setPinned(!account.isPinned());
                accountConfig.save();
                return true;
            }

            // Delete button
            if (mouseX >= delBtnX && mouseX <= delBtnX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize) {
                accountConfig.removeAccount(account);
                return true;
            }

            // Клик по карточке - выбор аккаунта
            if (mouseX >= cardX && mouseX <= cardX + cardWidth &&
                mouseY >= cardY && mouseY <= cardY + cardHeight) {
                accountConfig.setActiveAccount(account.getName(), account.getDate(), account.getSkin());
                accountConfig.save();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!isUnlocked) { unlock(); return true; }
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (currentView == View.ALT_SCREEN) {
                if (nicknameFieldFocused) {
                    nicknameFieldFocused = false;
                    return true;
                }
                switchToView(View.MAIN_MENU);
                return true;
            }
            client.setScreen(null); return true;
        }
        if (currentView == View.ALT_SCREEN && nicknameFieldFocused) {
            if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!nicknameText.isEmpty()) {
                    nicknameText = nicknameText.substring(0, nicknameText.length() - 1);
                }
                return true;
            }
            if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                if (!nicknameText.isEmpty()) {
                    String date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                    accountConfig.addAccount(new AccountEntry(nicknameText, date, null, false, accountConfig.getAccounts().size()));
                    accountConfig.save();
                    nicknameText = "";
                    nicknameFieldFocused = false;
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!isUnlocked) { unlock(); return true; }
        if (currentView == View.ALT_SCREEN && nicknameFieldFocused) {
            int cp = input.codepoint();
            if (!Character.isISOControl(cp) && cp < 0x10000 && nicknameText.length() < 32) {
                nicknameText += Character.toString(cp);
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (currentView == View.ALT_SCREEN) {
            targetScrollOffset = Math.max(-200f, Math.min(0f, targetScrollOffset - (float) (vertical * 20)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean shouldPause() { return false; }

    private int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    @Override
    public void close() {
        if (currentView == View.ALT_SCREEN) switchToView(View.MAIN_MENU);
        else client.setScreen(null);
    }

    private static class MenuParticle {
        float x, y, size, speed, baseAlpha, angle;
        MenuParticle(float x, float y, float size, float speed, float alpha, float angle) {
            this.x = x; this.y = y; this.size = size; this.speed = speed; this.baseAlpha = alpha; this.angle = angle;
        }
    }
}
