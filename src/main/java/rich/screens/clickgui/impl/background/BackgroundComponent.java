package rich.screens.clickgui.impl.background;

import net.minecraft.client.gui.DrawContext;
import rich.IMinecraft;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.screens.clickgui.impl.background.render.AvatarRenderer;
import rich.screens.clickgui.impl.background.render.BackgroundRenderer;
import rich.screens.clickgui.impl.background.render.CategoryRenderer;
import rich.screens.clickgui.impl.background.render.HeaderRenderer;
import rich.screens.clickgui.impl.background.search.SearchHandler;
import rich.screens.clickgui.impl.background.search.SearchRenderer;

import java.util.List;

public class BackgroundComponent implements IMinecraft {
    public static final int BG_WIDTH = 400;
    public static final int BG_HEIGHT = 250;

    private final BackgroundRenderer backgroundRenderer;
    private final CategoryRenderer categoryRenderer;
    private final HeaderRenderer headerRenderer;
    private final AvatarRenderer avatarRenderer;
    private final SearchHandler searchHandler;
    private final SearchRenderer searchRenderer;

    private ModuleCategory previousCategory = null;
    private ModuleCategory currentCategory = null;
    private float headerTransition = 1f;

    private static final float HEADER_SPEED = 3f;
    private long lastUpdateTime = System.currentTimeMillis();

    public BackgroundComponent() {
        this.backgroundRenderer = new BackgroundRenderer();
        this.categoryRenderer = new CategoryRenderer();
        this.headerRenderer = new HeaderRenderer();
        this.avatarRenderer = new AvatarRenderer();
        this.searchHandler = new SearchHandler();
        this.searchRenderer = new SearchRenderer(searchHandler);
    }

    public boolean isSearchActive() {
        return searchHandler.isSearchActive();
    }

    public float getSearchPanelAlpha() {
        return searchHandler.getSearchPanelAlpha();
    }

    public float getNormalPanelAlpha() {
        return searchHandler.getNormalPanelAlpha();
    }

    public void setSearchActive(boolean active) {
        searchHandler.setSearchActive(active);
    }

    public String getSearchText() {
        return searchHandler.getSearchText();
    }

    public List<ModuleStructure> getSearchResults() {
        return searchHandler.getSearchResults();
    }

    public ModuleStructure getSelectedSearchModule() {
        return searchHandler.getSelectedSearchModule();
    }

    public void updateAnimations(ModuleCategory selectedCategory, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        if (currentCategory != selectedCategory) {
            previousCategory = currentCategory;
            currentCategory = selectedCategory;
            headerTransition = 0f;
        }

        if (headerTransition < 1f) {
            headerTransition += HEADER_SPEED * deltaTime;
            if (headerTransition > 1f) {
                headerTransition = 1f;
            }
        }

        categoryRenderer.updateAnimations(selectedCategory, deltaTime);
        searchHandler.updateAnimations(deltaTime);
    }

    public void render(DrawContext context, float bgX, float bgY, ModuleCategory selectedCategory, float delta, float alphaMultiplier) {
        updateAnimations(selectedCategory, delta);
        backgroundRenderer.render(context, bgX, bgY, alphaMultiplier);
        avatarRenderer.render(context, bgX, bgY, alphaMultiplier);
    }

    public void renderCategoryPanel(float bgX, float bgY, float alphaMultiplier) {
        backgroundRenderer.renderCategoryPanel(bgX, bgY, BG_HEIGHT, alphaMultiplier);
    }

    public void renderHeader(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        headerRenderer.render(bgX, bgY, BG_WIDTH, selectedCategory, previousCategory, currentCategory,
                headerTransition, searchHandler, alphaMultiplier);
    }

    public void renderCategoryNames(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        categoryRenderer.render(bgX, bgY, selectedCategory, alphaMultiplier);
    }

    public void renderSearchResults(DrawContext context, float bgX, float bgY, float mouseX, float mouseY, int guiScale, float alphaMultiplier) {
        searchRenderer.render(context, bgX, bgY, BG_WIDTH, BG_HEIGHT, mouseX, mouseY, guiScale, alphaMultiplier);
    }

    public boolean handleSearchChar(char chr) {
        return searchHandler.handleSearchChar(chr);
    }

    public boolean handleSearchKey(int keyCode) {
        return searchHandler.handleSearchKey(keyCode);
    }

    public void handleSearchScroll(double vertical, float panelHeight) {
        searchHandler.handleSearchScroll(vertical, panelHeight);
    }

    public boolean isSearchBoxHovered(double mouseX, double mouseY, float bgX, float bgY) {
        return headerRenderer.isSearchBoxHovered(mouseX, mouseY, bgX, bgY);
    }

    public ModuleStructure getSearchModuleAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        return searchRenderer.getModuleAtPosition(mouseX, mouseY, bgX, bgY, BG_WIDTH, BG_HEIGHT, searchHandler);
    }

    public ModuleCategory getCategoryAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        return categoryRenderer.getCategoryAtPosition(mouseX, mouseY, bgX, bgY);
    }
}