package rich.util.window;

import org.lwjgl.glfw.GLFWNativeWin32;
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.win32.*;

/**
 *  © 2025 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public class WindowStyle {

    public interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);
        HRESULT DwmSetWindowAttribute(HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    public static void setDarkMode(long windowHandle) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
        HWND hwndJna = new HWND(new Pointer(hwnd));
        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
        Memory darkModeEnabled = new Memory(4);
        darkModeEnabled.setInt(0, 1);
        DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeEnabled, 4);
    }
}