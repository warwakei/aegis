package rich.screens.clickgui.impl.background.search;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.ModuleStructure;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.List;

public class SearchRenderer {

    private final SearchHandler searchHandler;

    public SearchRenderer(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    public void render(DrawContext context, float bgX, float bgY, float bgWidth, float bgHeight,
                       float mouseX, float mouseY, int guiScale, float alphaMultiplier) {

        if (searchHandler.getSearchPanelAlpha() <= 0.01f) return;

        float panelX = bgX + 92f;
        float panelY = bgY + 38f;
        float panelW = bgWidth - 100f;
        float panelH = bgHeight - 46f;

        float resultAlpha = searchHandler.getSearchPanelAlpha() * alphaMultiplier;

        renderPanelBackground(panelX, panelY, panelW, panelH, resultAlpha);

        List<ModuleStructure> results = searchHandler.getSearchResults();
        if (results.isEmpty()) {
            renderEmptyState(panelX, panelY, panelW, panelH, resultAlpha);
            return;
        }

        Scissor.enable(panelX + 3, panelY + 3, panelW - 6, panelH - 6, 2);
        renderResults(panelX, panelY, panelW, panelH, mouseX, mouseY, resultAlpha);
        Scissor.disable();

        renderScrollIndicators(panelX, panelY, panelW, panelH, resultAlpha);
    }

    private void renderPanelBackground(float panelX, float panelY, float panelW, float panelH, float resultAlpha) {
        int panelBgAlpha = (int) (15 * resultAlpha);
        int outlineAlpha = (int) (215 * resultAlpha);

        Render2D.rect(panelX, panelY, panelW, panelH, new Color(64, 64, 64, panelBgAlpha).getRGB(), 7f);
        Render2D.outline(panelX, panelY, panelW, panelH, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 7f);
    }

    private void renderEmptyState(float panelX, float panelY, float panelW, float panelH, float resultAlpha) {
        String noResults = searchHandler.getSearchText().isEmpty() ? "Start typing to search..." : "No modules found";
        float textSize = 6f;
        float textWidth = Fonts.BOLD.getWidth(noResults, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = panelX + (panelW - textWidth) / 2f;
        float centerY = panelY + (panelH - textHeight) / 2f;
        Fonts.BOLD.draw(noResults, centerX, centerY, textSize, new Color(100, 100, 100, (int) (150 * resultAlpha)).getRGB());
    }

    private void renderResults(float panelX, float panelY, float panelW, float panelH,
                               float mouseX, float mouseY, float resultAlpha) {

        List<ModuleStructure> results = searchHandler.getSearchResults();
        float startY = panelY + 5 + searchHandler.getSearchScrollOffset();
        float resultHeight = searchHandler.getSearchResultHeight();

        int newHoveredIndex = -1;

        for (int i = 0; i < results.size(); i++) {
            ModuleStructure module = results.get(i);
            float itemY = startY + i * (resultHeight + 2);

            if (itemY + resultHeight < panelY || itemY > panelY + panelH) continue;

            float itemAnim = searchHandler.getSearchResultAnimations().getOrDefault(module, 0f);
            float itemAlpha = itemAnim * resultAlpha;

            if (itemAlpha <= 0.01f) continue;

            float itemOffsetX = (1f - itemAnim) * 20f;

            boolean hovered = mouseX >= panelX + 5 && mouseX <= panelX + panelW - 5 &&
                    mouseY >= itemY && mouseY <= itemY + resultHeight;

            if (hovered) {
                newHoveredIndex = i;
            }

            boolean selected = module == searchHandler.getSelectedSearchModule();

            renderResultItem(module, panelX, itemY, panelW, resultHeight,
                    itemOffsetX, itemAlpha, hovered, selected);
        }

        searchHandler.setHoveredSearchIndex(newHoveredIndex);
    }

    private void renderResultItem(ModuleStructure module, float panelX, float itemY, float panelW,
                                   float resultHeight, float itemOffsetX, float itemAlpha,
                                   boolean hovered, boolean selected) {

        Color bg;
        if (selected) {
            bg = new Color(140, 140, 140, (int) (60 * itemAlpha));
        } else if (hovered) {
            bg = new Color(100, 100, 100, (int) (40 * itemAlpha));
        } else {
            bg = new Color(64, 64, 64, (int) (25 * itemAlpha));
        }

        float itemX = panelX + 5 + itemOffsetX;
        float itemW = panelW - 10;

        Render2D.rect(itemX, itemY, itemW, resultHeight, bg.getRGB(), 5);

        if (selected) {
            Render2D.outline(itemX, itemY, itemW, resultHeight, 0.5f,
                    new Color(160, 160, 160, (int) (100 * itemAlpha)).getRGB(), 5);
        }

        Color textColor = module.isState()
                ? new Color(255, 255, 255, (int) (255 * itemAlpha))
                : new Color(180, 180, 180, (int) (200 * itemAlpha));

        Fonts.BOLD.draw(module.getName(), itemX + 5, itemY + 3, 6, textColor.getRGB());

        String categoryName = module.getCategory().getReadableName();
        Color categoryColor = new Color(140, 140, 140, (int) (180 * itemAlpha));
        Fonts.BOLD.draw(categoryName, itemX + 5, itemY + 11, 4, categoryColor.getRGB());

        if (module.isState()) {
            float indicatorX = itemX + itemW - 10;
            float indicatorY = itemY + resultHeight / 2 - 2;
            Render2D.rect(indicatorX, indicatorY, 4, 4,
                    new Color(100, 200, 100, (int) (200 * itemAlpha)).getRGB(), 2);
        }
    }

    private void renderScrollIndicators(float panelX, float panelY, float panelW, float panelH, float resultAlpha) {
        List<ModuleStructure> results = searchHandler.getSearchResults();
        float resultHeight = searchHandler.getSearchResultHeight();
        float maxScroll = Math.max(0, results.size() * (resultHeight + 2) - panelH + 10);

        if (maxScroll > 0) {
            if (searchHandler.getSearchScrollOffset() < -0.5f) {
                for (int i = 0; i < 10; i++) {
                    float fadeAlpha = 60 * resultAlpha * (1f - i / 10f);
                    Render2D.rect(panelX + 3, panelY + 3 + i, panelW - 6, 1,
                            new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
                }
            }
            if (searchHandler.getSearchScrollOffset() > -maxScroll + 0.5f) {
                for (int i = 0; i < 10; i++) {
                    float fadeAlpha = 60 * resultAlpha * (i / 10f);
                    Render2D.rect(panelX + 3, panelY + panelH - 13 + i, panelW - 6, 1,
                            new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
                }
            }
        }
    }

    public ModuleStructure getModuleAtPosition(double mouseX, double mouseY, float bgX, float bgY,
                                               float bgWidth, float bgHeight, SearchHandler handler) {

        if (!handler.isSearchActive() || handler.getSearchResults().isEmpty()) return null;

        float panelX = bgX + 92f;
        float panelY = bgY + 38f;
        float panelW = bgWidth - 100f;
        float panelH = bgHeight - 46f;

        if (mouseX < panelX + 5 || mouseX > panelX + panelW - 5 ||
                mouseY < panelY || mouseY > panelY + panelH) return null;

        float startY = panelY + 5 + handler.getSearchScrollOffset();
        float resultHeight = handler.getSearchResultHeight();

        List<ModuleStructure> results = handler.getSearchResults();
        for (int i = 0; i < results.size(); i++) {
            float itemY = startY + i * (resultHeight + 2);

            if (mouseY >= itemY && mouseY <= itemY + resultHeight) {
                return results.get(i);
            }
        }

        return null;
    }
}