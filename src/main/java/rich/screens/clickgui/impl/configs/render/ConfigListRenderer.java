package rich.screens.clickgui.impl.configs.render;

import net.minecraft.client.gui.DrawContext;
import rich.screens.clickgui.impl.configs.ConfigsRenderer;
import rich.screens.clickgui.impl.configs.handler.ConfigAnimationHandler;
import rich.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.Map;

public class ConfigListRenderer {

    private static final float CONFIG_ITEM_HEIGHT = 24f;
    private static final float CONFIG_ITEM_SPACING = 3f;
    private static final float HOVER_SPEED = 0.15f;

    private final ConfigAnimationHandler animationHandler;
    private final ConfigDataHandler dataHandler;
    private final ConfigNotificationRenderer notificationRenderer;

    public ConfigListRenderer(ConfigAnimationHandler animationHandler, ConfigDataHandler dataHandler,
                              ConfigNotificationRenderer notificationRenderer) {
        this.animationHandler = animationHandler;
        this.dataHandler = dataHandler;
        this.notificationRenderer = notificationRenderer;
    }

    public void render(DrawContext context, float x, float y, float mouseX, float mouseY,
                       int guiScale, float alpha) {
        float listX = x + 8;
        float listY = y + 37;
        float listW = ConfigsRenderer.PANEL_WIDTH - 16;
        float listH = ConfigsRenderer.PANEL_HEIGHT - 45;

        if (dataHandler.isCreating()) {
            listH -= 40 * animationHandler.getCreateBoxAnimation();
        }

        dataHandler.updateScroll(0.016f);
        dataHandler.updateScrollFades(listH);

        Scissor.enable(listX, listY - 8, listW, listH + 15, 2);

        float itemY = listY + (float) dataHandler.getScrollOffset();

        for (String config : dataHandler.getConfigs()) {
            float itemAlpha = animationHandler.getItemAppearAnimation(config);

            if (itemAlpha < 0.01f) {
                itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
                continue;
            }

            if (itemY + CONFIG_ITEM_HEIGHT >= listY && itemY <= listY + listH) {
                float itemSlide = (1f - itemAlpha) * 15f;
                renderConfigItem(config, listX + itemSlide, itemY, listW, mouseX, mouseY, alpha * itemAlpha);
            }
            itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
        }

        if (dataHandler.getConfigs().isEmpty()) {
            renderEmptyMessage(x, y, alpha);
        }

        Scissor.disable();
    }

    private void renderConfigItem(String config, float x, float y, float width,
                                  float mouseX, float mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + CONFIG_ITEM_HEIGHT;
        boolean isSelected = config.equals(dataHandler.getSelectedConfig());

        float hoverAnim = animationHandler.getHoverAnimation(config);
        float target = isHovered ? 1f : 0f;
        hoverAnim += (target - hoverAnim) * HOVER_SPEED;
        animationHandler.setHoverAnimation(config, hoverAnim);

        renderItemBackground(x, y, width, isSelected, hoverAnim, alpha);
        renderItemName(config, x, y - 0.5f, alpha);
        renderActionButtons(config, x, y - 0.5f, width, mouseX, mouseY, alpha);
    }

    private void renderItemBackground(float x, float y, float width, boolean isSelected,
                                      float hoverAnim, float alpha) {
        int bgAlpha = (int) ((20 + 15 * hoverAnim + (isSelected ? 10 : 0)) * alpha);
        int gray = (int) (60 + 20 * hoverAnim);
        Render2D.rect(x, y, width, CONFIG_ITEM_HEIGHT, new Color(gray, gray, gray, bgAlpha).getRGB(), 5);

        if (isSelected || hoverAnim > 0.01f) {
            int outlineAlpha = (int) ((40 + 40 * hoverAnim) * alpha);
            Render2D.outline(x, y, width, CONFIG_ITEM_HEIGHT, 0.5f,
                    new Color(100, 100, 100, outlineAlpha).getRGB(), 5);
        }
    }

    private void renderItemName(String config, float x, float y, float alpha) {
        Fonts.GUI_ICONS.draw("B", x + 4, y + 4.5f, 16, new Color(220, 220, 220, (int) (25 * alpha)).getRGB());
        Fonts.BOLD.draw(config, x + 10, y + 8, 6, new Color(220, 220, 220, (int) (255 * alpha)).getRGB());
    }

    private void renderActionButtons(String config, float x, float y, float width,
                                     float mouseX, float mouseY, float alpha) {
        float buttonSize = 18f;
        float buttonY = y + (CONFIG_ITEM_HEIGHT - buttonSize) / 2 + 1;
        float deleteButtonX = x + width - buttonSize - 8;
        float refreshButtonX = deleteButtonX - buttonSize - 5;
        float loadButtonX = refreshButtonX - buttonSize - 5;

        renderActionButton(loadButtonX, buttonY, buttonSize, "P", 15f, 4f, 2f,
                mouseX, mouseY, animationHandler.getLoadHoverAnimations(), config,
                new Color(80, 180, 80), alpha);

        renderActionButton(refreshButtonX, buttonY, buttonSize, "N", 10f, 5f, 4f,
                mouseX, mouseY, animationHandler.getRefreshHoverAnimations(), config,
                new Color(80, 140, 200), alpha);

        renderActionButton(deleteButtonX, buttonY, buttonSize, "O", 13f, 4.5f, 2.5f,
                mouseX, mouseY, animationHandler.getDeleteHoverAnimations(), config,
                new Color(180, 80, 80), alpha);
    }

