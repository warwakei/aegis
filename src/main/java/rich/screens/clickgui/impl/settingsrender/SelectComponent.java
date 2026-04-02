package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectComponent extends AbstractSettingComponent {
    private final SelectSetting selectSetting;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private float hoverAnimation = 0f;

    private float descScrollOffset = 0f;
    private boolean descScrollingRight = true;
    private long descScrollPauseTime = 0;

    private float arrowRotation = 0f;

    private final Map<String, Float> optionHoverAnimations = new HashMap<>();
    private final Map<String, Float> selectAnimations = new HashMap<>();

    private String previousSelected = "";
    private float selectedTextAlpha = 1f;
    private float selectedTextSlide = 1f;
    private float newSelectedTextAlpha = 0f;
    private float newSelectedTextSlide = 0f;
    private String animatingFromText = "";
    private boolean isAnimatingSelection = false;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float COLLAPSE_SPEED = 15f;
    private static final float BOX_WIDTH = 65f;
    private static final float OPTION_HEIGHT = 14f;
    private static final long SCROLL_PAUSE_DURATION = 2000;
    private static final float SCROLL_PIXELS_PER_SECOND = 20f;
    private static final float DESC_PADDING = 8f;
    private static final float SELECTION_ANIMATION_SPEED = 10f;

    public SelectComponent(SelectSetting setting) {
        super(setting);
        this.selectSetting = setting;
        this.previousSelected = setting.getSelected();
        for (String option : setting.getList()) {
            optionHoverAnimations.put(option, 0f);
            selectAnimations.put(option, setting.isSelected(option) ? 1f : 0f);
        }
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    private float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            return target;
        }
        return current + diff * Math.min(speed, 1f);
    }

    private void updateSelectionAnimation(float deltaTime) {
        String currentSelected = selectSetting.getSelected();

        if (!currentSelected.equals(previousSelected) && !isAnimatingSelection) {
            animatingFromText = previousSelected;
            isAnimatingSelection = true;
            selectedTextAlpha = 1f;
            selectedTextSlide = 1f;
            newSelectedTextAlpha = 0f;
            newSelectedTextSlide = 0f;
        }

        if (isAnimatingSelection) {
            selectedTextAlpha = lerp(selectedTextAlpha, 0f, deltaTime * SELECTION_ANIMATION_SPEED);
            selectedTextSlide = lerp(selectedTextSlide, 0f, deltaTime * SELECTION_ANIMATION_SPEED);

            if (selectedTextAlpha < 0.5f) {
                newSelectedTextAlpha = lerp(newSelectedTextAlpha, 1f, deltaTime * SELECTION_ANIMATION_SPEED);
                newSelectedTextSlide = lerp(newSelectedTextSlide, 1f, deltaTime * SELECTION_ANIMATION_SPEED);
            }

            if (newSelectedTextAlpha > 0.99f && newSelectedTextSlide > 0.99f) {
                isAnimatingSelection = false;
                previousSelected = currentSelected;
                selectedTextAlpha = 1f;
                selectedTextSlide = 1f;
                newSelectedTextAlpha = 1f;
                newSelectedTextSlide = 1f;
            }
        } else {
            previousSelected = currentSelected;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaTime = getDeltaTime();

        updateSelectionAnimation(deltaTime);

        boolean mainHovered = isMainHover(mouseX, mouseY);
        hoverAnimation = lerp(hoverAnimation, mainHovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);

        float expandSpeed = expanded ? ANIMATION_SPEED : COLLAPSE_SPEED;
        expandAnimation = lerp(expandAnimation, expanded ? 1f : 0f, deltaTime * expandSpeed);

        float targetRotation = expanded ? 90f : 0f;
        arrowRotation = lerp(arrowRotation, targetRotation, deltaTime * ANIMATION_SPEED);

        Fonts.GUI_ICONS.draw("J", x - 0.5f, y + height / 2 - 8.5f, 9, applyAlpha(new Color(210, 210, 210, 200)).getRGB());

        Fonts.BOLD.draw(selectSetting.getName(), x + 9.5f, y + height / 2 - 7.5f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        String description = selectSetting.getDescription();
        if (description != null && !description.isEmpty()) {
            renderScrollingDescription(description, deltaTime);
        }

        float boxX = x + width - BOX_WIDTH - 2;
        float boxY = y + height / 2 - 5;
        float boxHeight = 10f;

        int bgAlpha = 25 + (int)(hoverAnimation * 15);
        Render2D.rect(boxX, boxY, BOX_WIDTH, boxHeight, applyAlpha(new Color(55, 55, 55, bgAlpha)).getRGB(), 3f);

        int outlineAlpha = 60 + (int)(hoverAnimation * 40);
        Render2D.outline(boxX, boxY, BOX_WIDTH, boxHeight, 0.5f, applyAlpha(new Color(155, 155, 155, outlineAlpha)).getRGB(), 3f);

        renderAnimatedSelectedText(boxX, boxY, boxHeight);

        renderArrowIcon(boxX + BOX_WIDTH - 8, boxY + boxHeight / 2 - 4f);

        if (expandAnimation > 0.01f) {
            renderExpandedOptions(context, mouseX, mouseY, boxX, boxY + boxHeight + 2, deltaTime);
        }
    }

    private void renderArrowIcon(float iconX, float iconY) {
        int arrowAlpha = 120 + (int)(hoverAnimation * 60);

        float centerX = iconX + 4f;
        float centerY = iconY + 4f;

        float rad = (float) Math.toRadians(arrowRotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float offsetX = -4f;
        float offsetY = -4f;

        float rotatedX = centerX + (offsetX * cos - offsetY * sin);
        float rotatedY = centerY + (offsetX * sin + offsetY * cos);

//        Fonts.GUI_ICONS.draw("W", rotatedX, rotatedY, 8, applyAlpha(new Color(180, 180, 185, arrowAlpha)).getRGB());
    }

    private void renderAnimatedSelectedText(float boxX, float boxY, float boxHeight) {
        float maxTextWidth = BOX_WIDTH - 14;
        float textY = boxY + boxHeight / 2 - 2.5f;

        Scissor.enable(boxX + 2, boxY, maxTextWidth + 2, boxHeight,2);

        if (isAnimatingSelection) {
            if (selectedTextAlpha > 0.01f) {
                String displayOld = truncateText(animatingFromText, maxTextWidth);
                float slideOffset = (1f - selectedTextSlide) * -15f;
                int alpha = (int)(200 * selectedTextAlpha * alphaMultiplier);
                Fonts.BOLD.draw(displayOld, boxX + 4 + slideOffset, textY, 5, new Color(160, 160, 165, alpha).getRGB());
            }

            if (newSelectedTextAlpha > 0.01f) {
                String selected = selectSetting.getSelected();
                String displayNew = truncateText(selected, maxTextWidth);
                float slideOffset = (1f - newSelectedTextSlide) * 20f;
                int alpha = (int)(200 * newSelectedTextAlpha * alphaMultiplier);
                Fonts.BOLD.draw(displayNew, boxX + 4 + slideOffset, textY, 5, new Color(160, 160, 165, alpha).getRGB());
            }
        } else {
            String selected = selectSetting.getSelected();
            String displaySelected = truncateText(selected, maxTextWidth);
            Fonts.BOLD.draw(displaySelected, boxX + 4, textY, 5, applyAlpha(new Color(160, 160, 165, 200)).getRGB());
        }

        Scissor.disable();
    }

    private String truncateText(String text, float maxWidth) {
        if (Fonts.BOLD.getWidth(text, 5) <= maxWidth) {
            return text;
        }
        String truncated = text;
        while (Fonts.BOLD.getWidth(truncated + "..", 5) > maxWidth && truncated.length() > 1) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + "..";
    }

    private void renderScrollingDescription(String description, float deltaTime) {
        float descY = y + height / 2 + 0.5f;
        float boxX = x + width - BOX_WIDTH - 2;
        float availableWidth = boxX - x - DESC_PADDING;
        float descWidth = Fonts.BOLD.getWidth(description, 5);

        if (descWidth <= availableWidth) {
            descScrollOffset = 0;
            Fonts.BOLD.draw(description, x + 0.5f, descY, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
        } else {
            updateDescScrollAnimation(deltaTime, descWidth, availableWidth);

            float maxScroll = descWidth - availableWidth + 5;
            float currentScroll = descScrollOffset * maxScroll;

            Scissor.enable(x, descY - 2, availableWidth, 10,2);
            Fonts.BOLD.draw(description, x + 0.5f - currentScroll, descY, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
            Scissor.disable();
        }
    }

    private void updateDescScrollAnimation(float deltaTime, float textWidth, float availableWidth) {
        long currentTime = System.currentTimeMillis();

        if (descScrollPauseTime > 0) {
            if (currentTime - descScrollPauseTime < SCROLL_PAUSE_DURATION) {
                return;
            }
            descScrollPauseTime = 0;
        }

        float scrollDistance = textWidth - availableWidth + 5;
        if (scrollDistance <= 0) {
            descScrollOffset = 0;
            return;
        }

        float scrollSpeed = SCROLL_PIXELS_PER_SECOND / scrollDistance;

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

    private void renderExpandedOptions(DrawContext context, int mouseX, int mouseY, float boxX, float startY, float deltaTime) {
        List<String> options = selectSetting.getList();

        float fullPanelHeight = options.size() * OPTION_HEIGHT;
        float visibleHeight = fullPanelHeight * expandAnimation;

        float panelAlpha = expandAnimation * alphaMultiplier;

        int panelBgAlpha = (int)(200 * panelAlpha);
        Render2D.rect(boxX, startY, BOX_WIDTH, visibleHeight, new Color(30, 30, 30, panelBgAlpha).getRGB(), 3f);

        int panelOutlineAlpha = (int)(100 * panelAlpha);
        Render2D.outline(boxX, startY, BOX_WIDTH, visibleHeight, 0.5f, new Color(80, 80, 85, panelOutlineAlpha).getRGB(), 3f);

        if (visibleHeight < 1f) return;

        Scissor.enable(boxX, startY, BOX_WIDTH, visibleHeight,2);

        float optionY = startY;

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);

            boolean optionHovered = mouseX >= boxX && mouseX <= boxX + BOX_WIDTH &&
                    mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT &&
                    expandAnimation > 0.8f;

            float hoverAnim = optionHoverAnimations.getOrDefault(option, 0f);
            hoverAnim = lerp(hoverAnim, optionHovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
            optionHoverAnimations.put(option, hoverAnim);

            boolean isSelected = selectSetting.isSelected(option);
            float selectAnim = selectAnimations.getOrDefault(option, 0f);
            selectAnim = lerp(selectAnim, isSelected ? 1f : 0f, deltaTime * 10f);
            selectAnimations.put(option, selectAnim);

            if (hoverAnim > 0.01f) {
                int hoverBgAlpha = (int)(30 * hoverAnim * panelAlpha);
                Render2D.rect(boxX + 2, optionY + 1, BOX_WIDTH - 4, OPTION_HEIGHT - 2,
                        new Color(100, 100, 105, hoverBgAlpha).getRGB(), 2f);
            }

            float checkSize = 6f;
            float checkX = boxX + 5;
            float checkY = optionY + OPTION_HEIGHT / 2 - checkSize / 2;

            int checkBgAlpha = (int)((40 + hoverAnim * 20) * panelAlpha);
            Render2D.rect(checkX, checkY, checkSize, checkSize, new Color(55, 55, 60, checkBgAlpha).getRGB(), 2f);

            int checkOutlineAlpha = (int)((80 + hoverAnim * 40) * panelAlpha);
            Render2D.outline(checkX, checkY, checkSize, checkSize, 0.5f, new Color(120, 120, 125, checkOutlineAlpha).getRGB(), 2f);

            if (selectAnim > 0.01f) {
                float innerSize = (checkSize - 2) * selectAnim;
                float innerX = checkX + (checkSize - innerSize) / 2;
                float innerY = checkY + (checkSize - innerSize) / 2;

                int innerAlpha = (int)(220 * selectAnim * panelAlpha);
                Render2D.rect(innerX, innerY, innerSize, innerSize, new Color(140, 180, 160, innerAlpha).getRGB(), 1.5f);
            }

            float textX = checkX + checkSize + 4;
            float textY = optionY + OPTION_HEIGHT / 2 - 2.5f;

            float availableTextWidth = BOX_WIDTH - checkSize - 14;
            String displayOption = option;
            float optionTextWidth = Fonts.BOLD.getWidth(option, 5);

            if (optionTextWidth > availableTextWidth) {
                while (Fonts.BOLD.getWidth(displayOption + "..", 5) > availableTextWidth && displayOption.length() > 1) {
                    displayOption = displayOption.substring(0, displayOption.length() - 1);
                }
                displayOption += "..";
            }

            int textGray = (int)(140 + selectAnim * 40 + hoverAnim * 20);
            int textAlpha = (int)(200 * panelAlpha);
            Fonts.BOLD.draw(displayOption, textX, textY, 5, new Color(textGray, textGray, textGray + 5, textAlpha).getRGB());

            optionY += OPTION_HEIGHT;
        }

        Scissor.disable();
    }

    private boolean isMainHover(double mouseX, double mouseY) {
        float boxX = x + width - BOX_WIDTH - 2;
        float boxY = y + height / 2 - 5;
        float boxHeight = 10f;
        return mouseX >= boxX && mouseX <= boxX + BOX_WIDTH && mouseY >= boxY && mouseY <= boxY + boxHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMainHover(mouseX, mouseY)) {
                expanded = !expanded;
                return true;
            }

            if (expanded && expandAnimation > 0.8f) {
                float boxX = x + width - BOX_WIDTH - 2;
                float boxY = y + height / 2 - 5;
                float startY = boxY + 10f + 2;

                float optionY = startY;
                for (String option : selectSetting.getList()) {
                    if (mouseX >= boxX && mouseX <= boxX + BOX_WIDTH &&
                            mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT) {
                        selectSetting.setSelected(option);
                        expanded = false;
                        return true;
                    }
                    optionY += OPTION_HEIGHT;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getTotalHeight() {
        float baseHeight = height;
        float expandedHeight = selectSetting.getList().size() * OPTION_HEIGHT * expandAnimation;
        return baseHeight + expandedHeight;
    }
}