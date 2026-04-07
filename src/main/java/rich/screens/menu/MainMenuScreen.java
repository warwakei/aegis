package rich.screens.menu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
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
import rich.util.session.SessionChanger;
import rich.util.sounds.SoundManager;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainMenuScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("rich", "textures/menu/backmenu.png");
    private static final Identifier STEVE_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final int BUTTON_SIZE = 42;
    private static final int BUTTON_SPACING = 16;
    private static final float BLUR_RADIUS = 15f;
    private static final float OUTLINE_THICKNESS = 1f;
    private static final String[] BUTTON_ICONS = {"a", "b", "x", "s", "i"};

    private static final float LEFT_PANEL_WIDTH = 100;
    private static final float LEFT_PANEL_TOP_HEIGHT = 100;
    private static final float LEFT_PANEL_BOTTOM_HEIGHT = 58;
    private static final float RIGHT_PANEL_WIDTH = 300;
    private static final float RIGHT_PANEL_HEIGHT = 165;
    private static final float GAP = 5;

    private static final long UNLOCK_FADE_DURATION = 300L;
    private static final long MENU_APPEAR_DURATION = 800L;
    private static final long MENU_APPEAR_DELAY = 200L;
    private static final long VIEW_FADE_OUT_DURATION = 200L;
    private static final long VIEW_FADE_IN_DURATION = 250L;
    private static final float SLIDE_DISTANCE = 40f;

    private static final float ZOOM_INITIAL = 1.08f;
    private static final float ZOOM_NORMAL = 1.0f;
    private static final float ZOOM_SPEED = 3f;

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
    private int hoveredButton = -1;
    private float exitButtonRedProgress = 0f;

    private boolean welcomeSoundPlayed = false;
    private boolean isUnlocked = false;
    private long unlockTime = 0L;
    private float unlockTextPulse = 0f;

    private float currentZoom = ZOOM_INITIAL;
    private float targetZoom = ZOOM_INITIAL;

    private final AccountRenderer accountRenderer;
    private final AccountConfig accountConfig;
    private String nicknameText = "";
    private boolean nicknameFieldFocused = false;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    public MainMenuScreen() {
        super(Text.literal("Main Menu"));
        for (int i = 0; i < 5; i++) {
            buttonScales[i] = 1f;
            buttonHoverProgress[i] = 0f;
        }
        this.accountRenderer = new AccountRenderer();
        this.accountConfig = AccountConfig.getInstance();
        this.accountConfig.load();
    }

    @Override
    protected void init() {
        initialized = false;
    }

    private int getFixedScaledWidth() {
        return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    private float getScaleMultiplier() {
        float currentScale = (float) client.getWindow().getScaleFactor();
        return currentScale / FIXED_GUI_SCALE;
    }

    private float toFixedCoord(double coord) {
        float currentScale = (float) client.getWindow().getScaleFactor();
        return (float) (coord * currentScale / FIXED_GUI_SCALE);
    }

    private void unlock() {
        if (!isUnlocked) {
            isUnlocked = true;
            unlockTime = Util.getMeasuringTimeMs();
            targetZoom = ZOOM_NORMAL;
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

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    private float easeInCubic(float x) {
        return x * x * x;
    }

    private float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1f - x, 4);
    }

    private void switchToView(View view) {
        if (currentView != view && transitionPhase == TransitionPhase.NONE) {
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
        } else if (transitionPhase == TransitionPhase.FADE_IN) {
            if (elapsed >= VIEW_FADE_IN_DURATION) {
                transitionPhase = TransitionPhase.NONE;
            }
        }
    }

    private float getMainMenuAlpha(long currentTime) {
        if (currentView == View.ALT_SCREEN && transitionPhase == TransitionPhase.NONE) {
            return 0f;
        }
        if (currentView == View.MAIN_MENU && transitionPhase == TransitionPhase.NONE) {
            return 1f;
        }

        long elapsed = currentTime - transitionStart;

        if (transitionPhase == TransitionPhase.FADE_OUT) {
            if (currentView == View.MAIN_MENU) {
                return 1f - easeInCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_OUT_DURATION, 0f, 1f));
            } else {
                return 0f;
            }
        } else if (transitionPhase == TransitionPhase.FADE_IN) {
            if (currentView == View.MAIN_MENU) {
                return easeOutCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_IN_DURATION, 0f, 1f));
            } else {
                return 0f;
            }
        }

        return currentView == View.MAIN_MENU ? 1f : 0f;
    }

    private float getAltScreenAlpha(long currentTime) {
        if (currentView == View.MAIN_MENU && transitionPhase == TransitionPhase.NONE) {
            return 0f;
        }
        if (currentView == View.ALT_SCREEN && transitionPhase == TransitionPhase.NONE) {
            return 1f;
        }

        long elapsed = currentTime - transitionStart;

        if (transitionPhase == TransitionPhase.FADE_OUT) {
            if (currentView == View.ALT_SCREEN) {
                return 1f - easeInCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_OUT_DURATION, 0f, 1f));
            } else {
                return 0f;
            }
        } else if (transitionPhase == TransitionPhase.FADE_IN) {
            if (currentView == View.ALT_SCREEN) {
                return easeOutCubic(MathHelper.clamp((float) elapsed / VIEW_FADE_IN_DURATION, 0f, 1f));
            } else {
                return 0f;
            }
        }

        return currentView == View.ALT_SCREEN ? 1f : 0f;
    }

    private void drawBackground(float zoom) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float zoomedWidth = screenWidth * zoom;
        float zoomedHeight = screenHeight * zoom;
        float offsetX = (screenWidth - zoomedWidth) / 2f;
        float offsetY = (screenHeight - zoomedHeight) / 2f;

        int[] colors = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
        float[] radii = {0, 0, 0, 0};

        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(BACKGROUND_TEXTURE, offsetX, offsetY, zoomedWidth, zoomedHeight,
                        0, 0, 1, 1, colors, radii, 1f);

        long currentTime = Util.getMeasuringTimeMs();
        float pulse = (float) Math.sin(currentTime * 0.001) * 0.5f + 0.5f;
        int overlayAlpha = (int)(8 + pulse * 5);
        int overlayColor = withAlpha(0x1a2040, overlayAlpha);
        Render2D.rect(0, 0, screenWidth, screenHeight, overlayColor, 0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = Util.getMeasuringTimeMs();

        if (!initialized) {
            screenStartTime = currentTime;
            lastRenderTime = currentTime;
            initialized = true;
        }

        float deltaTime = (currentTime - lastRenderTime) / 1000f;
        lastRenderTime = currentTime;
        deltaTime = MathHelper.clamp(deltaTime, 0f, 0.1f);

        updateTransition(currentTime);

        unlockTextPulse += deltaTime * 3f;
        currentZoom = MathHelper.lerp(deltaTime * ZOOM_SPEED, currentZoom, targetZoom);

        float scrollSpeed = 12f;
        float scrollDiff = targetScrollOffset - scrollOffset;
        scrollOffset += scrollDiff * Math.min(1f, deltaTime * scrollSpeed);
        if (Math.abs(scrollDiff) < 0.1f) {
            scrollOffset = targetScrollOffset;
        }

        float unlockTextAlpha = getUnlockTextAlpha(currentTime);
        float menuProgress = easeOutQuart(getMenuProgress(currentTime));

        float mainAlpha = getMainMenuAlpha(currentTime);
        float altAlpha = getAltScreenAlpha(currentTime);

        if (!welcomeSoundPlayed && menuProgress > 0.1f) {
            SoundManager.playSoundDirect(SoundManager.WELCOME, 1.0f, 1.0f);
            welcomeSoundPlayed = true;
        }

        float scaledMouseX = toFixedCoord(mouseX);
        float scaledMouseY = toFixedCoord(mouseY);

        int fixedWidth = getFixedScaledWidth();
        int fixedHeight = getFixedScaledHeight();

        boolean canInteractMain = currentView == View.MAIN_MENU && transitionPhase == TransitionPhase.NONE && menuProgress > 0.8f;
        boolean canInteractAlt = currentView == View.ALT_SCREEN && transitionPhase == TransitionPhase.NONE;

        hoveredButton = canInteractMain ? getHoveredButton(scaledMouseX, scaledMouseY, fixedWidth, fixedHeight, menuProgress) : -1;
        updateButtonAnimations(deltaTime);

        Render2D.beginOverlay();

        drawBackground(currentZoom);

        if (mainAlpha > 0.01f) {
            renderMainMenuContent(fixedWidth, fixedHeight, scaledMouseX, scaledMouseY, menuProgress, mainAlpha, unlockTextAlpha, currentTime);
        }

        if (altAlpha > 0.01f) {
            renderAltScreenContent(fixedWidth, fixedHeight, scaledMouseX, scaledMouseY, altAlpha, currentTime);
        }

        Render2D.blur(scaledMouseX, scaledMouseY, 1, 1, BLUR_RADIUS, 1, new Color(128, 128, 128, 0).getRGB());

        Fonts.TEST.drawCentered(Version.NAME + " © All Rights Reserved", fixedWidth / 2f, fixedHeight - 6, 5f, new Color(128, 128, 128, 128).getRGB());

        Render2D.blur(scaledMouseX, scaledMouseY, 1, 1, BLUR_RADIUS, 1, new Color(128, 128, 128, 0).getRGB());

        Render2D.endOverlay();
    }

    private void renderMainMenuContent(int screenWidth, int screenHeight, float mouseX, float mouseY, float menuProgress, float alpha, float unlockTextAlpha, long currentTime) {
        float slideOffset = (1f - alpha) * 20f;

        if (unlockTextAlpha > 0.01f && alpha > 0.5f) {
            renderUnlockText(unlockTextAlpha * alpha, screenWidth, screenHeight);
        }

        if (menuProgress > 0.01f) {
            renderTime(menuProgress * alpha, screenWidth, screenHeight, menuProgress, slideOffset);
            renderButtons(mouseX, mouseY, menuProgress * alpha, screenWidth, screenHeight, menuProgress, slideOffset);
        }
    }

    private void renderAltScreenContent(int screenWidth, int screenHeight, float mouseX, float mouseY, float alpha, long currentTime) {
        float totalWidth = LEFT_PANEL_WIDTH + GAP + RIGHT_PANEL_WIDTH;
        float totalHeight = LEFT_PANEL_TOP_HEIGHT + GAP + LEFT_PANEL_BOTTOM_HEIGHT;

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        float startX = centerX - totalWidth / 2f;
        float startY = centerY - totalHeight / 2f;

        float leftPanelX = startX;
        float leftPanelTopY = startY;

        float accountPanelOffsetX = (1f - alpha) * -SLIDE_DISTANCE;

        if (alpha > 0.01f) {
            accountRenderer.renderLeftPanelTop(leftPanelX + accountPanelOffsetX, leftPanelTopY, LEFT_PANEL_WIDTH, LEFT_PANEL_TOP_HEIGHT,
                    alpha, nicknameText, nicknameFieldFocused, mouseX - accountPanelOffsetX, mouseY, currentTime);
        }

        float leftPanelBottomY = startY + LEFT_PANEL_TOP_HEIGHT + GAP;
        float activeSessionOffsetY = (1f - alpha) * SLIDE_DISTANCE;

        if (alpha > 0.01f) {
            accountRenderer.renderLeftPanelBottom(leftPanelX, leftPanelBottomY + activeSessionOffsetY, LEFT_PANEL_WIDTH, LEFT_PANEL_BOTTOM_HEIGHT,
                    alpha, accountConfig.getActiveAccountName(), accountConfig.getActiveAccountDate(), accountConfig.getActiveAccountSkin());
        }

        float rightPanelX = startX + LEFT_PANEL_WIDTH + GAP;
        float rightPanelY = startY;

        int guiScale = (int) FIXED_GUI_SCALE;

        List<AccountEntry> sortedAccounts = accountConfig.getSortedAccounts();
        float accountsListOffsetX = (1f - alpha) * SLIDE_DISTANCE;

        if (alpha > 0.01f) {
            accountRenderer.renderRightPanel(rightPanelX + accountsListOffsetX, rightPanelY, RIGHT_PANEL_WIDTH, RIGHT_PANEL_HEIGHT,
                    alpha, sortedAccounts, scrollOffset, mouseX - accountsListOffsetX, mouseY, 1f, guiScale);
        }
    }

    private void updateButtonAnimations(float deltaTime) {
        for (int i = 0; i < 5; i++) {
            float targetHover = (hoveredButton == i) ? 1f : 0f;
            buttonHoverProgress[i] = MathHelper.lerp(deltaTime * 10f, buttonHoverProgress[i], targetHover);

            float targetScale = (hoveredButton == i) ? 1.08f : 1f;
            buttonScales[i] = MathHelper.lerp(deltaTime * 12f, buttonScales[i], targetScale);
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

        String dateText = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.ENGLISH)
        );
        int dateAlpha = (int) (opacity * 220);
        
        float dateY = centerY + textHeight / 2f + 4;
        float dateWidth = Fonts.BOLD.getWidth(dateText, 12f);
        float dateX = centerX - dateWidth / 2f;
        
        float lineWidth = dateWidth + 20;
        float lineHeight = 0.5f;
        float lineY = dateY - 3f;
        int lineAlpha = (int)(dateAlpha * 0.4f);
        int lineColor = withAlpha(0x80a0c0, lineAlpha);
        
        Render2D.gradientRect(centerX - lineWidth / 2f, lineY, lineWidth, lineHeight,
                new int[]{
                        withAlpha(0x406080, 0),
                        lineColor,
                        lineColor,
                        withAlpha(0x406080, 0)
                }, 0);
        
        Fonts.BOLD.draw(dateText, dateX, dateY, 12f, withAlpha(0xc0d0e0, dateAlpha));
    }

    private void renderButtons(float mouseX, float mouseY, float opacity, int screenWidth, int screenHeight, float menuProgress, float extraSlideOffset) {
        float totalWidth = BUTTON_SIZE * 5 + BUTTON_SPACING * 4;
        float startX = (screenWidth - totalWidth) / 2f;

        float slideOffset = (1f - menuProgress) * 60f + extraSlideOffset;
        float centerY = screenHeight / 2f + 30 + slideOffset;

        for (int i = 0; i < 5; i++) {
            float buttonDelay = i * 0.12f;
            float buttonProgress = MathHelper.clamp((menuProgress - buttonDelay) / (1f - buttonDelay * 0.5f), 0f, 1f);
            float easedProgress = easeOutCubic(buttonProgress);

            float buttonX = startX + i * (BUTTON_SIZE + BUTTON_SPACING);
            float buttonOpacity = opacity * easedProgress;

            renderCircleButton(i, buttonX, centerY, buttonOpacity);
        }
    }

    private void renderCircleButton(int index, float x, float y, float opacity) {
        if (opacity < 0.01f) return;

        float scaleVal = buttonScales[index];
        float hoverProgress = buttonHoverProgress[index];

        float size = BUTTON_SIZE * scaleVal;
        float halfSize = size / 2f;
        float centerX = x + BUTTON_SIZE / 2f;
        float centerY = y + BUTTON_SIZE / 2f;
        float drawX = centerX - halfSize;
        float drawY = centerY - halfSize;
        float radius = size / 2f;

        int bgAlpha = (int) (opacity * 140);
        int headerAlpha = (int) (opacity * (170 + hoverProgress * 60));
        int outlineAlpha = (int) (opacity * (180 + hoverProgress * 75));
        int blurAlpha = (int) (opacity * 100);

        int bgTopLeft, bgTopRight, bgBottomLeft, bgBottomRight;
        int outlineColor;
        int iconColor;

        if (index == 4) {
            float redLerp = exitButtonRedProgress;
            int rBg = (int) MathHelper.lerp(redLerp, 0x18, 0x30);
            int gBg = (int) MathHelper.lerp(redLerp, 0x1a, 0x18);
            int bBg = (int) MathHelper.lerp(redLerp, 0x22, 0x1a);

            bgTopLeft = withAlpha((rBg << 16) | (gBg << 8) | bBg, headerAlpha);
            bgTopRight = withAlpha((rBg + 6 << 16) | (gBg + 6 << 8) | bBg + 6, headerAlpha);
            bgBottomLeft = withAlpha((rBg - 6 << 16) | (gBg - 6 << 8) | bBg - 6, headerAlpha);
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
            long currentTime = Util.getMeasuringTimeMs();
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

        int blurTint = withAlpha(0x080a12, blurAlpha);
        Render2D.blur(drawX, drawY, size, size, BLUR_RADIUS, radius, blurTint);

        int[] bgColors = {bgTopLeft, bgTopRight, bgBottomRight, bgBottomLeft};
        Render2D.gradientRect(drawX, drawY, size, size, bgColors, radius);

        Render2D.outline(drawX, drawY, size, size, OUTLINE_THICKNESS + hoverProgress * 0.5f, outlineColor, radius);

        if (hoverProgress > 0.01f) {
            float glowSize = size * (1.0f + hoverProgress * 0.15f);
            float glowX = centerX - glowSize / 2f;
            float glowY = centerY - glowSize / 2f;
            int glowAlpha = (int)(opacity * hoverProgress * 40);
            int glowColor = withAlpha(0x4060a0, glowAlpha);
            Render2D.blur(glowX, glowY, glowSize, glowSize, BLUR_RADIUS * 1.5f, radius * 1.2f, glowColor);
        }

        float iconSize = 17f * scaleVal;
        String icon = BUTTON_ICONS[index];
        float iconWidth = Fonts.MAINMENUSCREEN.getWidth(icon, iconSize);
        float iconHeight = Fonts.MAINMENUSCREEN.getHeight(iconSize);

        Fonts.MAINMENUSCREEN.draw(icon, centerX - iconWidth / 2f + 0.5f, centerY - iconHeight / 2f, iconSize, iconColor);
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

            if (dx * dx + dy * dy <= (BUTTON_SIZE / 2f) * (BUTTON_SIZE / 2f)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (transitionPhase != TransitionPhase.NONE) return false;

        float scaledMouseX = toFixedCoord(click.x());
        float scaledMouseY = toFixedCoord(click.y());

        if (currentView == View.MAIN_MENU) {
            if (!isUnlocked) {
                unlock();
                return true;
            }

            if (click.button() == 0 && hoveredButton >= 0) {
                handleMainMenuButtonClick(hoveredButton);
                return true;
            }
        } else if (currentView == View.ALT_SCREEN) {
            return handleAltScreenClick(scaledMouseX, scaledMouseY, click);
        }

        return super.mouseClicked(click, doubled);
    }

    private void handleMainMenuButtonClick(int index) {
        switch (index) {
            case 0 -> this.client.setScreen(new SelectWorldScreen(this));
            case 1 -> {
                Screen screen = this.client.options.skipMultiplayerWarning
                        ? new MultiplayerScreen(this)
                        : new MultiplayerWarningScreen(this);
                this.client.setScreen(screen);
            }
            case 2 -> switchToView(View.ALT_SCREEN);
            case 3 -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            case 4 -> this.client.scheduleStop();
        }
    }

    private boolean handleAltScreenClick(float mouseX, float mouseY, Click click) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float totalWidth = LEFT_PANEL_WIDTH + GAP + RIGHT_PANEL_WIDTH;
        float totalHeight = LEFT_PANEL_TOP_HEIGHT + GAP + LEFT_PANEL_BOTTOM_HEIGHT;

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        float startX = centerX - totalWidth / 2f;
        float startY = centerY - totalHeight / 2f;

        float leftPanelX = startX;
        float leftPanelTopY = startY;

        float fieldX = leftPanelX + 5;
        float fieldY = leftPanelTopY + 38;
        float fieldHeight = 14;
        float addButtonSize = 14;
        float buttonGap = 3;
        float fieldWidth = LEFT_PANEL_WIDTH - 10 - addButtonSize - buttonGap;

        if (accountRenderer.isMouseOver(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight)) {
            nicknameFieldFocused = true;
            return true;
        } else {
            nicknameFieldFocused = false;
        }

        float addButtonX = fieldX + fieldWidth + buttonGap;
        float addButtonY = fieldY;

        if (accountRenderer.isMouseOver(mouseX, mouseY, addButtonX, addButtonY, addButtonSize, addButtonSize)) {
            if (!nicknameText.isEmpty()) {
                addAccount(nicknameText);
                nicknameText = "";
            }
            return true;
        }

        float buttonWidth = LEFT_PANEL_WIDTH - 10;
        float buttonHeight = 16;

        float randomButtonX = leftPanelX + 5;
        float randomButtonY = fieldY + fieldHeight + 6;

        if (accountRenderer.isMouseOver(mouseX, mouseY, randomButtonX, randomButtonY, buttonWidth, buttonHeight)) {
            String randomNick = generateRandomNickname();
            addAccount(randomNick);
            nicknameText = "";
            return true;
        }

        float clearButtonX = leftPanelX + 5;
        float clearButtonY = randomButtonY + buttonHeight + 5;

        if (accountRenderer.isMouseOver(mouseX, mouseY, clearButtonX, clearButtonY, buttonWidth, buttonHeight)) {
            accountConfig.clearAllAccounts();
            targetScrollOffset = 0f;
            scrollOffset = 0f;
            return true;
        }

        float rightPanelX = startX + LEFT_PANEL_WIDTH + GAP;
        float rightPanelY = startY;

        float accountListX = rightPanelX + 5;
        float accountListY = rightPanelY + 26;
        float accountListWidth = RIGHT_PANEL_WIDTH - 10;
        float accountListHeight = RIGHT_PANEL_HEIGHT - 31;

        if (!accountRenderer.isMouseOver(mouseX, mouseY, accountListX, accountListY, accountListWidth, accountListHeight)) {
            return false;
        }

        float cardWidth = (accountListWidth - 5) / 2f;
        float cardHeight = 40;
        float cardGap = 5;

        List<AccountEntry> sortedAccounts = accountConfig.getSortedAccounts();

        for (int i = 0; i < sortedAccounts.size(); i++) {
            int col = i % 2;
            int row = i / 2;

            float cardX = accountListX + col * (cardWidth + cardGap);
            float cardY = accountListY + row * (cardHeight + cardGap) - scrollOffset;

            if (cardY + cardHeight < accountListY || cardY > accountListY + accountListHeight) {
                continue;
            }

            float btnSize = 12;
            float buttonYPos = cardY + cardHeight - btnSize - 5;
            float pinButtonX = cardX + cardWidth - btnSize * 2 - 8;
            float deleteButtonX = cardX + cardWidth - btnSize - 5;

            if (accountRenderer.isMouseOver(mouseX, mouseY, pinButtonX, buttonYPos, btnSize, btnSize)) {
                AccountEntry entry = sortedAccounts.get(i);
                entry.togglePinned();
                if (entry.isPinned()) {
                    setActiveAccount(entry);
                }
                accountConfig.save();
                return true;
            }

            if (accountRenderer.isMouseOver(mouseX, mouseY, deleteButtonX, buttonYPos, btnSize, btnSize)) {
                accountConfig.removeAccountByIndex(i);
                return true;
            }

            if (accountRenderer.isMouseOver(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight)) {
                setActiveAccount(sortedAccounts.get(i));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentView != View.ALT_SCREEN || transitionPhase != TransitionPhase.NONE) return false;

        float scaledMouseX = toFixedCoord(mouseX);
        float scaledMouseY = toFixedCoord(mouseY);
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float totalWidth = LEFT_PANEL_WIDTH + GAP + RIGHT_PANEL_WIDTH;
        float totalHeight = LEFT_PANEL_TOP_HEIGHT + GAP + LEFT_PANEL_BOTTOM_HEIGHT;

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        float startX = centerX - totalWidth / 2f;
        float startY = centerY - totalHeight / 2f;

        float rightPanelX = startX + LEFT_PANEL_WIDTH + GAP;
        float rightPanelY = startY;

        if (accountRenderer.isMouseOver(scaledMouseX, scaledMouseY, rightPanelX, rightPanelY, RIGHT_PANEL_WIDTH, RIGHT_PANEL_HEIGHT)) {
            float cardHeight = 40;
            float cardGap = 5;
            float accountListHeight = RIGHT_PANEL_HEIGHT - 31;
            int rows = (int) Math.ceil(accountConfig.getSortedAccounts().size() / 2.0);
            float maxScroll = Math.max(0, rows * (cardHeight + cardGap) - accountListHeight);

            targetScrollOffset -= (float) verticalAmount * 25;
            targetScrollOffset = MathHelper.clamp(targetScrollOffset, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (transitionPhase != TransitionPhase.NONE) return false;

        if (currentView == View.MAIN_MENU) {
            if (!isUnlocked) {
                unlock();
                return true;
            }
        } else if (currentView == View.ALT_SCREEN) {
            if (nicknameFieldFocused) {
                int keyCode = input.key();

                if (keyCode == 259) {
                    if (!nicknameText.isEmpty()) {
                        nicknameText = nicknameText.substring(0, nicknameText.length() - 1);
                    }
                    return true;
                }

                if (keyCode == 256) {
                    nicknameFieldFocused = false;
                    return true;
                }

                if (keyCode == 257 || keyCode == 335) {
                    if (!nicknameText.isEmpty()) {
                        addAccount(nicknameText);
                        nicknameText = "";
                    }
                    nicknameFieldFocused = false;
                    return true;
                }
            }

            if (input.key() == 256) {
                switchToView(View.MAIN_MENU);
                accountConfig.save();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (currentView == View.ALT_SCREEN && nicknameFieldFocused && transitionPhase == TransitionPhase.NONE) {
            int codepoint = input.codepoint();
            if (Character.isLetterOrDigit(codepoint) || codepoint == '_') {
                if (nicknameText.length() < 16) {
                    nicknameText += Character.toString(codepoint);
                }
                return true;
            }
        }
        return super.charTyped(input);
    }

    private void setActiveAccount(AccountEntry account) {
        accountConfig.setActiveAccount(account.getName(), account.getDate(), account.getSkin());
        SessionChanger.changeUsername(account.getName());
    }

    private void addAccount(String nickname) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String date = now.format(formatter);

        AccountEntry entry = new AccountEntry(nickname, date, null);
        accountConfig.addAccount(entry);
        setActiveAccount(entry);
        SessionChanger.changeUsername(nickname);
    }

    private String generateRandomNickname() {
        java.util.Random random = new java.util.Random();
        StringBuilder username = new StringBuilder();
        char[] vowels = {'a', 'e', 'i', 'o', 'u'};
        char[] consonants = {'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};

        String finalUsername = null;
        int attempts = 0;
        final int MAX_ATTEMPTS = 10;

        List<AccountEntry> existingAccounts = accountConfig.getAccounts();

        do {
            username.setLength(0);
            int length = 6 + random.nextInt(5);
            boolean startWithVowel = random.nextBoolean();

            for (int i = 0; i < length; i++) {
                if (i % 2 == 0) {
                    username.append(startWithVowel ? vowels[random.nextInt(vowels.length)] : consonants[random.nextInt(consonants.length)]);
                } else {
                    username.append(startWithVowel ? consonants[random.nextInt(consonants.length)] : vowels[random.nextInt(vowels.length)]);
                }
            }

            if (random.nextInt(100) < 30) {
                username.append(random.nextInt(100));
            }

            String tempUsername = username.substring(0, 1).toUpperCase() + username.substring(1);
            attempts++;

            boolean exists = false;
            for (AccountEntry account : existingAccounts) {
                if (account.getName().equalsIgnoreCase(tempUsername)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                finalUsername = tempUsername;
                break;
            }

        } while (attempts < MAX_ATTEMPTS);

        if (finalUsername == null) {
            finalUsername = username.substring(0, 1).toUpperCase() + username.substring(1) + (System.currentTimeMillis() % 1000);
        }

        return finalUsername;
    }

    private Identifier getSkinTexturePath(SkinTextures skinTextures) {
        if (skinTextures == null || skinTextures.body() == null) {
            return STEVE_SKIN;
        }
        try {
            return skinTextures.body().texturePath();
        } catch (Exception e) {
            return STEVE_SKIN;
        }
    }

    private Identifier getSkinForPlayer(String playerName) {
        if (client == null || client.player == null || client.player.networkHandler == null) {
            return STEVE_SKIN;
        }
        for (PlayerListEntry entry : client.player.networkHandler.getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().name().equalsIgnoreCase(playerName)) {
                try {
                    SkinTextures skinTextures = entry.getSkinTextures();
                    Identifier skin = getSkinTexturePath(skinTextures);
                    if (skin != null && !skin.equals(STEVE_SKIN)) {
                        return skin;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return STEVE_SKIN;
    }

    private Identifier getLocalPlayerSkin() {
        if (client == null || client.player == null || client.player.networkHandler == null) {
            return STEVE_SKIN;
        }
        try {
            PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                SkinTextures skinTextures = entry.getSkinTextures();
                return getSkinTexturePath(skinTextures);
            }
        } catch (Exception ignored) {
        }
        return STEVE_SKIN;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        drawBackground(currentZoom);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }
}