package rich.screens.clickgui.impl.module.handler;

import lombok.Getter;
import lombok.Setter;
import rich.modules.module.ModuleStructure;
import rich.util.interfaces.AbstractSettingComponent;

import java.util.*;

@Getter
@Setter
public class ModuleAnimationHandler {

    private Map<ModuleStructure, Float> moduleAnimations = new HashMap<>();
    private Map<ModuleStructure, Long> moduleAnimStartTimes = new HashMap<>();
    private Map<ModuleStructure, Float> oldModuleAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Float> settingAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Long> settingAnimStartTimes = new HashMap<>();
    private Map<AbstractSettingComponent, Float> visibilityAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Float> heightAnimations = new HashMap<>();

    private Map<ModuleStructure, Float> hoverAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> stateAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> selectedIconAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> favoriteAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> positionAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> moduleAlphaAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> bindBoxWidthAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> bindBoxAlphaAnimations = new HashMap<>();

    private List<ModuleStructure> oldModules = new ArrayList<>();
    private double oldModuleDisplayScroll = 0;

    private float selectedPulseAnimation = 0f;
    private long lastHoverUpdateTime = System.currentTimeMillis();
    private long lastStateUpdateTime = System.currentTimeMillis();
    private long lastIconUpdateTime = System.currentTimeMillis();
    private long lastFavoriteUpdateTime = System.currentTimeMillis();
    private long lastBindUpdateTime = System.currentTimeMillis();
    private long lastVisibilityUpdateTime = System.currentTimeMillis();

    private ModuleStructure highlightedModule = null;
    private long highlightStartTime = 0;
    private float highlightAnimation = 0f;

    private boolean scrollToModule = false;
    private ModuleStructure scrollTargetModule = null;

    private boolean isCategoryTransitioning = false;
    private float categoryTransitionProgress = 1f;
    private long categoryTransitionStartTime = 0;

    private static final float MODULE_ANIM_DURATION = 300f;
    private static final float SETTING_ANIM_DURATION = 450f;
    private static final float CATEGORY_TRANSITION_DURATION = 280f;
    private static final float HIGHLIGHT_DURATION = 2000f;
    private static final float HOVER_ANIM_SPEED = 8f;
    private static final float STATE_ANIM_SPEED = 10f;
    private static final float ICON_ANIM_SPEED = 10f;
    private static final float FAVORITE_ANIM_SPEED = 8f;
    private static final float POSITION_ANIM_SPEED = 6f;
    private static final float BIND_WIDTH_ANIM_SPEED = 12f;
    private static final float PULSE_SPEED = 5.5f;
    private static final float VISIBILITY_ANIM_SPEED = 8f;
    private static final float HEIGHT_ANIM_SPEED = 10f;
    private static final float CORNER_INSET = 3f;
    private static final float MODULE_ITEM_HEIGHT = 22f;

    public void prepareTransition(List<ModuleStructure> modules, List<ModuleStructure> displayModules) {
        if (!modules.isEmpty()) {
            oldModules = new ArrayList<>(modules);
            oldModuleAnimations = new HashMap<>(moduleAnimations);
            isCategoryTransitioning = true;
            categoryTransitionStartTime = System.currentTimeMillis();
            categoryTransitionProgress = 0f;
        }
    }

