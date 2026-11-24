package rich.screens.clickgui;

import rich.util.render.layout.Bounds;

/**
 * Вся геометрия ClickGui в одном месте. Рендер и хитбоксы обязаны брать
 * координаты отсюда, а не хардкодить свои копии.
 */
public final class ClickGuiLayout {

    public static final float BG_WIDTH = 420f;
    public static final float BG_HEIGHT = 260f;

    public static final float CONTENT_X = 92f;
    public static final float CONTENT_Y = 38f;
    // правый край контента совпадает с правым краем хедера (bgX + 412)
    public static final float CONTENT_W = 320f;
    public static final float CONTENT_H = BG_HEIGHT - 46f;

    public static final float MODULE_LIST_W = 126f;
    public static final float SETTINGS_X = 231f;
    public static final float SETTINGS_W = 181f;

    public static final float HEADER_Y = 7.5f;
    public static final float HEADER_H = 25f;

    public static final float SEARCH_X = 342f;
    public static final float SEARCH_Y = 12.5f;
    public static final float SEARCH_W = 70f;
    public static final float SEARCH_H = 15f;

    private ClickGuiLayout() {
    }

    public static Bounds background(float bgX, float bgY) {
        return Bounds.of(bgX, bgY, BG_WIDTH, BG_HEIGHT);
    }

    public static Bounds header(float bgX, float bgY) {
        return Bounds.of(bgX + CONTENT_X, bgY + HEADER_Y, CONTENT_W, HEADER_H);
    }

    public static Bounds searchBox(float bgX, float bgY) {
        return Bounds.of(bgX + SEARCH_X, bgY + SEARCH_Y, SEARCH_W, SEARCH_H);
    }

    public static Bounds moduleList(float bgX, float bgY) {
        return Bounds.of(bgX + CONTENT_X, bgY + CONTENT_Y, MODULE_LIST_W, CONTENT_H);
    }

    public static Bounds settingsPanel(float bgX, float bgY) {
        return Bounds.of(bgX + SETTINGS_X, bgY + CONTENT_Y, SETTINGS_W, CONTENT_H);
    }

    /** Полная контентная область: панель поиска, AutoBuy, конфиги. */
    public static Bounds contentPanel(float bgX, float bgY) {
        return Bounds.of(bgX + CONTENT_X, bgY + CONTENT_Y, CONTENT_W, CONTENT_H);
    }
}