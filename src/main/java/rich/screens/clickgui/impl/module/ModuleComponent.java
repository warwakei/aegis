package rich.screens.clickgui.impl.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import rich.IMinecraft;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.SettingComponentAdder;
import rich.screens.clickgui.impl.module.handler.ModuleAnimationHandler;
import rich.screens.clickgui.impl.module.handler.ModuleBindHandler;
import rich.screens.clickgui.impl.module.handler.ModuleFavoriteHandler;
import rich.screens.clickgui.impl.module.handler.ModuleScrollHandler;
import rich.screens.clickgui.impl.module.render.ModuleListRenderer;
import rich.screens.clickgui.impl.module.render.SettingsPanelRenderer;
import rich.screens.clickgui.impl.module.util.ModuleDisplayHelper;
import rich.screens.clickgui.impl.settingsrender.*;
import rich.util.interfaces.AbstractSettingComponent;

import java.util.*;

@Getter
@Setter
public class ModuleComponent implements IMinecraft {

    private List<ModuleStructure> modules = new ArrayList<>();
    private List<ModuleStructure> displayModules = new ArrayList<>();
    private ModuleStructure selectedModule = null;
    private ModuleStructure bindingModule = null;
    private List<AbstractSettingComponent> settingComponents = new ArrayList<>();

    private ModuleCategory currentCategory = null;

    private final ModuleAnimationHandler animationHandler;
    private final ModuleScrollHandler scrollHandler;
    private final ModuleFavoriteHandler favoriteHandler;
    private final ModuleBindHandler bindHandler;
    private final ModuleListRenderer listRenderer;
    private final SettingsPanelRenderer settingsRenderer;
    private final ModuleDisplayHelper displayHelper;

    private int savedGuiScale = 1;
    private float lastMouseX = 0, lastMouseY = 0;
    private float lastListX = 0, lastListY = 0, lastListWidth = 0, lastListHeight = 0;

    public ModuleComponent() {
        this.animationHandler = new ModuleAnimationHandler();
        this.scrollHandler = new ModuleScrollHandler();
        this.favoriteHandler = new ModuleFavoriteHandler();
        this.bindHandler = new ModuleBindHandler();
        this.displayHelper = new ModuleDisplayHelper();
        this.listRenderer = new ModuleListRenderer(animationHandler, bindHandler, displayHelper);
        this.settingsRenderer = new SettingsPanelRenderer(animationHandler);
    }

    public void updateModules(List<ModuleStructure> newModules, ModuleCategory category) {
        if (category == currentCategory) return;

        animationHandler.prepareTransition(modules, displayModules);
        currentCategory = category;
        modules = newModules;
        rebuildDisplayList();

        scrollHandler.resetModuleScroll();
        animationHandler.initModuleAnimations(displayModules);
        displayHelper.updateModulesWithSettings(displayModules);

        if (animationHandler.shouldScrollToModule() && displayModules.contains(animationHandler.getScrollTargetModule())) {
            scrollToModuleAndHighlight(animationHandler.getScrollTargetModule());
            animationHandler.clearScrollTarget();
        } else if (!displayModules.isEmpty() && (selectedModule == null || !displayModules.contains(selectedModule))) {
            selectModule(displayModules.get(0));
        } else if (displayModules.isEmpty()) {
            selectedModule = null;
            settingComponents.clear();
        }
    }

    private void rebuildDisplayList() {
        displayModules.clear();
        List<ModuleStructure> favorites = new ArrayList<>();
        List<ModuleStructure> nonFavorites = new ArrayList<>();

        for (ModuleStructure mod : modules) {
            if (mod.isFavorite()) favorites.add(mod);
            else nonFavorites.add(mod);
        }

        displayModules.addAll(favorites);
        displayModules.addAll(nonFavorites);
    }

    public void toggleFavorite(ModuleStructure module) {
        favoriteHandler.toggleFavorite(module, displayModules, animationHandler);
        rebuildDisplayList();
    }

