package rich.events.impl;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import rich.IMinecraft;
import rich.events.api.events.Event;

public record KeyEvent(Screen screen, InputUtil.Type type, int key, int action) implements Event, IMinecraft {
    public boolean isKeyDown(int key) {
        return isKeyDown(key, mc.currentScreen == null);
    }

    public boolean isKeyDown(int key, boolean screen) {
        return this.key == key && action == 1 && screen;
    }

    public boolean isKeyReleased(int key) {
        return isKeyReleased(key, mc.currentScreen == null);
    }

    public boolean isKeyReleased(int key, boolean screen) {
        return this.key == key && action == 0 && screen;
    }
}