    public void initModuleAnimations(List<ModuleStructure> displayModules) {
        moduleAnimations.clear();
        moduleAnimStartTimes.clear();
        hoverAnimations.clear();
        stateAnimations.clear();
        selectedIconAnimations.clear();
        bindBoxWidthAnimations.clear();
        bindBoxAlphaAnimations.clear();

        long currentTime = System.currentTimeMillis();
        long delayBase = (long) (CATEGORY_TRANSITION_DURATION * 0.3f);

        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure mod = displayModules.get(i);
            moduleAnimations.put(mod, 0f);
            moduleAnimStartTimes.put(mod, currentTime + delayBase + i * 25L);
            hoverAnimations.put(mod, 0f);
            stateAnimations.put(mod, mod.isState() ? 1f : 0f);
            selectedIconAnimations.put(mod, 0f);
            favoriteAnimations.put(mod, mod.isFavorite() ? 1f : 0f);
            positionAnimations.put(mod, 1f);
            moduleAlphaAnimations.put(mod, 1f);
        }
    }

    public void initSettingAnimations(List<AbstractSettingComponent> settingComponents) {
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent comp = settingComponents.get(i);
            settingAnimations.put(comp, 0f);
            settingAnimStartTimes.put(comp, currentTime + i * 25L);
            boolean visible = comp.getSetting().isVisible();
            visibilityAnimations.put(comp, visible ? 1f : 0f);
            heightAnimations.put(comp, visible ? 1f : 0f);
        }
    }

    public void clearSettingAnimations() {
        settingAnimations.clear();
        settingAnimStartTimes.clear();
        visibilityAnimations.clear();
        heightAnimations.clear();
    }

    public void updateAll(List<ModuleStructure> displayModules, ModuleStructure selectedModule,
                          float mouseX, float mouseY, float listX, float listY, float listWidth, float listHeight,
                          float scrollOffset) {
        updateCategoryTransition();
        updateModuleAnimations(displayModules);
        updateStateAnimations(displayModules);
        updateSelectedIconAnimations(displayModules, selectedModule);
        updateFavoriteAnimations(displayModules);
        updateBindAnimations(displayModules);
        updateHighlightAnimation();
        updateHoverAnimations(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight, scrollOffset);
    }

    private void updateCategoryTransition() {
        if (!isCategoryTransitioning) return;

        long elapsed = System.currentTimeMillis() - categoryTransitionStartTime;
        float progress = Math.min(1f, elapsed / CATEGORY_TRANSITION_DURATION);
        categoryTransitionProgress = easeOutCubic(progress);

        if (progress >= 1f) {
            isCategoryTransitioning = false;
            oldModules.clear();
            oldModuleAnimations.clear();
            categoryTransitionProgress = 1f;
        }
    }

    private void updateModuleAnimations(List<ModuleStructure> displayModules) {
        long currentTime = System.currentTimeMillis();
        for (ModuleStructure mod : displayModules) {
            Long startTime = moduleAnimStartTimes.get(mod);
            if (startTime == null) continue;

            float elapsed = currentTime - startTime;
            float progress = Math.min(1f, Math.max(0f, elapsed / MODULE_ANIM_DURATION));
            progress = easeOutCubic(progress);
            moduleAnimations.put(mod, progress);
        }
    }

    private void updateStateAnimations(List<ModuleStructure> displayModules) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastStateUpdateTime) / 1000f, 0.1f);
        lastStateUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            float currentAnim = stateAnimations.getOrDefault(module, module.isState() ? 1f : 0f);
            float targetAnim = module.isState() ? 1f : 0f;
            stateAnimations.put(module, animateTowards(currentAnim, targetAnim, STATE_ANIM_SPEED, deltaTime));
        }
    }

    private void updateSelectedIconAnimations(List<ModuleStructure> displayModules, ModuleStructure selectedModule) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastIconUpdateTime) / 1000f, 0.1f);
        lastIconUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            float currentAnim = selectedIconAnimations.getOrDefault(module, 0f);
            float targetAnim = (module == selectedModule) ? 1f : 0f;
            selectedIconAnimations.put(module, animateTowards(currentAnim, targetAnim, ICON_ANIM_SPEED, deltaTime));
        }
    }

    private void updateFavoriteAnimations(List<ModuleStructure> displayModules) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastFavoriteUpdateTime) / 1000f, 0.1f);
        lastFavoriteUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            float currentFavAnim = favoriteAnimations.getOrDefault(module, 0f);
            float targetFavAnim = module.isFavorite() ? 1f : 0f;
            favoriteAnimations.put(module, animateTowards(currentFavAnim, targetFavAnim, FAVORITE_ANIM_SPEED, deltaTime));

            float currentPosAnim = positionAnimations.getOrDefault(module, 1f);
            if (currentPosAnim < 1f) {
                positionAnimations.put(module, Math.min(1f, currentPosAnim + POSITION_ANIM_SPEED * deltaTime));
            }

            float currentAlphaAnim = moduleAlphaAnimations.getOrDefault(module, 1f);
            if (currentAlphaAnim < 1f) {
                moduleAlphaAnimations.put(module, Math.min(1f, currentAlphaAnim + POSITION_ANIM_SPEED * deltaTime));
            }
        }
    }

    private void updateBindAnimations(List<ModuleStructure> displayModules) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastBindUpdateTime) / 1000f, 0.1f);
        lastBindUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            int key = module.getKey();
            boolean hasBind = key != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN && key != -1;
            float currentAlpha = bindBoxAlphaAnimations.getOrDefault(module, 0f);
            float targetAlpha = hasBind ? 1f : 0f;
            bindBoxAlphaAnimations.put(module, animateTowards(currentAlpha, targetAlpha, BIND_WIDTH_ANIM_SPEED, deltaTime));
        }
    }

    private void updateHoverAnimations(List<ModuleStructure> displayModules, float mouseX, float mouseY,
                                       float listX, float listY, float listWidth, float listHeight, float scrollOffset) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastHoverUpdateTime) / 1000f, 0.1f);
        lastHoverUpdateTime = currentTime;

        selectedPulseAnimation += deltaTime * PULSE_SPEED;
        if (selectedPulseAnimation > Math.PI * 2) {
            selectedPulseAnimation -= (float) (Math.PI * 2);
        }

        float topInset = CORNER_INSET;
        float bottomInset = CORNER_INSET;
        float startY = listY + topInset + 2f + scrollOffset;
        float itemHeight = MODULE_ITEM_HEIGHT;

        float visibleTop = listY + topInset;
        float visibleBottom = listY + listHeight - bottomInset;

        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure module = displayModules.get(i);
            float modY = startY + i * (itemHeight + 2);

            boolean isInVisibleArea = modY + itemHeight >= visibleTop && modY <= visibleBottom;

            boolean isHovered = !isCategoryTransitioning &&
                    isInVisibleArea &&
                    mouseX >= listX + 3 && mouseX <= listX + listWidth - 3 &&
                    mouseY >= Math.max(modY, visibleTop) && mouseY <= Math.min(modY + itemHeight, visibleBottom) &&
                    mouseY >= modY && mouseY <= modY + itemHeight;

            float currentHover = hoverAnimations.getOrDefault(module, 0f);
            float targetHover = isHovered ? 1f : 0f;
            hoverAnimations.put(module, animateTowards(currentHover, targetHover, HOVER_ANIM_SPEED, deltaTime));
        }
    }

    private void updateHighlightAnimation() {
        if (highlightedModule == null) return;

        long elapsed = System.currentTimeMillis() - highlightStartTime;

        if (elapsed >= HIGHLIGHT_DURATION) {
            long fadeElapsed = elapsed - (long) HIGHLIGHT_DURATION;
            float fadeProgress = fadeElapsed / 500f;

            if (fadeProgress >= 1f) {
                highlightedModule = null;
                highlightAnimation = 0f;
            } else {
                highlightAnimation = 1f - fadeProgress;
            }
        } else {
            highlightAnimation = 1f;
        }
    }

    public void updateSettingAnimations(List<AbstractSettingComponent> settingComponents) {
        long currentTime = System.currentTimeMillis();
        for (AbstractSettingComponent comp : settingComponents) {
            Long startTime = settingAnimStartTimes.get(comp);
            if (startTime == null) continue;

            float elapsed = currentTime - startTime;
            float progress = Math.min(1f, Math.max(0f, elapsed / SETTING_ANIM_DURATION));
            progress = easeOutCubic(progress);
            settingAnimations.put(comp, progress);
        }
    }

    public void updateVisibilityAnimations(List<AbstractSettingComponent> settingComponents) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastVisibilityUpdateTime) / 1000f, 0.1f);
        lastVisibilityUpdateTime = currentTime;

        for (AbstractSettingComponent comp : settingComponents) {
            boolean isVisible = comp.getSetting().isVisible();
            float currentVisAnim = visibilityAnimations.getOrDefault(comp, isVisible ? 1f : 0f);
            float currentHeightAnim = heightAnimations.getOrDefault(comp, isVisible ? 1f : 0f);

            float visTarget = isVisible ? 1f : 0f;
            float heightTarget = isVisible ? 1f : 0f;

            heightAnimations.put(comp, animateTowards(currentHeightAnim, heightTarget, HEIGHT_ANIM_SPEED, deltaTime));
            visibilityAnimations.put(comp, animateTowards(currentVisAnim, visTarget, VISIBILITY_ANIM_SPEED, deltaTime));
        }
    }

    public void startHighlight(ModuleStructure module) {
        highlightedModule = module;
        highlightStartTime = System.currentTimeMillis();
        highlightAnimation = 1f;
    }

    public void setScrollTarget(ModuleStructure module) {
        scrollToModule = true;
        scrollTargetModule = module;
    }

    public boolean shouldScrollToModule() {
        return scrollToModule;
    }

    public void clearScrollTarget() {
        scrollToModule = false;
        scrollTargetModule = null;
    }

    private float animateTowards(float current, float target, float speed, float deltaTime) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * speed * deltaTime;
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }

    public float easeInCubic(float x) {
        return x * x * x;
    }

    public float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1 - x, 4);
    }

    public float getCategorySlideDistance() {
        return 40f;
    }
}