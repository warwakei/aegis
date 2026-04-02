package fun.aegis.features.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.client.sound.SoundManager;
import fun.aegis.Aegis;
import fun.aegis.features.module.setting.SettingRepository;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.display.hud.Notifications;
import fun.aegis.display.hud.DynamicIsland;
import fun.aegis.features.impl.render.Hud;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Module extends SettingRepository implements QuickImports {
    String name;
    String visibleName;
    ModuleCategory category;
    Animation animation = new Decelerate().setMs(175).setValue(1);

    public Module(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.visibleName = name;
    }

    public Module(String name, String visibleName, ModuleCategory category) {
        this.name = name;
        this.visibleName = visibleName;
        this.category = category;
    }

    @NonFinal
    int key = GLFW.GLFW_KEY_UNKNOWN, type = 1;

    @NonFinal
    public boolean state;

    public void switchState() {
        setState(!state);
    }

    public void setState(boolean state) {
        animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
        if (state != this.state) {
            this.state = state;
            handleStateChange();
        }
    }

    private void handleStateChange() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float volume = Hud.getInstance().getModuleVolume();

        if (mc.player != null && mc.world != null) {
            if (state) {
                if (Hud.getInstance() != null && Hud.getInstance().notificationSettings.isSelected("Module Switch")) {
                    Notifications.getInstance().addList("Функция " + Formatting.GRAY + visibleName + Formatting.RESET + " - " + Formatting.GREEN + "включена" + Formatting.RESET + "! ", 2000, null);
                    SoundManager.playSound(SoundManager.ENABLE_MODULE, volume, 1.0f);
                }
                DynamicIsland.getInstance().showModuleNotification(visibleName, true);
                activate();
            } else {
                if (Hud.getInstance() != null && Hud.getInstance().notificationSettings.isSelected("Module Switch")) {
                    Notifications.getInstance().addList("Функция " + Formatting.GRAY + visibleName + Formatting.RESET + " - " + Formatting.RED + "выключена" + Formatting.RESET + "! ", 2000, null);
                    SoundManager.playSound(SoundManager.DISABLE_MODULE, volume, 1.0f);
                }
                DynamicIsland.getInstance().showModuleNotification(visibleName, false);
                deactivate();
            }
        }
        toggleSilent(state);
    }

    private void toggleSilent(boolean activate) {
        EventManager eventManager = Aegis.getInstance().getEventManager();
        if (activate) {
            eventManager.register(this);
        } else {
            eventManager.unregister(this);
        }
    }

    public void activate() {
    }

    public void deactivate() {
        animation.reset();
    }
}
