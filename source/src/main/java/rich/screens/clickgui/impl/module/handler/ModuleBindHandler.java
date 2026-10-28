package rich.screens.clickgui.impl.module.handler;

import org.lwjgl.glfw.GLFW;
import rich.screens.clickgui.impl.settingsrender.BindComponent;

public class ModuleBindHandler {

    public String getBindDisplayName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) return "";

        if (key == BindComponent.SCROLL_UP_BIND) return "Up";
        if (key == BindComponent.SCROLL_DOWN_BIND) return "Dn";
        if (key == BindComponent.MIDDLE_MOUSE_BIND) return "M3";

        String keyName = GLFW.glfwGetKeyName(key, 0);
        if (keyName != null) {
            return keyName.toUpperCase();
        }

        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LS";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RS";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LC";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RC";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LA";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RA";
            case GLFW.GLFW_KEY_SPACE -> "Sp";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Cap";
            case GLFW.GLFW_KEY_ENTER -> "Ent";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bk";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_HOME -> "Hm";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PU";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PD";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Dn";
            case GLFW.GLFW_KEY_LEFT -> "Lt";
            case GLFW.GLFW_KEY_RIGHT -> "Rt";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            default -> "K" + key;
        };
    }
}