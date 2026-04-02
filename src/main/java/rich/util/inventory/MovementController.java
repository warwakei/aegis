package rich.util.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class MovementController {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean forward, back, left, right, jump, sprint;
    private boolean saved = false;
    private boolean blocked = false;

    public void saveState() {
        if (mc.player == null) return;
        forward = isKeyPressed(mc.options.forwardKey);
        back = isKeyPressed(mc.options.backKey);
        left = isKeyPressed(mc.options.leftKey);
        right = isKeyPressed(mc.options.rightKey);
        jump = isKeyPressed(mc.options.jumpKey);
        sprint = mc.player.isSprinting();
        saved = true;
    }

    public void block() {
        if (mc.player == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        blocked = true;
    }

    public void stopSprint() {
        if (mc.player != null) {
            mc.player.setSprinting(false);
            mc.options.sprintKey.setPressed(false);
        }
    }

    public void restore() {
        if (!saved) return;
        mc.options.forwardKey.setPressed(forward && isCurrentlyPressed(mc.options.forwardKey));
        mc.options.backKey.setPressed(back && isCurrentlyPressed(mc.options.backKey));
        mc.options.leftKey.setPressed(left && isCurrentlyPressed(mc.options.leftKey));
        mc.options.rightKey.setPressed(right && isCurrentlyPressed(mc.options.rightKey));
        mc.options.jumpKey.setPressed(jump && isCurrentlyPressed(mc.options.jumpKey));
        blocked = false;
        saved = false;
    }

    public void restoreFromCurrent() {
        mc.options.forwardKey.setPressed(isCurrentlyPressed(mc.options.forwardKey));
        mc.options.backKey.setPressed(isCurrentlyPressed(mc.options.backKey));
        mc.options.leftKey.setPressed(isCurrentlyPressed(mc.options.leftKey));
        mc.options.rightKey.setPressed(isCurrentlyPressed(mc.options.rightKey));
        mc.options.jumpKey.setPressed(isCurrentlyPressed(mc.options.jumpKey));
        mc.options.sprintKey.setPressed(isCurrentlyPressed(mc.options.sprintKey));
        blocked = false;
    }

    public boolean isPlayerStopped(double threshold) {
        if (mc.player == null) return true;
        double vx = Math.abs(mc.player.getVelocity().x);
        double vz = Math.abs(mc.player.getVelocity().z);
        return vx < threshold && vz < threshold;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void reset() {
        saved = false;
        blocked = false;
    }

    private boolean isKeyPressed(KeyBinding key) {
        return key.isPressed();
    }

    private boolean isCurrentlyPressed(KeyBinding key) {
        return InputUtil.isKeyPressed(
                mc.getWindow(),
                InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode()
        );
    }
}