    public void selectModuleFromSearch(ModuleStructure module) {
        animationHandler.setScrollTarget(module);
    }

    public void scrollToModuleAndHighlight(ModuleStructure module) {
        if (module == null || !displayModules.contains(module)) return;

        selectModule(module);
        int moduleIndex = displayModules.indexOf(module);
        if (moduleIndex >= 0 && scrollHandler.getLastModuleListHeight() > 0) {
            scrollHandler.scrollToModule(moduleIndex, displayModules.size());
        }
        animationHandler.startHighlight(module);
    }

    public void selectModule(ModuleStructure module) {
        if (module == selectedModule) return;

        selectedModule = module;
        scrollHandler.resetSettingScroll();
        settingComponents.clear();
        animationHandler.clearSettingAnimations();

        if (module == null) return;

        new SettingComponentAdder().addSettingComponent(module.settings(), settingComponents);
        animationHandler.initSettingAnimations(settingComponents);
    }

    public void renderModuleList(DrawContext context, float x, float y, float width, float height,
                                 float mouseX, float mouseY, int guiScale, float alphaMultiplier) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastListX = x;
        lastListY = y;
        lastListWidth = width;
        lastListHeight = height;

        animationHandler.updateAll(displayModules, selectedModule, mouseX, mouseY, x, y, width, height,
                (float) scrollHandler.getModuleDisplayScroll());
        listRenderer.render(context, displayModules, selectedModule, bindingModule, x, y, width, height,
                mouseX, mouseY, guiScale, alphaMultiplier, animationHandler, scrollHandler);
    }

    public void renderSettingsPanel(DrawContext context, float x, float y, float width, float height,
                                    float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        savedGuiScale = guiScale;
        settingsRenderer.render(context, selectedModule, settingComponents, x, y, width, height,
                mouseX, mouseY, delta, guiScale, alphaMultiplier, scrollHandler, animationHandler);
    }

    public void updateScroll(float delta, float scrollSpeed) {
        scrollHandler.update(delta);
    }

    public void updateScrollFades(float delta, float scrollSpeed, float moduleListHeight, float settingsPanelHeight) {
        scrollHandler.updateFades(displayModules.size(), calculateTotalSettingHeight(), moduleListHeight, settingsPanelHeight);
    }

    public float calculateTotalSettingHeight() {
        return settingsRenderer.calculateTotalHeight(settingComponents, animationHandler);
    }

    public ModuleStructure getModuleAtPosition(double mouseX, double mouseY, float listX, float listY,
                                               float listWidth, float listHeight) {
        return listRenderer.getModuleAtPosition(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight,
                scrollHandler.getModuleDisplayScroll(), animationHandler.isCategoryTransitioning());
    }

    public boolean isStarClicked(double mouseX, double mouseY, float listX, float listY,
                                 float listWidth, float listHeight) {
        return listRenderer.isStarClicked(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight,
                scrollHandler.getModuleDisplayScroll(), displayHelper, animationHandler.isCategoryTransitioning());
    }

    public ModuleStructure getModuleForStarClick(double mouseX, double mouseY, float listX, float listY,
                                                 float listWidth, float listHeight) {
        return listRenderer.getModuleForStarClick(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight,
                scrollHandler.getModuleDisplayScroll(), displayHelper, animationHandler.isCategoryTransitioning());
    }

    public void handleModuleScroll(double vertical, float listHeight) {
        if (animationHandler.isCategoryTransitioning()) return;
        scrollHandler.handleModuleScroll(vertical, listHeight, displayModules.size());
    }

    public void handleSettingScroll(double vertical, float panelHeight) {
        scrollHandler.handleSettingScroll(vertical, panelHeight, calculateTotalSettingHeight());
    }

    public void tick() {
        settingComponents.forEach(AbstractSettingComponent::tick);
    }

    public boolean isTransitioning() {
        return animationHandler.isCategoryTransitioning();
    }
}