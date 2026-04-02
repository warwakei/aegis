package rich.screens.clickgui.impl.module.render;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.module.handler.ModuleAnimationHandler;
import rich.screens.clickgui.impl.module.handler.ModuleScrollHandler;
import rich.screens.clickgui.impl.settingsrender.ColorComponent;
import rich.screens.clickgui.impl.settingsrender.MultiSelectComponent;
import rich.screens.clickgui.impl.settingsrender.SelectComponent;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsPanelRenderer {

    private static final float SETTINGS_PANEL_CORNER_RADIUS = 7f;
    private static final float CORNER_INSET = 3f;
    private static final int SETTING_HEIGHT = 16;
    private static final int SETTING_SPACING = 2;

    private final ModuleAnimationHandler animationHandler;

    public SettingsPanelRenderer(ModuleAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void render(DrawContext context, ModuleStructure selectedModule, List<AbstractSettingComponent> settingComponents,
                       float x, float y, float width, float height, float mouseX, float mouseY, float delta,
                       int guiScale, float alphaMultiplier, ModuleScrollHandler scrollHandler, ModuleAnimationHandler animHandler) {

        animHandler.updateSettingAnimations(settingComponents);
        animHandler.updateVisibilityAnimations(settingComponents);

        int panelAlpha = (int) (15 * alphaMultiplier);
        int outlineAlpha = (int) (215 * alphaMultiplier);
        Render2D.rect(x, y, width, height, new Color(64, 64, 64, panelAlpha).getRGB(), SETTINGS_PANEL_CORNER_RADIUS);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), SETTINGS_PANEL_CORNER_RADIUS);

        if (selectedModule == null) {
            String text = "Select a module";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, (int) (150 * alphaMultiplier)).getRGB());
            return;
        }

        Fonts.BOLD.draw(selectedModule.getName(), x + 8, y + 8, 7, new Color(255, 255, 255, (int) (200 * alphaMultiplier)).getRGB());
        String desc = selectedModule.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Fonts.BOLD.draw(desc.length() > 52 ? desc.substring(0, 55) + "..." : desc, x + 15, y + 20, 5, new Color(128, 128, 128, (int) (150 * alphaMultiplier)).getRGB());
            Fonts.GUI_ICONS.draw("C", x + 8, y + 20, 6, new Color(128, 128, 128, (int) (150 * alphaMultiplier)).getRGB());
        }
        Render2D.rect(x + 8, y + 30, width - 16, 1.25f, new Color(64, 64, 64, (int) (64 * alphaMultiplier)).getRGB(), 10);

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
            Fonts.BOLD.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, (int) (150 * alphaMultiplier)).getRGB());
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

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade, int alpha, int size) {
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = alpha * topFade * (1f - i / (float) size);
                Render2D.rect(x, y + i, w, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = alpha * bottomFade * (i / (float) size);
                Render2D.rect(x, y + h - size + i, w, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
            }
        }
    }
}