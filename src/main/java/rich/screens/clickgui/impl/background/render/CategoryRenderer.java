package rich.screens.clickgui.impl.background.render;

import rich.modules.module.category.ModuleCategory;
import rich.screens.clickgui.theme.ClickGuiPalette;
import rich.util.animations.Easings;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CategoryRenderer {

    private static final ModuleCategory[] MAIN_CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final String[] MAIN_CATEGORY_NAMES = {"Combat", "Movement", "Render", "Player", "Util"};
    private static final String[] MAIN_CATEGORY_ICONS = {"a", "b", "c", "d", "e"};

    private static final ModuleCategory[] EXTRA_CATEGORIES = {
            ModuleCategory.AUTOBUY
    };
    private static final String[] EXTRA_CATEGORY_NAMES = {"AutoBuy"};
    private static final String[] EXTRA_CATEGORY_ICONS = {"g"};

    private final Map<ModuleCategory, Float> categoryAnimations = new HashMap<>();

    private static final float ANIMATION_SPEED = 14f;
    private static final float MAX_OFFSET = 3f;
    private static final float TEXT_SIZE = 6.5f;
    private static final float ICON_SIZE = 6.5f;
    private static final float ICON_SPACING = 4f;
    private static final float SECTION_TEXT_SIZE = 5f;
    private static final float EXTRA_CATEGORY_OFFSET = 10f;

    private static final float BAR_W = 3.5f;
    private static final float BAR_PAD = 9f;

    public CategoryRenderer() {
        for (ModuleCategory cat : MAIN_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
        for (ModuleCategory cat : EXTRA_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
    }

    public void updateAnimations(ModuleCategory selectedCategory, float deltaTime) {
        for (ModuleCategory cat : MAIN_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
        for (ModuleCategory cat : EXTRA_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
    }

    private void updateCategoryAnimation(ModuleCategory cat, ModuleCategory selected, float deltaTime) {
        float target = cat == selected ? 1f : 0f;
        float current = categoryAnimations.getOrDefault(cat, 0f);
        float diff = target - current;
        float rawProgress = Math.min(1f, ANIMATION_SPEED * deltaTime);
        float springProgress = (float) Easings.SPRING.ease(rawProgress);
        float change = diff * springProgress;

        if (Math.abs(diff) < 0.001f) {
            categoryAnimations.put(cat, target);
        } else {
            categoryAnimations.put(cat, current + change);
        }
    }

    public void render(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        renderSectionHeader(bgX, bgY + 52f, "Основные", alphaMultiplier);
        renderMainCategories(bgX, bgY, alphaMultiplier);
        renderSectionHeader(bgX, bgY + 62f + MAIN_CATEGORY_NAMES.length * 15f + 10f - EXTRA_CATEGORY_OFFSET, "Другие", alphaMultiplier);
        renderExtraCategories(bgX, bgY, alphaMultiplier);
    }

    private void renderSectionHeader(float bgX, float sectionY, String title, float alphaMultiplier) {
        float lineWidth = 14f;
        float textWidth = Fonts.BOLD.getWidth(title, SECTION_TEXT_SIZE);
        float totalWidth = 65f;
        float textX = bgX + 15f + (totalWidth - textWidth) / 2f;
        float lineY = sectionY + 3f;
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 3000.0) * 0.5 + 0.5);
        int lineAlpha = (int) ((28 + 14 * pulse) * alphaMultiplier);
        int textAlpha = (int) ((100 + 30 * pulse) * alphaMultiplier);
        int lineR = (int) (40 + 30 * pulse);
        Render2D.rect(bgX + 15f, lineY, lineWidth, 0.5f, new Color(lineR, 80 + (int)(20*pulse), 130 + (int)(30*pulse), lineAlpha).getRGB(), 0);
        Render2D.rect(bgX + 15f + totalWidth - lineWidth, lineY, lineWidth, 0.5f, new Color(lineR, 80 + (int)(20*pulse), 130 + (int)(30*pulse), lineAlpha).getRGB(), 0);
        Fonts.BOLD.draw(title, textX, sectionY, SECTION_TEXT_SIZE, new Color(120 + (int)(30*pulse), 128 + (int)(20*pulse), 150 + (int)(30*pulse), textAlpha).getRGB());
    }

    private void renderMainCategories(float bgX, float bgY, float alphaMultiplier) {
        for (int i = 0; i < MAIN_CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = MAIN_CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);
            float textY = bgY + 65f + i * 15f;
            renderCategoryItem(bgX, textY, MAIN_CATEGORY_NAMES[i], MAIN_CATEGORY_ICONS[i], animation, alphaMultiplier);
        }
    }

    private void renderExtraCategories(float bgX, float bgY, float alphaMultiplier) {
        float separatorY = bgY + 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        float extraStartY = separatorY + 18f - EXTRA_CATEGORY_OFFSET;

        for (int i = 0; i < EXTRA_CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = EXTRA_CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);
            float textY = extraStartY + i * 15f;
            renderCategoryItem(bgX, textY, EXTRA_CATEGORY_NAMES[i], EXTRA_CATEGORY_ICONS[i], animation, alphaMultiplier);
        }
    }

    private void renderCategoryItem(float bgX, float textY, String name, String icon, float animation, float alphaMultiplier) {
        float offsetX = animation * MAX_OFFSET;
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 1200.0) * 0.5 + 0.5);

        int alpha = (int) ((140 + 115 * animation) * alphaMultiplier);

        int colorR = (int) (135 + 103 * animation + 18 * pulse * animation);
        int colorG = (int) (138 + 110 * animation + 14 * pulse * animation);
        int colorB = (int) (150 + 98 * animation + 36 * pulse * animation);
        Color textColor = new Color(
                Math.min(255, colorR),
                Math.min(255, colorG),
                Math.min(255, colorB),
                alpha
        );

        float iconX = bgX + 17f + offsetX;
        float iconWidth = Fonts.CATEGORY_ICONS.getWidth(icon, ICON_SIZE);
        float textX = iconX + iconWidth + ICON_SPACING;

        Fonts.CATEGORY_ICONS.draw(icon, iconX, textY + 0.5f, ICON_SIZE, textColor.getRGB());

        if (animation > 0.02f) {
            int barA = (int) ((160 + 60 * pulse) * animation * alphaMultiplier);
            float textH = Fonts.BOLD.getHeight(TEXT_SIZE);
            float barH = (textH + 2.0f) + animation * 2f;
            float barY = textY - 1.5f;
            int r = (int) (66 + 40 * pulse);
            int g = (int) (100 + 56 * pulse);
            int b = (int) (180 + 50 * pulse);
            Render2D.rect(bgX + BAR_PAD, barY, BAR_W, barH, new Color(r, g, b, barA).getRGB(), 1.5f);

            // Внешнее свечение бара
            if (animation > 0.3f) {
                int glowA = (int) (40 * animation * pulse * alphaMultiplier);
                Render2D.outline(bgX + BAR_PAD - 1.5f, barY - 1.5f, BAR_W + 3, barH + 3, 3f,
                        new Color(66, 130, 220, glowA).getRGB(), 2.5f);
            }
        }

        Fonts.BOLD.draw(name, textX, textY, TEXT_SIZE, textColor.getRGB());
    }

    public ModuleCategory getCategoryAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        if (mouseX < bgX + 10f || mouseX > bgX + 95f) return null;

        for (int i = 0; i < MAIN_CATEGORY_NAMES.length; i++) {
            float catY = 65f + i * 15f;
            if (mouseY >= bgY + catY && mouseY <= bgY + catY + 13f) {
                return MAIN_CATEGORIES[i];
            }
        }

        float separatorY = 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        float extraStartY = separatorY + 18f - EXTRA_CATEGORY_OFFSET;

        for (int i = 0; i < EXTRA_CATEGORIES.length; i++) {
            float catY = extraStartY + i * 15f;
            if (mouseY >= bgY + catY && mouseY <= bgY + catY + 13f) {
                return EXTRA_CATEGORIES[i];
            }
        }

        return null;
    }
}
