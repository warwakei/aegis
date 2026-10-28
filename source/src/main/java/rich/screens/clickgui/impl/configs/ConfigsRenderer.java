package rich.screens.clickgui.impl.configs;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.category.ModuleCategory;
import rich.screens.clickgui.impl.configs.handler.ConfigAnimationHandler;
import rich.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import rich.screens.clickgui.impl.configs.render.ConfigCreateBoxRenderer;
import rich.screens.clickgui.impl.configs.render.ConfigHeaderRenderer;
import rich.screens.clickgui.impl.configs.render.ConfigListRenderer;
import rich.screens.clickgui.impl.configs.render.ConfigNotificationRenderer;
import rich.util.render.Render2D;

import java.awt.*;

public class ConfigsRenderer {

    public static final float PANEL_X_OFFSET = 92f;
    public static final float PANEL_Y_OFFSET = 38f;
    public static final float PANEL_WIDTH = 298f;
    public static final float PANEL_HEIGHT = 204f;
    public static final float CORNER_RADIUS = 6f;

    private final ConfigAnimationHandler animationHandler;
    private final ConfigDataHandler dataHandler;
    private final ConfigHeaderRenderer headerRenderer;
    private final ConfigListRenderer listRenderer;
    private final ConfigCreateBoxRenderer createBoxRenderer;
    private final ConfigNotificationRenderer notificationRenderer;

    private boolean isActive = false;
    private boolean wasActive = false;

    public ConfigsRenderer() {
        this.animationHandler = new ConfigAnimationHandler();
        this.dataHandler = new ConfigDataHandler(animationHandler);
        this.notificationRenderer = new ConfigNotificationRenderer();
        this.headerRenderer = new ConfigHeaderRenderer(dataHandler);
        this.listRenderer = new ConfigListRenderer(animationHandler, dataHandler, notificationRenderer);
        this.createBoxRenderer = new ConfigCreateBoxRenderer(dataHandler, notificationRenderer);
    }

    public void render(DrawContext context, float bgX, float bgY, float mouseX, float mouseY,
                       float delta, int guiScale, float alphaMultiplier, ModuleCategory category) {

//        boolean shouldBeActive = category == ModuleCategory.CONFIGS;
//
//        if (shouldBeActive && !wasActive) {
//            isActive = true;
//            animationHandler.reset();
//            dataHandler.refreshConfigs();
//            animationHandler.initItemAnimations(dataHandler.getConfigs());
//        } else if (!shouldBeActive && wasActive) {
//            isActive = false;
//        }
//
//        wasActive = shouldBeActive;
//
//        animationHandler.update(isActive, dataHandler.getConfigs(), dataHandler.isCreating());

        if (animationHandler.isFullyHidden() && !isActive) {
            return;
        }

        if (isActive) {
            dataHandler.refreshConfigs();
            animationHandler.initItemAnimations(dataHandler.getConfigs());
        }

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        float slideOffset = (1f - animationHandler.getPanelSlide()) * 20f;
        float finalAlpha = alphaMultiplier * animationHandler.getPanelAlpha();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(slideOffset, 0);

        renderPanel(panelX, panelY, finalAlpha);
        headerRenderer.render(panelX, panelY, mouseX - slideOffset, mouseY, finalAlpha);
        listRenderer.render(context, panelX, panelY, mouseX - slideOffset, mouseY, guiScale, finalAlpha);
        createBoxRenderer.render(panelX, panelY, finalAlpha);
        notificationRenderer.render(panelX, panelY, finalAlpha);

        context.getMatrices().popMatrix();
    }

    private void renderPanel(float x, float y, float alpha) {
        int panelAlpha = (int) (15 * alpha);
        int outlineAlpha = (int) (215 * alpha);

        Render2D.rect(x, y, PANEL_WIDTH, PANEL_HEIGHT, new Color(64, 64, 64, panelAlpha).getRGB(), CORNER_RADIUS);
        Render2D.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), CORNER_RADIUS);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float bgX, float bgY, ModuleCategory category) {
//        if (category != ModuleCategory.CONFIGS) return false;
        if (animationHandler.getPanelAlpha() < 0.5f) return false;

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        float slideOffset = (1f - animationHandler.getPanelSlide()) * 20f;
        mouseX -= slideOffset;

        if (headerRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        if (createBoxRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        if (listRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical, float bgX, float bgY, ModuleCategory category) {
//        if (category != ModuleCategory.CONFIGS) return false;
        if (animationHandler.getPanelAlpha() < 0.5f) return false;

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        return listRenderer.mouseScrolled(mouseX, mouseY, vertical, panelX, panelY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return createBoxRenderer.keyPressed(keyCode);
    }

    public boolean charTyped(char chr, int modifiers) {
        return createBoxRenderer.charTyped(chr);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean isEditing() {
        return dataHandler.isCreating();
    }
}