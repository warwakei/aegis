package rich.screens.clickgui.impl.configs.render;

import rich.screens.clickgui.impl.configs.ConfigsRenderer;
import rich.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class ConfigCreateBoxRenderer {

    private final ConfigDataHandler dataHandler;
    private final ConfigNotificationRenderer notificationRenderer;

    private float createBoxAnimation = 0f;
    private float cursorBlink = 0f;
    private long lastUpdateTime = System.currentTimeMillis();

    public ConfigCreateBoxRenderer(ConfigDataHandler dataHandler, 
                                    ConfigNotificationRenderer notificationRenderer) {
        this.dataHandler = dataHandler;
        this.notificationRenderer = notificationRenderer;
    }

    public void render(float x, float y, float alpha) {
        updateAnimations();

        if (createBoxAnimation < 0.01f) return;

        float boxY = y + ConfigsRenderer.PANEL_HEIGHT - 40;
        float boxAlpha = createBoxAnimation * alpha;

        renderBackground(x, boxY, boxAlpha);
        renderInput(x, boxY, boxAlpha);
        renderSaveButton(x, boxY, boxAlpha);
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        float targetCreate = dataHandler.isCreating() ? 1f : 0f;
        createBoxAnimation += (targetCreate - createBoxAnimation) * 14f * deltaTime;

        cursorBlink += deltaTime * 2f;
        if (cursorBlink > 1f) cursorBlink -= 1f;
    }

    private void renderBackground(float x, float boxY, float alpha) {
        Render2D.rect(x + 8, boxY, ConfigsRenderer.PANEL_WIDTH - 16, 32, 
                new Color(50, 50, 55, (int) (30 * alpha)).getRGB(), 5);
        Render2D.outline(x + 8, boxY, ConfigsRenderer.PANEL_WIDTH - 16, 32, 0.5f, 
                new Color(80, 80, 85, (int) (100 * alpha)).getRGB(), 5);
    }

    private void renderInput(float x, float boxY, float alpha) {
        float inputX = x + 15;
        float inputY = boxY + 8;
        float inputW = ConfigsRenderer.PANEL_WIDTH - 100;
        float inputH = 16;

        Render2D.rect(inputX, inputY, inputW, inputH, 
                new Color(40, 40, 45, (int) (40 * alpha)).getRGB(), 4);
        Render2D.outline(inputX, inputY, inputW, inputH, 0.5f, 
                new Color(70, 70, 75, (int) (80 * alpha)).getRGB(), 4);

        String configName = dataHandler.getNewConfigName();
        if (configName.isEmpty()) {
            Fonts.BOLD.draw("Enter config name...", inputX + 5, inputY + 5, 5,
                    new Color(100, 100, 105, (int) (150 * alpha)).getRGB());
        } else {
            Fonts.BOLD.draw(configName, inputX + 5, inputY + 5, 5,
                    new Color(210, 210, 220, (int) (255 * alpha)).getRGB());
        }

        if (dataHandler.isCreating()) {
            renderCursor(inputX, inputY, inputH, configName, alpha);
        }
    }

    private void renderCursor(float inputX, float inputY, float inputH, String text, float alpha) {
        float cursorAlpha = (float) (Math.sin(cursorBlink * Math.PI * 2) * 0.5 + 0.5);
        if (cursorAlpha > 0.3f) {
            float cursorX = inputX + 5 + Fonts.BOLD.getWidth(text, 5);
            Render2D.rect(cursorX, inputY + 3, 0.5f, inputH - 6,
                    new Color(180, 180, 185, (int) (255 * cursorAlpha * alpha)).getRGB(), 0);
        }
    }

    private void renderSaveButton(float x, float boxY, float alpha) {
        float saveX = x + ConfigsRenderer.PANEL_WIDTH - 75;
        float saveY = boxY + 6;
        float saveW = 60;
        float saveH = 20;

        Render2D.rect(saveX, saveY, saveW, saveH, 
                new Color(80, 140, 80, (int) (40 * alpha)).getRGB(), 4);
        Render2D.outline(saveX, saveY, saveW, saveH, 0.5f, 
                new Color(100, 180, 100, (int) (80 * alpha)).getRGB(), 4);

        float textWidth = Fonts.BOLD.getWidth("Save", 5);
        Fonts.BOLD.draw("Save", saveX + (saveW - textWidth) / 2, saveY + 7, 5,
                new Color(180, 220, 180, (int) (255 * alpha)).getRGB());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        if (!dataHandler.isCreating() || createBoxAnimation < 0.5f) return false;

        float saveX = panelX + ConfigsRenderer.PANEL_WIDTH - 75;
        float saveY = panelY + ConfigsRenderer.PANEL_HEIGHT - 34;

        if (mouseX >= saveX && mouseX <= saveX + 60 &&
                mouseY >= saveY && mouseY <= saveY + 20 && button == 0) {
            saveConfig();
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!dataHandler.isCreating()) return false;

        if (keyCode == 259) {
            dataHandler.removeLastChar();
            return true;
        }

        if (keyCode == 257) {
            saveConfig();
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr) {
        if (!dataHandler.isCreating()) return false;

        dataHandler.appendChar(chr);
        return true;
    }

    private void saveConfig() {
        String name = dataHandler.getNewConfigName();

        if (name.isEmpty()) {
            notificationRenderer.show("Enter a config name", 
                    ConfigNotificationRenderer.NotificationType.ERROR);
            return;
        }

        if (name.equalsIgnoreCase("autoconfig")) {
            notificationRenderer.show("This name is reserved", 
                    ConfigNotificationRenderer.NotificationType.ERROR);
            return;
        }

        if (dataHandler.saveConfig(name)) {
            notificationRenderer.show("Config saved: " + name, 
                    ConfigNotificationRenderer.NotificationType.SUCCESS);
            dataHandler.clearNewConfigName();
            dataHandler.setCreating(false);
        } else {
            notificationRenderer.show("Config already exists", 
                    ConfigNotificationRenderer.NotificationType.ERROR);
        }
    }
}