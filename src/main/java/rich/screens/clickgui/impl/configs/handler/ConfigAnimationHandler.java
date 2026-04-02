package rich.screens.clickgui.impl.configs.handler;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ConfigAnimationHandler {

    private final Map<String, Float> hoverAnimations = new HashMap<>();
    private final Map<String, Float> deleteHoverAnimations = new HashMap<>();
    private final Map<String, Float> loadHoverAnimations = new HashMap<>();
    private final Map<String, Float> refreshHoverAnimations = new HashMap<>();
    private final Map<String, Float> itemAppearAnimations = new HashMap<>();

    private float panelAlpha = 0f;
    private float panelSlide = 0f;
    private float createBoxAnimation = 0f;
    private float cursorBlink = 0f;
    private float selectedAnimation = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    public void reset() {
        panelAlpha = 0f;
        panelSlide = 0f;
        createBoxAnimation = 0f;
        itemAppearAnimations.clear();
        hoverAnimations.clear();
        deleteHoverAnimations.clear();
        loadHoverAnimations.clear();
        refreshHoverAnimations.clear();
    }

    public void initItemAnimations(List<String> configs) {
        for (String config : configs) {
            if (!itemAppearAnimations.containsKey(config)) {
                itemAppearAnimations.put(config, 0f);
            }
        }
    }

    public void update(boolean isActive, List<String> configs, boolean isCreating) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        updatePanelAnimations(isActive, deltaTime);
        updateCreateBoxAnimation(isCreating, deltaTime);
        updateCursorBlink(deltaTime);
        updateItemAnimations(isActive, configs, deltaTime);
        updateHoverAnimations(configs, deltaTime);
    }

    private void updatePanelAnimations(boolean isActive, float deltaTime) {
        float targetPanelAlpha = isActive ? 1f : 0f;
        float alphaDiff = targetPanelAlpha - panelAlpha;
        panelAlpha += alphaDiff * 16f * deltaTime;
        panelAlpha = Math.max(0f, Math.min(1f, panelAlpha));

        float targetSlide = isActive ? 1f : 0f;
        float slideDiff = targetSlide - panelSlide;
        panelSlide += slideDiff * 20f * deltaTime;
        panelSlide = Math.max(0f, Math.min(1f, panelSlide));
    }

    private void updateCreateBoxAnimation(boolean isCreating, float deltaTime) {
        float targetCreate = isCreating ? 1f : 0f;
        createBoxAnimation += (targetCreate - createBoxAnimation) * 14f * deltaTime;
    }

    private void updateCursorBlink(float deltaTime) {
        cursorBlink += deltaTime * 2f;
        if (cursorBlink > 1f) cursorBlink -= 1f;
    }

    private void updateItemAnimations(boolean isActive, List<String> configs, float deltaTime) {
        int index = 0;
        for (String config : configs) {
            float currentAppear = itemAppearAnimations.getOrDefault(config, 0f);
            float targetAppear;

            if (isActive) {
                float delay = index * 0.02f;
                if (panelAlpha > delay) {
                    targetAppear = 1f;
                } else {
                    targetAppear = 0f;
                }
            } else {
                targetAppear = 0f;
            }

            float speed = isActive ? 20f : 16f;
            float appearDiff = targetAppear - currentAppear;
            currentAppear += appearDiff * speed * deltaTime;
            itemAppearAnimations.put(config, Math.max(0f, Math.min(1f, currentAppear)));
            index++;
        }
    }

    private void updateHoverAnimations(List<String> configs, float deltaTime) {
        for (String config : configs) {
            float current = hoverAnimations.getOrDefault(config, 0f);
            hoverAnimations.put(config, current + (0f - current) * 8f * deltaTime);

            float deleteCurrent = deleteHoverAnimations.getOrDefault(config, 0f);
            deleteHoverAnimations.put(config, deleteCurrent + (0f - deleteCurrent) * 8f * deltaTime);

            float loadCurrent = loadHoverAnimations.getOrDefault(config, 0f);
            loadHoverAnimations.put(config, loadCurrent + (0f - loadCurrent) * 8f * deltaTime);

            float refreshCurrent = refreshHoverAnimations.getOrDefault(config, 0f);
            refreshHoverAnimations.put(config, refreshCurrent + (0f - refreshCurrent) * 8f * deltaTime);
        }
    }

    public void updateSelectedAnimation(boolean hasSelection, float deltaTime) {
        float targetSelected = hasSelection ? 1f : 0f;
        selectedAnimation += (targetSelected - selectedAnimation) * 8f * deltaTime;
    }

    public void setHoverAnimation(String config, float value) {
        hoverAnimations.put(config, value);
    }

    public void setDeleteHoverAnimation(String config, float value) {
        deleteHoverAnimations.put(config, value);
    }

    public void setLoadHoverAnimation(String config, float value) {
        loadHoverAnimations.put(config, value);
    }

    public void setRefreshHoverAnimation(String config, float value) {
        refreshHoverAnimations.put(config, value);
    }

    public float getItemAppearAnimation(String config) {
        return itemAppearAnimations.getOrDefault(config, 0f);
    }

    public float getHoverAnimation(String config) {
        return hoverAnimations.getOrDefault(config, 0f);
    }

    public float getDeleteHoverAnimation(String config) {
        return deleteHoverAnimations.getOrDefault(config, 0f);
    }

    public float getLoadHoverAnimation(String config) {
        return loadHoverAnimations.getOrDefault(config, 0f);
    }

    public float getRefreshHoverAnimation(String config) {
        return refreshHoverAnimations.getOrDefault(config, 0f);
    }

    public boolean isFullyHidden() {
        return panelAlpha < 0.01f && panelSlide < 0.01f;
    }
}