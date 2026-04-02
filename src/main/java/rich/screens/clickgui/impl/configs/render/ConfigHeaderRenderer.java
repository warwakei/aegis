package rich.screens.clickgui.impl.configs.render;

import rich.screens.clickgui.impl.configs.ConfigsRenderer;
import rich.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class ConfigHeaderRenderer {

    private final ConfigDataHandler dataHandler;

    public ConfigHeaderRenderer(ConfigDataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public void render(float x, float y, float mouseX, float mouseY, float alpha) {
        Fonts.BOLD.draw("Configurations", x + 10, y + 10, 7, new Color(255, 255, 255, (int) (200 * alpha)).getRGB());

        renderCreateButton(x, y, mouseX, mouseY, alpha);
        renderSeparator(x, y, alpha);
    }

    private void renderCreateButton(float x, float y, float mouseX, float mouseY, float alpha) {
        float buttonX = x + ConfigsRenderer.PANEL_WIDTH - 70;
        float buttonY = y + 8;
        float buttonW = 60;
        float buttonH = 16;

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonW &&
                mouseY >= buttonY && mouseY <= buttonY + buttonH;

        int bgAlpha = (int) ((hovered ? 40 : 25) * alpha);
        int outlineAlpha = (int) ((hovered ? 100 : 60) * alpha);

        Render2D.rect(buttonX, buttonY, buttonW, buttonH, new Color(64, 64, 64, bgAlpha).getRGB(), 4);
        Render2D.outline(buttonX, buttonY, buttonW, buttonH, 0.5f, new Color(100, 100, 100, outlineAlpha).getRGB(), 4);

        String text = dataHandler.isCreating() ? "Cancel" : "+ Create";
        float textWidth = Fonts.BOLD.getWidth(text, 5);
        Fonts.BOLD.draw(text, buttonX + (buttonW - textWidth) / 2, buttonY + 5.5f, 5,
                new Color(180, 180, 180, (int) (255 * alpha)).getRGB());
    }

    private void renderSeparator(float x, float y, float alpha) {
        Render2D.rect(x + 10, y + 28, ConfigsRenderer.PANEL_WIDTH - 20, 0.5f,
                new Color(64, 64, 64, (int) (100 * alpha)).getRGB(), 0);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        float buttonX = panelX + ConfigsRenderer.PANEL_WIDTH - 70;
        float buttonY = panelY + 8;

        if (mouseX >= buttonX && mouseX <= buttonX + 60 &&
                mouseY >= buttonY && mouseY <= buttonY + 16 && button == 0) {
            dataHandler.toggleCreating();
            return true;
        }

        return false;
    }
}