    private void renderActionButton(float x, float y, float size, String icon,
                                    float iconSize, float iconOffsetX, float iconOffsetY,
                                    float mouseX, float mouseY,
                                    Map<String, Float> animations, String config,
                                    Color hoverColor, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;

        float anim = animations.getOrDefault(config, 0f);
        float target = hovered ? 1f : 0f;
        anim += (target - anim) * HOVER_SPEED;
        animations.put(config, anim);

        int bgAlpha = (int) ((25 + 20 * anim) * alpha);
        int r = (int) (60 + (hoverColor.getRed() - 60) * anim);
        int g = (int) (60 + (hoverColor.getGreen() - 60) * anim);
        int b = (int) (60 + (hoverColor.getBlue() - 60) * anim);

        Render2D.rect(x, y, size, size, new Color(r, g, b, bgAlpha).getRGB(), 4);

        int iconAlpha = (int) ((150 + 105 * anim) * alpha);
        Fonts.GUI_ICONS.draw(icon, x + iconOffsetX, y + iconOffsetY, iconSize,
                new Color(200, 200, 200, iconAlpha).getRGB());
    }

    private void renderEmptyMessage(float x, float y, float alpha) {
        String text = "No configs found";
        float textWidth = Fonts.BOLD.getWidth(text, 6);
        Fonts.BOLD.draw(text, x + (ConfigsRenderer.PANEL_WIDTH - textWidth) / 2,
                y + ConfigsRenderer.PANEL_HEIGHT / 2, 6,
                new Color(100, 100, 100, (int) (150 * alpha)).getRGB());
    }

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade) {
        int size = 15;
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = 80 * topFade * (1f - i / (float) size);
                Render2D.rect(x, y + i, w, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = 80 * bottomFade * (i / (float) size);
                Render2D.rect(x, y + h - size + i, w, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        float listX = panelX + 8;
        float listY = panelY + 37;
        float listW = ConfigsRenderer.PANEL_WIDTH - 16;
        float listH = ConfigsRenderer.PANEL_HEIGHT - 45;

        if (dataHandler.isCreating()) {
            listH -= 40 * animationHandler.getCreateBoxAnimation();
        }

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            float itemY = listY + (float) dataHandler.getScrollOffset();

            for (String config : dataHandler.getConfigs()) {
                float itemAlpha = animationHandler.getItemAppearAnimation(config);
                if (itemAlpha < 0.5f) {
                    itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
                    continue;
                }

                if (mouseY >= itemY && mouseY <= itemY + CONFIG_ITEM_HEIGHT) {
                    return handleItemClick(config, mouseX, mouseY, button, listX, listW, itemY);
                }
                itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
            }
        }

        return false;
    }

    private boolean handleItemClick(String config, double mouseX, double mouseY, int button,
                                    float listX, float listW, float itemY) {
        float buttonSize = 18f;
        float buttonYPos = itemY + (CONFIG_ITEM_HEIGHT - buttonSize) / 2 + 1;
        float deleteButtonX = listX + listW - buttonSize - 8;
        float refreshButtonX = deleteButtonX - buttonSize - 5;
        float loadButtonX = refreshButtonX - buttonSize - 5;

        if (mouseX >= loadButtonX && mouseX <= loadButtonX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize && button == 0) {
            if (dataHandler.loadConfig(config)) {
                notificationRenderer.show("Config loaded: " + config,
                        ConfigNotificationRenderer.NotificationType.SUCCESS);
            } else {
                notificationRenderer.show("Config not found",
                        ConfigNotificationRenderer.NotificationType.ERROR);
            }
            return true;
        }

        if (mouseX >= refreshButtonX && mouseX <= refreshButtonX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize && button == 0) {
            if (dataHandler.refreshConfig(config)) {
                notificationRenderer.show("Конфиг успешно обновлён",
                        ConfigNotificationRenderer.NotificationType.INFO);
            } else {
                notificationRenderer.show("Error refreshing config",
                        ConfigNotificationRenderer.NotificationType.ERROR);
            }
            return true;
        }

        if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize && button == 0) {
            if (dataHandler.deleteConfig(config)) {
                notificationRenderer.show("Config deleted: " + config,
                        ConfigNotificationRenderer.NotificationType.SUCCESS);
            } else {
                notificationRenderer.show("Error deleting config",
                        ConfigNotificationRenderer.NotificationType.ERROR);
            }
            return true;
        }

        if (button == 0) {
            String current = dataHandler.getSelectedConfig();
            dataHandler.setSelectedConfig(config.equals(current) ? null : config);
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical,
                                 float panelX, float panelY) {
        if (mouseX >= panelX && mouseX <= panelX + ConfigsRenderer.PANEL_WIDTH &&
                mouseY >= panelY && mouseY <= panelY + ConfigsRenderer.PANEL_HEIGHT) {

            float visibleHeight = ConfigsRenderer.PANEL_HEIGHT - 45;
            if (dataHandler.isCreating()) {
                visibleHeight -= 40;
            }

            dataHandler.handleScroll(vertical, visibleHeight);
            return true;
        }

        return false;
    }
}