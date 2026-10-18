package rich.screens.clickgui.impl.module.render;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.module.handler.ModuleAnimationHandler;
import rich.screens.clickgui.impl.module.handler.ModuleScrollHandler;
import rich.screens.clickgui.impl.settingsrender.ColorComponent;
import rich.screens.clickgui.impl.settingsrender.MultiSelectComponent;
import rich.screens.clickgui.impl.settingsrender.SelectComponent;
import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsPanelRenderer {

    private static final float SETTINGS_PANEL_CORNER_RADIUS = 8f;
    private static final float CORNER_INSET = 3f;
    private static final int SETTING_HEIGHT = 18;
    private static final int SETTING_SPACING = 3;
    private static final float SEPARATOR_THICKNESS = 1.5f;
    private static final float SCROLL_FADE_SIZE = 10f; // так плавнее будет

    private static final long DESC_SCROLL_PAUSE_DURATION = 2000;
    private static final float DESC_SCROLL_PIXELS_PER_SECOND = 20f;

    private final ModuleAnimationHandler animationHandler;

    // скролл длинного описания модуля в шапке панели
    private float descScrollOffset = 0f;
    private boolean descScrollingRight = true;
    private long descScrollPauseTime = 0;
    private ModuleStructure lastDescModule = null;

    public SettingsPanelRenderer(ModuleAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void render(DrawContext context, ModuleStructure selectedModule, List<AbstractSettingComponent> settingComponents,
                       float x, float y, float width, float height, float mouseX, float mouseY, float delta,
                       int guiScale, float alphaMultiplier, ModuleScrollHandler scrollHandler, ModuleAnimationHandler animHandler) {

        animHandler.updateSettingAnimations(settingComponents);
        animHandler.updateVisibilityAnimations(settingComponents);

        float alignedX = Math.round(x * 10f) / 10f;
        float alignedY = Math.round(y * 10f) / 10f;
        float alignedW = Math.round(width * 10f) / 10f;
        float alignedH = Math.round(height * 10f) / 10f;

        Render2D.rect(alignedX, alignedY, alignedW, alignedH, ClickGuiPalette.panelList(alphaMultiplier), SETTINGS_PANEL_CORNER_RADIUS);
        Render2D.outline(alignedX, alignedY, alignedW, alignedH, 0.5f, ClickGuiPalette.border(alphaMultiplier), SETTINGS_PANEL_CORNER_RADIUS);

        if (selectedModule == null) {
            String text = "Select a module";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, ClickGuiPalette.textMuted(alphaMultiplier));
            return;
        }

        // заголовок модуля приукрашен
        Fonts.BOLD.draw(selectedModule.getName(), x + 8, y + 8, 7, ClickGuiPalette.textPrimary(alphaMultiplier));
        String desc = selectedModule.getDescription();
        if (desc != null && !desc.isEmpty()) {
            renderHeaderDescription(selectedModule, desc, x, y, width, alphaMultiplier, delta);
        }
        // полоска разделитель с акцентом
        float sepPulse = (float) (Math.sin(System.currentTimeMillis() / 2000.0) * 0.5 + 0.5);
        int sepR = (int) (60 + 46 * sepPulse);
        Render2D.rect(x + 8, y + 30, width - 16, SEPARATOR_THICKNESS,
                new Color(sepR, 130, 220, (int) (65 * alphaMultiplier)).getRGB(), 10);

        float sideInset = CORNER_INSET;
        float bottomInset = CORNER_INSET + 3;

        float clipY = y + 31;
        float clipH = height - 26 - bottomInset;

        float clipX = x + sideInset;
        float clipW = width - sideInset * 2;

        Scissor.enable(clipX, clipY, clipW, clipH, guiScale);

        List<Float> finalYPositions = new ArrayList<>();
        List<Float> animatedHeights = new ArrayList<>();
        float posY = y + 38f + (float) scrollHandler.getSettingDisplayScroll();

        for (AbstractSettingComponent c : settingComponents) {
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (heightAnim <= 0.001f) {
                finalYPositions.add(null);
                animatedHeights.add(0f);
                continue;
            }

            finalYPositions.add(posY);

            float baseHeight = getComponentBaseHeight(c);
            float layoutHeight = baseHeight * heightAnim;
            animatedHeights.add(layoutHeight);
            posY += layoutHeight + SETTING_SPACING * heightAnim;
        }

        float visibleTop = clipY;
        float visibleBottom = clipY + clipH;

        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent c = settingComponents.get(i);
            Float startY = finalYPositions.get(i);

            if (startY == null) continue;

            float visAnim = animHandler.getVisibilityAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (visAnim <= 0.001f && heightAnim <= 0.001f) continue;

            float animatedHeight = animatedHeights.get(i);

            float progress = animHandler.getSettingAnimations().getOrDefault(c, 1f);
            float componentAlpha = progress * visAnim * alphaMultiplier;

            c.position(x + 8, startY);
            c.size(width - 16f, SETTING_HEIGHT);
            c.setAlphaMultiplier(componentAlpha);

            if (startY + animatedHeight >= visibleTop && startY <= visibleBottom && componentAlpha > 0.01f) {
                float itemClipTop = Math.max(startY, visibleTop);
                float itemClipBottom = Math.min(startY + animatedHeight, visibleBottom);
                float itemClipHeight = itemClipBottom - itemClipTop;

                if (itemClipHeight > 0.5f) {
                    Scissor.enable(clipX, itemClipTop, clipW, itemClipHeight, guiScale);
                    context.getMatrices().pushMatrix();
                    c.render(context, (int) mouseX, (int) mouseY, delta);
                    context.getMatrices().popMatrix();
                    Scissor.disable();
                }
            }
        }

        Scissor.disable();

        boolean hasVisibleSettings = false;
        for (AbstractSettingComponent c : settingComponents) {
            float visAnim = animHandler.getVisibilityAnimations().getOrDefault(c, 0f);
            if (visAnim > 0.01f) {
                hasVisibleSettings = true;
                break;
            }
        }

        if (!hasVisibleSettings) {
            String text = "This module doesn't have settings";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f + 10f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, ClickGuiPalette.textMuted(alphaMultiplier));
        }

        renderScrollFade(x + sideInset, clipY, width - sideInset * 2, clipH,
                scrollHandler.getSettingScrollTopFade() * alphaMultiplier,
                scrollHandler.getSettingScrollBottomFade() * alphaMultiplier, 60, 12);
    }

    public float calculateTotalHeight(List<AbstractSettingComponent> settingComponents, ModuleAnimationHandler animHandler) {
        float total = 0;
        for (AbstractSettingComponent c : settingComponents) {
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            if (heightAnim <= 0.001f) continue;

            float baseHeight = getComponentBaseHeight(c);
            total += (baseHeight + SETTING_SPACING) * heightAnim;
        }
        return total;
    }

    private float getComponentBaseHeight(AbstractSettingComponent c) {
        if (c instanceof SelectComponent) return ((SelectComponent) c).getTotalHeight();
        if (c instanceof MultiSelectComponent) return ((MultiSelectComponent) c).getTotalHeight();
        if (c instanceof ColorComponent) return ((ColorComponent) c).getTotalHeight();
        return SETTING_HEIGHT;
    }

    /**
     * Описание модуля в шапке: если текст влезает — рисуем статично,
     * если нет — прокрутка туда-обратно (как в настройках модулей).
     */
    private void renderHeaderDescription(ModuleStructure module, String desc, float x, float y, float width, float alphaMultiplier, float deltaTime) {
        float iconX = x + 8;
        float descX = x + 15;
        float descY = y + 20;

        float availableWidth = (x + width - 8) - descX;
        float descWidth = Fonts.BOLD.getWidth(desc, 5);

        if (module != lastDescModule) {
            descScrollOffset = 0f;
            descScrollingRight = true;
            descScrollPauseTime = 0;
            lastDescModule = module;
        }

        Fonts.GUI_ICONS.draw("C", iconX, descY, 6, ClickGuiPalette.textMuted(alphaMultiplier));

        if (descWidth <= availableWidth) {
            descScrollOffset = 0f;
            Fonts.BOLD.draw(desc, descX, descY, 5, ClickGuiPalette.textMuted(alphaMultiplier));
            return;
        }

        updateDescScroll(deltaTime, descWidth, availableWidth);

        float maxScroll = descWidth - availableWidth + 5;
        float currentScroll = descScrollOffset * maxScroll;

        Scissor.enable(descX - 1, descY - 2, availableWidth + 3, 10, 2);
        Fonts.BOLD.draw(desc, descX - currentScroll, descY, 5, ClickGuiPalette.textMuted(alphaMultiplier));
        Scissor.disable();
    }

    private void updateDescScroll(float deltaTime, float textWidth, float availableWidth) {
        long currentTime = System.currentTimeMillis();

        if (descScrollPauseTime > 0) {
            if (currentTime - descScrollPauseTime < DESC_SCROLL_PAUSE_DURATION) {
                return;
            }
            descScrollPauseTime = 0;
        }

        float scrollDistance = textWidth - availableWidth + 5;
        float scrollSpeed = DESC_SCROLL_PIXELS_PER_SECOND / scrollDistance;

        if (descScrollingRight) {
            descScrollOffset += deltaTime * scrollSpeed;
            if (descScrollOffset >= 1f) {
                descScrollOffset = 1f;
                descScrollingRight = false;
                descScrollPauseTime = currentTime;
            }
        } else {
            descScrollOffset -= deltaTime * scrollSpeed;
            if (descScrollOffset <= 0f) {
                descScrollOffset = 0f;
                descScrollingRight = true;
                descScrollPauseTime = currentTime;
            }
        }
    }

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade, int alpha, int size) {
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float smoothProgress = (float) Math.sin((i / (float) size) * Math.PI / 2);
                float fadeAlpha = alpha * topFade * smoothProgress;
                Render2D.rect(x, y + i, w, 1, new Color(0, 0, 0, (int) fadeAlpha).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float smoothProgress = (float) Math.sin((i / (float) size) * Math.PI / 2);
                float fadeAlpha = alpha * bottomFade * smoothProgress;
                Render2D.rect(x, y + h - size + i, w, 1, new Color(0, 0, 0, (int) fadeAlpha).getRGB(), 0);
            }
        }
    }
}