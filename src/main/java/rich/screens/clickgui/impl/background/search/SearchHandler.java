package rich.screens.clickgui.impl.background.search;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.ModuleStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SearchHandler implements IMinecraft {

    private boolean searchActive = false;
    private String searchText = "";
    private int searchCursorPosition = 0;
    private int searchSelectionStart = -1;
    private int searchSelectionEnd = -1;
    private float searchCursorBlink = 0f;
    private float searchBoxAnimation = 0f;
    private float searchFocusAnimation = 0f;
    private float searchPanelAlpha = 0f;
    private float normalPanelAlpha = 1f;
    private float searchSelectionAnimation = 0f;
    private List<ModuleStructure> searchResults = new ArrayList<>();
    private Map<ModuleStructure, Float> searchResultAnimations = new HashMap<>();
    private Map<ModuleStructure, Long> searchResultAnimStartTimes = new HashMap<>();
    private float searchScrollOffset = 0f;
    private float searchTargetScroll = 0f;
    private int hoveredSearchIndex = -1;
    private ModuleStructure selectedSearchModule = null;

    private static final float SEARCH_ANIM_SPEED = 8f;
    private static final float PANEL_FADE_SPEED = 15f;
    private static final float SEARCH_RESULT_HEIGHT = 18f;
    private static final float SEARCH_RESULT_ANIM_DURATION = 200f;

    public void setSearchActive(boolean active) {
        if (active && !searchActive) {
            searchText = "";
            searchCursorPosition = 0;
            searchSelectionStart = -1;
            searchSelectionEnd = -1;
            searchResults.clear();
            searchResultAnimations.clear();
            searchResultAnimStartTimes.clear();
            searchScrollOffset = 0f;
            searchTargetScroll = 0f;
            hoveredSearchIndex = -1;
            selectedSearchModule = null;
        }
        searchActive = active;
    }

    public void updateAnimations(float deltaTime) {
        float searchTarget = searchActive ? 1f : 0f;
        searchBoxAnimation = updateAnimation(searchBoxAnimation, searchTarget, SEARCH_ANIM_SPEED, deltaTime);
        searchFocusAnimation = updateAnimation(searchFocusAnimation, searchTarget, SEARCH_ANIM_SPEED, deltaTime);
        searchPanelAlpha = updateAnimation(searchPanelAlpha, searchTarget, PANEL_FADE_SPEED, deltaTime);
        normalPanelAlpha = updateAnimation(normalPanelAlpha, searchActive ? 0f : 1f, PANEL_FADE_SPEED, deltaTime);
        searchSelectionAnimation = updateAnimation(searchSelectionAnimation, hasSearchSelection() ? 1f : 0f, SEARCH_ANIM_SPEED, deltaTime);

        if (searchActive) {
            searchCursorBlink += deltaTime * 2f;
            if (searchCursorBlink > 1f) searchCursorBlink -= 1f;
        }

        updateResultAnimations();
        updateScrollAnimation(deltaTime);
    }

    private float updateAnimation(float current, float target, float speed, float deltaTime) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            return target;
        }
        return current + diff * speed * deltaTime;
    }

    private void updateResultAnimations() {
        long currentTime = System.currentTimeMillis();
        for (ModuleStructure mod : searchResults) {
            Long startTime = searchResultAnimStartTimes.get(mod);
            if (startTime == null) continue;

            float elapsed = currentTime - startTime;
            float progress = Math.min(1f, Math.max(0f, elapsed / SEARCH_RESULT_ANIM_DURATION));
            progress = easeOutCubic(progress);
            searchResultAnimations.put(mod, progress);
        }
    }

    private void updateScrollAnimation(float deltaTime) {
        float scrollDiff = searchTargetScroll - searchScrollOffset;
        if (Math.abs(scrollDiff) < 0.5f) {
            searchScrollOffset = searchTargetScroll;
        } else {
            searchScrollOffset += scrollDiff * 12f * deltaTime;
        }
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }

    public boolean hasSearchSelection() {
        return searchSelectionStart != -1 && searchSelectionEnd != -1 && searchSelectionStart != searchSelectionEnd;
    }

    public int getSearchSelectionStart() {
        return Math.min(searchSelectionStart, searchSelectionEnd);
    }

    public int getSearchSelectionEnd() {
        return Math.max(searchSelectionStart, searchSelectionEnd);
    }

    private void clearSearchSelection() {
        searchSelectionStart = -1;
        searchSelectionEnd = -1;
    }

    private void selectAllSearchText() {
        searchSelectionStart = 0;
        searchSelectionEnd = searchText.length();
        searchCursorPosition = searchText.length();
    }

    private void deleteSelectedSearchText() {
        if (hasSearchSelection()) {
            int start = getSearchSelectionStart();
            int end = getSearchSelectionEnd();
            searchText = searchText.substring(0, start) + searchText.substring(end);
            searchCursorPosition = start;
            clearSearchSelection();
            updateSearchResults();
        }
    }

    private String getSelectedSearchText() {
        if (!hasSearchSelection()) return "";
        return searchText.substring(getSearchSelectionStart(), getSearchSelectionEnd());
    }

    private void copySearchToClipboard() {
        if (hasSearchSelection()) {
            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), getSelectedSearchText());
        }
    }

    private void pasteToSearch() {
        String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (clipboardText != null && !clipboardText.isEmpty()) {
            clipboardText = clipboardText.replaceAll("[\n\r\t]", "");

            if (hasSearchSelection()) {
                deleteSelectedSearchText();
            }

            searchText = searchText.substring(0, searchCursorPosition) + clipboardText + searchText.substring(searchCursorPosition);
            searchCursorPosition += clipboardText.length();
            updateSearchResults();
        }
    }

    private boolean isControlDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean isShiftDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public void updateSearchResults() {
        if (searchText.isEmpty()) {
            searchResults.clear();
            searchResultAnimations.clear();
            searchResultAnimStartTimes.clear();
            searchScrollOffset = 0f;
            searchTargetScroll = 0f;
            selectedSearchModule = null;
            return;
        }

        String query = searchText.toLowerCase();
        List<ModuleStructure> newResults = new ArrayList<>();
        Map<ModuleStructure, Float> oldAnimations = new HashMap<>(searchResultAnimations);

        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure module : repo.modules()) {
                    if (module.getName().toLowerCase().contains(query)) {
                        newResults.add(module);
                    }
                }
            }
        } catch (Exception ignored) {}

        searchResultAnimations.clear();
        searchResultAnimStartTimes.clear();

        long currentTime = System.currentTimeMillis();
        int newIndex = 0;

        for (int i = 0; i < newResults.size(); i++) {
            ModuleStructure module = newResults.get(i);

            if (oldAnimations.containsKey(module)) {
                float oldProgress = oldAnimations.get(module);
                searchResultAnimations.put(module, Math.max(oldProgress, 0.5f));
                searchResultAnimStartTimes.put(module, currentTime - (long)(SEARCH_RESULT_ANIM_DURATION * 0.85f));
            } else {
                searchResultAnimations.put(module, 0f);
                searchResultAnimStartTimes.put(module, currentTime + newIndex * 40L);
                newIndex++;
            }
        }

        searchResults = newResults;

        if (!searchResults.isEmpty()) {
            if (selectedSearchModule == null || !searchResults.contains(selectedSearchModule)) {
                selectedSearchModule = searchResults.get(0);
            }
        } else {
            selectedSearchModule = null;
        }
    }

    public boolean handleSearchChar(char chr) {
        if (!searchActive) return false;
        if (Character.isISOControl(chr)) return false;

        if (hasSearchSelection()) {
            deleteSelectedSearchText();
        }

        searchText = searchText.substring(0, searchCursorPosition) + chr + searchText.substring(searchCursorPosition);
        searchCursorPosition++;
        clearSearchSelection();
        updateSearchResults();
        return true;
    }

    public boolean handleSearchKey(int keyCode) {
        if (!searchActive) return false;

        if (isControlDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> { selectAllSearchText(); return true; }
                case GLFW.GLFW_KEY_C -> { copySearchToClipboard(); return true; }
                case GLFW.GLFW_KEY_V -> { pasteToSearch(); return true; }
                case GLFW.GLFW_KEY_X -> {
                    if (hasSearchSelection()) {
                        copySearchToClipboard();
                        deleteSelectedSearchText();
                    }
                    return true;
                }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSearchSelection()) {
                    deleteSelectedSearchText();
                } else if (searchCursorPosition > 0) {
                    searchText = searchText.substring(0, searchCursorPosition - 1) + searchText.substring(searchCursorPosition);
                    searchCursorPosition--;
                    updateSearchResults();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSearchSelection()) {
                    deleteSelectedSearchText();
                } else if (searchCursorPosition < searchText.length()) {
                    searchText = searchText.substring(0, searchCursorPosition) + searchText.substring(searchCursorPosition + 1);
                    updateSearchResults();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                handleLeftKey();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                handleRightKey();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                handleHomeKey();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                handleEndKey();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (!searchResults.isEmpty() && selectedSearchModule != null) {
                    int currentIndex = searchResults.indexOf(selectedSearchModule);
                    if (currentIndex > 0) {
                        selectedSearchModule = searchResults.get(currentIndex - 1);
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (!searchResults.isEmpty() && selectedSearchModule != null) {
                    int currentIndex = searchResults.indexOf(selectedSearchModule);
                    if (currentIndex < searchResults.size() - 1) {
                        selectedSearchModule = searchResults.get(currentIndex + 1);
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER -> {
                if (selectedSearchModule != null) {
                    selectedSearchModule.switchState();
                }
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                setSearchActive(false);
                return true;
            }
        }
        return false;
    }

    private void handleLeftKey() {
        if (hasSearchSelection() && !isShiftDown()) {
            searchCursorPosition = getSearchSelectionStart();
            clearSearchSelection();
        } else if (searchCursorPosition > 0) {
            if (isShiftDown()) {
                if (searchSelectionStart == -1) {
                    searchSelectionStart = searchCursorPosition;
                }
                searchCursorPosition--;
                searchSelectionEnd = searchCursorPosition;
            } else {
                searchCursorPosition--;
                clearSearchSelection();
            }
        }
    }

    private void handleRightKey() {
        if (hasSearchSelection() && !isShiftDown()) {
            searchCursorPosition = getSearchSelectionEnd();
            clearSearchSelection();
        } else if (searchCursorPosition < searchText.length()) {
            if (isShiftDown()) {
                if (searchSelectionStart == -1) {
                    searchSelectionStart = searchCursorPosition;
                }
                searchCursorPosition++;
                searchSelectionEnd = searchCursorPosition;
            } else {
                searchCursorPosition++;
                clearSearchSelection();
            }
        }
    }

    private void handleHomeKey() {
        if (isShiftDown()) {
            if (searchSelectionStart == -1) {
                searchSelectionStart = searchCursorPosition;
            }
            searchCursorPosition = 0;
            searchSelectionEnd = searchCursorPosition;
        } else {
            searchCursorPosition = 0;
            clearSearchSelection();
        }
    }

    private void handleEndKey() {
        if (isShiftDown()) {
            if (searchSelectionStart == -1) {
                searchSelectionStart = searchCursorPosition;
            }
            searchCursorPosition = searchText.length();
            searchSelectionEnd = searchCursorPosition;
        } else {
            searchCursorPosition = searchText.length();
            clearSearchSelection();
        }
    }

    public void handleSearchScroll(double vertical, float panelHeight) {
        if (!searchActive || searchResults.isEmpty()) return;

        float maxScroll = Math.max(0, searchResults.size() * (SEARCH_RESULT_HEIGHT + 2) - panelHeight + 10);
        searchTargetScroll = (float) Math.max(-maxScroll, Math.min(0, searchTargetScroll + vertical * 25));
    }

    public float getSearchResultHeight() {
        return SEARCH_RESULT_HEIGHT;
    }

    public void setHoveredSearchIndex(int index) {
        this.hoveredSearchIndex = index;
    }
}