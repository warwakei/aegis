package rich.screens.clickgui.impl.background.render;

import rich.modules.module.category.ModuleCategory;
import rich.screens.clickgui.impl.background.search.SearchHandler;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;

public class HeaderRenderer {

    private static final float HEADER_SLIDE_DISTANCE = 8f;

    public void render(float bgX, float bgY, float bgWidth, ModuleCategory selectedCategory,
                       ModuleCategory previousCategory, ModuleCategory currentCategory,
                       float headerTransition, SearchHandler searchHandler, float alphaMultiplier) {

        renderHeaderPanel(bgX, bgY, bgWidth, alphaMultiplier);
        renderSearchBox(bgX, bgY, searchHandler, alphaMultiplier);
        renderCategoryLabel(bgX, bgY, previousCategory, currentCategory, headerTransition, searchHandler, alphaMultiplier);
    }

    private void renderHeaderPanel(float bgX, float bgY, float bgWidth, float alphaMultiplier) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);

        Render2D.rect(bgX + 92f, bgY + 7.5f, bgWidth - 100f, 25, new Color(128, 128, 128, panelAlpha).getRGB(), 8);
        Render2D.outline(bgX + 92f, bgY + 7.5f, bgWidth - 100f, 25, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 8);
    }

    private void renderSearchBox(float bgX, float bgY, SearchHandler searchHandler, float alphaMultiplier) {
        float searchBoxX = bgX + 315f;
        float searchBoxY = bgY + 12.5f;
        float searchBoxW = 70f;
        float searchBoxH = 15f;

        int outlineAlpha = (int) (255 * alphaMultiplier);
        int panelAlpha = (int) (25 * alphaMultiplier);

        Color searchOutline = searchHandler.isSearchActive()
                ? new Color(180, 180, 180, outlineAlpha)
                : new Color(55, 55, 55, outlineAlpha);

        int searchBgAlpha = (int) ((25 + searchHandler.getSearchFocusAnimation() * 15) * alphaMultiplier);
        Render2D.rect(searchBoxX, searchBoxY, searchBoxW, searchBoxH, new Color(40, 40, 45, searchBgAlpha).getRGB(), 4);
        Render2D.outline(searchBoxX, searchBoxY, searchBoxW, searchBoxH, 0.5f, searchOutline.getRGB(), 4);

        float textAreaX = searchBoxX + 5;

        if (searchHandler.isSearchActive() && !searchHandler.getSearchText().isEmpty()) {
            renderSearchText(searchBoxX, searchBoxY, searchBoxW, searchBoxH, textAreaX, searchHandler, alphaMultiplier);
        } else if (searchHandler.isSearchActive()) {
            renderSearchPlaceholder(searchBoxX, searchBoxY, searchBoxH, textAreaX, searchHandler, alphaMultiplier, true);
        } else {
            Fonts.BOLD.draw("Search Modules...", textAreaX, searchBoxY + 5f, 5, new Color(128, 128, 128, outlineAlpha).getRGB());
        }

        Render2D.rect(searchBoxX + 53, searchBoxY + 3.5f, 1, searchBoxH - 7, new Color(128, 128, 128, panelAlpha).getRGB(), 8);
        Fonts.ICONS.draw("U", searchBoxX + 55, searchBoxY + 1.5f, 12, new Color(128, 128, 128, outlineAlpha).getRGB());
    }

    private void renderSearchText(float searchBoxX, float searchBoxY, float searchBoxW, float searchBoxH,
                                  float textAreaX, SearchHandler searchHandler, float alphaMultiplier) {
        Scissor.enable(searchBoxX + 3, searchBoxY, searchBoxW - 20, searchBoxH, 2);

        if (searchHandler.hasSearchSelection() && searchHandler.getSearchSelectionAnimation() > 0.01f) {
            renderSearchSelection(textAreaX, searchBoxY, searchBoxH, searchHandler, alphaMultiplier);
        }

        Fonts.BOLD.draw(searchHandler.getSearchText(), textAreaX, searchBoxY + 5f, 5,
                new Color(210, 210, 220, (int) (255 * alphaMultiplier)).getRGB());
        Scissor.disable();

        if (!searchHandler.hasSearchSelection()) {
            renderSearchCursor(textAreaX, searchBoxY, searchBoxH, searchHandler, alphaMultiplier);
        }
    }

    private void renderSearchSelection(float textAreaX, float searchBoxY, float searchBoxH,
                                       SearchHandler searchHandler, float alphaMultiplier) {
        int start = searchHandler.getSearchSelectionStart();
        int end = searchHandler.getSearchSelectionEnd();
        String beforeSelection = searchHandler.getSearchText().substring(0, start);
        String selection = searchHandler.getSearchText().substring(start, end);

        float selectionX = textAreaX + Fonts.BOLD.getWidth(beforeSelection, 5);
        float selectionWidth = Fonts.BOLD.getWidth(selection, 5);

        int selAlpha = (int) (100 * searchHandler.getSearchSelectionAnimation() * alphaMultiplier);
        Render2D.rect(selectionX, searchBoxY + 2, selectionWidth, searchBoxH - 4,
                new Color(100, 140, 180, selAlpha).getRGB(), 2f);
    }

    private void renderSearchCursor(float textAreaX, float searchBoxY, float searchBoxH,
                                    SearchHandler searchHandler, float alphaMultiplier) {
        float cursorAlpha = (float) (Math.sin(searchHandler.getSearchCursorBlink() * Math.PI * 2) * 0.5 + 0.5);
        if (cursorAlpha > 0.3f) {
            String beforeCursor = searchHandler.getSearchText().substring(0, searchHandler.getSearchCursorPosition());
            float cursorX = textAreaX + Fonts.BOLD.getWidth(beforeCursor, 5);
            int cursorAlphaInt = (int) (255 * cursorAlpha * alphaMultiplier);
            Render2D.rect(cursorX, searchBoxY + 3, 0.5f, searchBoxH - 6, new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0);
        }
    }

    private void renderSearchPlaceholder(float searchBoxX, float searchBoxY, float searchBoxH,
                                         float textAreaX, SearchHandler searchHandler, float alphaMultiplier, boolean showCursor) {
        Fonts.BOLD.draw("Type to search...", textAreaX, searchBoxY + 5f, 5,
                new Color(100, 100, 105, (int) (150 * alphaMultiplier)).getRGB());

        if (showCursor) {
            float cursorAlpha = (float) (Math.sin(searchHandler.getSearchCursorBlink() * Math.PI * 2) * 0.5 + 0.5);
            if (cursorAlpha > 0.3f) {
                int cursorAlphaInt = (int) (255 * cursorAlpha * alphaMultiplier);
                Render2D.rect(textAreaX, searchBoxY + 3, 0.5f, searchBoxH - 6,
                        new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0);
            }
        }
    }

    private void renderCategoryLabel(float bgX, float bgY, ModuleCategory previousCategory,
                                     ModuleCategory currentCategory, float headerTransition,
                                     SearchHandler searchHandler, float alphaMultiplier) {
        float baseX = bgX + 100f;
        float baseY = bgY + 16f;

        float categoryAlpha = searchHandler.getNormalPanelAlpha() * alphaMultiplier;
        if (categoryAlpha > 0.01f) {
            float eased = easeOutQuart(headerTransition);

            if (previousCategory != null && headerTransition < 1f) {
                float oldAlpha = (1f - eased) * categoryAlpha;
                float oldOffsetY = eased * HEADER_SLIDE_DISTANCE;

                int oldAlphaInt = (int) (128 * oldAlpha);
                if (oldAlphaInt > 0) {
                    String oldName = previousCategory.getReadableName();
                    Fonts.BOLD.draw(oldName, baseX, baseY + oldOffsetY, 7, new Color(128, 128, 128, oldAlphaInt).getRGB());
                }
            }

            if (currentCategory != null) {
                float newAlpha = eased * categoryAlpha;
                float newOffsetY = (1f - eased) * -HEADER_SLIDE_DISTANCE;

                int newAlphaInt = (int) (128 * newAlpha);
                if (newAlphaInt > 0) {
                    String newName = currentCategory.getReadableName();
                    Fonts.BOLD.draw(newName, baseX, baseY + newOffsetY, 7, new Color(128, 128, 128, newAlphaInt).getRGB());
                }
            }
        }

        float searchLabelAlpha = searchHandler.getSearchPanelAlpha() * alphaMultiplier;
        if (searchLabelAlpha > 0.01f) {
            int searchLabelAlphaInt = (int) (180 * searchLabelAlpha);
            if (searchLabelAlphaInt > 0) {
                String searchLabel = "Search Results";
                String searchText = searchHandler.getSearchText();
                if (!searchText.isEmpty()) {
                    searchLabel = "Results for \"" + (searchText.length() > 12 ? searchText.substring(0, 12) + "..." : searchText) + "\"";
                }
                Fonts.BOLD.draw(searchLabel, baseX, baseY, 7, new Color(160, 160, 160, searchLabelAlphaInt).getRGB());
            }
        }
    }

    private float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1 - x, 4);
    }

    public boolean isSearchBoxHovered(double mouseX, double mouseY, float bgX, float bgY) {
        float searchBoxX = bgX + 315f;
        float searchBoxY = bgY + 12.5f;
        float searchBoxW = 70f;
        float searchBoxH = 15f;

        return mouseX >= searchBoxX && mouseX <= searchBoxX + searchBoxW &&
                mouseY >= searchBoxY && mouseY <= searchBoxY + searchBoxH;
    }
}