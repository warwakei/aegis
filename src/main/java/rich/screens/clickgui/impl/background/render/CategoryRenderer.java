package rich.screens.clickgui.impl.background.render;

import rich.modules.module.category.ModuleCategory;
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

    private static final float ANIMATION_SPEED = 8f;
    private static final float MAX_OFFSET = 5f;
    private static final float BALL_SIZE = 3f;
    private static final float TEXT_SIZE = 6f;
    private static final float ICON_SIZE = 6f;
    private static final float ICON_SPACING = 4f;
    private static final float SECTION_TEXT_SIZE = 5f;
    private static final float EXTRA_CATEGORY_OFFSET = 10f;

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
        float change = diff * ANIMATION_SPEED * deltaTime;

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
        float lineWidth = 18f;
        float textWidth = Fonts.BOLD.getWidth(title, SECTION_TEXT_SIZE);
        float totalWidth = 65f;
        float textX = bgX + 15f + (totalWidth - textWidth) / 2f;
        float lineY = sectionY + 3f;
        int lineAlpha = (int) (40 * alphaMultiplier);
        int textAlpha = (int) (100 * alphaMultiplier);
        Render2D.rect(bgX + 15f, lineY, lineWidth, 0.5f, new Color(255, 255, 255, lineAlpha).getRGB(), 0);
        Render2D.rect(bgX + 15f + totalWidth - lineWidth, lineY, lineWidth, 0.5f, new Color(255, 255, 255, lineAlpha).getRGB(), 0);
        Fonts.BOLD.draw(title, textX, sectionY, SECTION_TEXT_SIZE, new Color(150, 150, 150, textAlpha).getRGB());
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

        int baseGray = 128;
        int targetWhite = 255;
        int colorValue = (int) (baseGray + (targetWhite - baseGray) * animation);
        int alpha = (int) ((128 + 127 * animation) * alphaMultiplier);
        Color textColor = new Color(colorValue, colorValue, colorValue, alpha);

        float iconX = bgX + 17f + offsetX;
        float iconWidth = Fonts.CATEGORY_ICONS.getWidth(icon, ICON_SIZE);
        float textX = iconX + iconWidth + ICON_SPACING;
        float textWidth = Fonts.BOLD.getWidth(name, TEXT_SIZE);

        Fonts.CATEGORY_ICONS.draw(icon, iconX, textY + 0.5f, ICON_SIZE, textColor.getRGB());

        if (animation > 0.01f) {
            float lineWidth = (iconWidth + ICON_SPACING + textWidth) * animation;
            float lineAlpha = animation * 60 * alphaMultiplier;
            Render2D.rect(iconX, textY + 9f, lineWidth, 0.5f, new Color(255, 255, 255, (int) lineAlpha).getRGB(), 0);

            float ballAlpha = animation * 200 * alphaMultiplier;
            float ballX = bgX + 12f;
            float ballY = textY + 2.5f;
            Render2D.rect(ballX, ballY, BALL_SIZE, BALL_SIZE, new Color(255, 255, 255, (int) ballAlpha).getRGB(), BALL_SIZE / 2f);
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