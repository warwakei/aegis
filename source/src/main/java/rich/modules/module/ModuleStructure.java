package rich.modules.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.ModuleToggleEvent;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.SettingRepository;
import rich.modules.impl.render.Hud;
import rich.screens.hud.Notifications;
import rich.util.animations.Animation;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleStructure extends SettingRepository implements IMinecraft {
    String name;
    String description;
    ModuleCategory category;
    Animation animation = new Decelerate().setMs(175).setValue(1);

    public ModuleStructure(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.description = "";
    }

    public ModuleStructure(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    @NonFinal
    int key = GLFW.GLFW_KEY_UNKNOWN, type = 1;

    @NonFinal
    public boolean state;

    @NonFinal
    public boolean favorite;

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

    public void switchFavorite() {
        setFavorite(!favorite);
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    private void handleStateChange() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null && mc.world != null) {
            Hud hud = Hud.getInstance();
            Notifications notifications = Notifications.getInstance();

            if (hud != null && hud.isState() && notifications != null) {
                if (hud.interfaceSettings.isSelected("Notifications")) {
                    if (state) {
                        notifications.addNotification("Feature "  + name + " - enabled!", 2000);
                    } else {
                        notifications.addNotification("Feature " + name + " - disabled!", 2000);
                    }
                }
            }

            if (state) {
                activate();
            } else {
                deactivate();
            }
        }
        toggleSilent(state);

        ModuleToggleEvent event = new ModuleToggleEvent(this, state);
        EventManager.callEvent(event);
    }

    private void toggleSilent(boolean activate) {
        var eventManager = Initialization.getInstance().getManager().getEventManager();
        if (activate) {
            eventManager.register(this);
        } else {
            eventManager.unregister(this);
        }
    }

    public void activate() {
    }

    public void deactivate() {
    }
}