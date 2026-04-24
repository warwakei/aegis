package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.option.KeyBinding;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShiftSpam extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    SliderSettings delayMs = new SliderSettings("Delay (ms)", "Delay between shift taps")
            .setValue(60)
            .range(10, 220);

    BindSetting holdBind = new BindSetting("Hold Key", "Hold this key to spam shift");

    public ShiftSpam() {
        super("ShiftSpam", "Spam shift while holding a key", ModuleCategory.PLAYER);
        settings(delayMs, holdBind);
    }

    @NonFinal boolean isControlling = false;
    @NonFinal boolean pressedState = false;

    @Override
    public void deactivate() {
        releaseSneak();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) {
            releaseSneak();
            return;
        }

        boolean shouldRun = PlayerInteractionHelper.isKey(holdBind);
        if (!shouldRun) {
            releaseSneak();
            return;
        }

        KeyBinding sneakKey = mc.options.sneakKey;

        // If the user is holding sneak themselves, don't fight them.
        if (sneakKey.isPressed() && !isControlling) {
            return;
        }

        if (!stopWatch.every(delayMs.getValue())) {
            return;
        }

        pressedState = !pressedState;
        sneakKey.setPressed(pressedState);
        isControlling = true;
    }

    private void releaseSneak() {
        if (!isControlling) return;
        mc.options.sneakKey.setPressed(false);
        isControlling = false;
        pressedState = false;
    }
}
