package rich.util.string;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyHelper {
    private static final Map<Integer, String> KEY_NAMES = new HashMap<>();
    private static final Map<String, Integer> NAME_TO_KEY = new HashMap<>();

    static {
        KEY_NAMES.put(GLFW.GLFW_KEY_SPACE, "Space");
        KEY_NAMES.put(GLFW.GLFW_KEY_APOSTROPHE, "'");
        KEY_NAMES.put(GLFW.GLFW_KEY_COMMA, ",");
        KEY_NAMES.put(GLFW.GLFW_KEY_MINUS, "-");
        KEY_NAMES.put(GLFW.GLFW_KEY_PERIOD, ".");
        KEY_NAMES.put(GLFW.GLFW_KEY_SLASH, "/");
        KEY_NAMES.put(GLFW.GLFW_KEY_0, "0");
        KEY_NAMES.put(GLFW.GLFW_KEY_1, "1");
        KEY_NAMES.put(GLFW.GLFW_KEY_2, "2");
        KEY_NAMES.put(GLFW.GLFW_KEY_3, "3");
        KEY_NAMES.put(GLFW.GLFW_KEY_4, "4");
        KEY_NAMES.put(GLFW.GLFW_KEY_5, "5");
        KEY_NAMES.put(GLFW.GLFW_KEY_6, "6");
        KEY_NAMES.put(GLFW.GLFW_KEY_7, "7");
        KEY_NAMES.put(GLFW.GLFW_KEY_8, "8");
        KEY_NAMES.put(GLFW.GLFW_KEY_9, "9");
        KEY_NAMES.put(GLFW.GLFW_KEY_SEMICOLON, ";");
        KEY_NAMES.put(GLFW.GLFW_KEY_EQUAL, "=");
        KEY_NAMES.put(GLFW.GLFW_KEY_A, "A");
        KEY_NAMES.put(GLFW.GLFW_KEY_B, "B");
        KEY_NAMES.put(GLFW.GLFW_KEY_C, "C");
        KEY_NAMES.put(GLFW.GLFW_KEY_D, "D");
        KEY_NAMES.put(GLFW.GLFW_KEY_E, "E");
        KEY_NAMES.put(GLFW.GLFW_KEY_F, "F");
        KEY_NAMES.put(GLFW.GLFW_KEY_G, "G");
        KEY_NAMES.put(GLFW.GLFW_KEY_H, "H");
        KEY_NAMES.put(GLFW.GLFW_KEY_I, "I");
        KEY_NAMES.put(GLFW.GLFW_KEY_J, "J");
        KEY_NAMES.put(GLFW.GLFW_KEY_K, "K");
        KEY_NAMES.put(GLFW.GLFW_KEY_L, "L");
        KEY_NAMES.put(GLFW.GLFW_KEY_M, "M");
        KEY_NAMES.put(GLFW.GLFW_KEY_N, "N");
        KEY_NAMES.put(GLFW.GLFW_KEY_O, "O");
        KEY_NAMES.put(GLFW.GLFW_KEY_P, "P");
        KEY_NAMES.put(GLFW.GLFW_KEY_Q, "Q");
        KEY_NAMES.put(GLFW.GLFW_KEY_R, "R");
        KEY_NAMES.put(GLFW.GLFW_KEY_S, "S");
        KEY_NAMES.put(GLFW.GLFW_KEY_T, "T");
        KEY_NAMES.put(GLFW.GLFW_KEY_U, "U");
        KEY_NAMES.put(GLFW.GLFW_KEY_V, "V");
        KEY_NAMES.put(GLFW.GLFW_KEY_W, "W");
        KEY_NAMES.put(GLFW.GLFW_KEY_X, "X");
        KEY_NAMES.put(GLFW.GLFW_KEY_Y, "Y");
        KEY_NAMES.put(GLFW.GLFW_KEY_Z, "Z");
        KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_BRACKET, "[");
        KEY_NAMES.put(GLFW.GLFW_KEY_BACKSLASH, "\\");
        KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_BRACKET, "]");
        KEY_NAMES.put(GLFW.GLFW_KEY_GRAVE_ACCENT, "`");
        KEY_NAMES.put(GLFW.GLFW_KEY_ESCAPE, "Escape");
        KEY_NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
        KEY_NAMES.put(GLFW.GLFW_KEY_TAB, "Tab");
        KEY_NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "Backspace");
        KEY_NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert");
        KEY_NAMES.put(GLFW.GLFW_KEY_DELETE, "Delete");
        KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
        KEY_NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
        KEY_NAMES.put(GLFW.GLFW_KEY_DOWN, "Down");
        KEY_NAMES.put(GLFW.GLFW_KEY_UP, "Up");
        KEY_NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "PageUp");
        KEY_NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "PageDown");
        KEY_NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
        KEY_NAMES.put(GLFW.GLFW_KEY_END, "End");
        KEY_NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "CapsLock");
        KEY_NAMES.put(GLFW.GLFW_KEY_SCROLL_LOCK, "ScrollLock");
        KEY_NAMES.put(GLFW.GLFW_KEY_NUM_LOCK, "NumLock");
        KEY_NAMES.put(GLFW.GLFW_KEY_PRINT_SCREEN, "PrintScreen");
        KEY_NAMES.put(GLFW.GLFW_KEY_PAUSE, "Pause");
        KEY_NAMES.put(GLFW.GLFW_KEY_F1, "F1");
        KEY_NAMES.put(GLFW.GLFW_KEY_F2, "F2");
        KEY_NAMES.put(GLFW.GLFW_KEY_F3, "F3");
        KEY_NAMES.put(GLFW.GLFW_KEY_F4, "F4");
        KEY_NAMES.put(GLFW.GLFW_KEY_F5, "F5");
        KEY_NAMES.put(GLFW.GLFW_KEY_F6, "F6");
        KEY_NAMES.put(GLFW.GLFW_KEY_F7, "F7");
        KEY_NAMES.put(GLFW.GLFW_KEY_F8, "F8");
        KEY_NAMES.put(GLFW.GLFW_KEY_F9, "F9");
        KEY_NAMES.put(GLFW.GLFW_KEY_F10, "F10");
        KEY_NAMES.put(GLFW.GLFW_KEY_F11, "F11");
        KEY_NAMES.put(GLFW.GLFW_KEY_F12, "F12");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_0, "Numpad0");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_1, "Numpad1");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_2, "Numpad2");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_3, "Numpad3");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_4, "Numpad4");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_5, "Numpad5");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_6, "Numpad6");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_7, "Numpad7");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_8, "Numpad8");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_9, "Numpad9");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_DECIMAL, "NumpadDecimal");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_DIVIDE, "NumpadDivide");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_MULTIPLY, "NumpadMultiply");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_SUBTRACT, "NumpadSubtract");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_ADD, "NumpadAdd");
        KEY_NAMES.put(GLFW.GLFW_KEY_KP_ENTER, "NumpadEnter");
        KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LShift");
        KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl");
        KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "LAlt");
        KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift");
        KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl");
        KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "RAlt");
        KEY_NAMES.put(GLFW.GLFW_KEY_MENU, "Menu");

        for (Map.Entry<Integer, String> entry : KEY_NAMES.entrySet()) {
            NAME_TO_KEY.put(entry.getValue().toLowerCase(), entry.getKey());
        }
    }

    public static String getKeyName(int keyCode) {
        return KEY_NAMES.getOrDefault(keyCode, "Unknown(" + keyCode + ")");
    }

    public static int getKeyCode(String name) {
        return NAME_TO_KEY.getOrDefault(name.toLowerCase(), -1);
    }

    public static boolean isValidKey(String name) {
        return NAME_TO_KEY.containsKey(name.toLowerCase());
    }

    public static String[] getAllKeyNames() {
        return KEY_NAMES.values().toArray(new String[0]);
    }
}