package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.BlockPos;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LongJump extends ModuleStructure {
    SelectSetting modeSetting = new SelectSetting("Режим", "Режим прыжка").value("Boat", "Shulker Screen", "Slime Boost", "FunTime Soul Sand").selected("Always");

    @NonFinal
    private boolean wasInShulkerScreen = false;

    @NonFinal
    private boolean wasOnSlimeBlock = false;

    private StopWatch timer = new StopWatch();

    public LongJump() {
        super("LongJump", "Long Jump", ModuleCategory.MOVEMENT);
        settings(modeSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    private void tickEvent(TickEvent event) {
        if (modeSetting.isSelected("Shulker Screen")) {
            handleShulkerScreen();
        }

        if (modeSetting.isSelected("FunTime Soul Sand") && mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            mc.player.addVelocity(0, 0.56, 0);
        }

        if (modeSetting.isSelected("Boat")) {
            handleBoatMode();
        }

        if (modeSetting.isSelected("Slime Boost")) {
            handleSlimeBoost();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleShulkerScreen() {
        if (mc.currentScreen instanceof ShulkerBoxScreen) {
            StopWatch speedTimer = new StopWatch();
            float speed = 0.9F;
            mc.player.addVelocity(0, speed, 0);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleBoatMode() {
        if (mc.currentScreen instanceof ShulkerBoxScreen) {
            float yaw = (float) Math.toRadians(mc.player.getYaw());
            double x = -Math.sin(yaw) * 1.0;
            double z = Math.cos(yaw) * 1.0;
            mc.player.addVelocity(0, 1, 0);
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 0.24, mc.player.getZ());
        }
        if (mc.currentScreen instanceof ShulkerBoxScreen) {
            wasInShulkerScreen = true;
        } else if (wasInShulkerScreen && mc.currentScreen == null && isNearShulkerBox()) {
            wasInShulkerScreen = false;
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleSlimeBoost() {
        if (mc.player.isOnGround() && isOnSlimeBlock()) {
            wasOnSlimeBlock = true;
        } else if (wasOnSlimeBlock && !mc.player.isOnGround() && mc.player.getVelocity().getY() > 0) {
            mc.player.addVelocity(0, 1.35, 0);
            wasOnSlimeBlock = false;
        } else if (!isOnSlimeBlock()) {
            wasOnSlimeBlock = false;
        }
    }

    private boolean isNearShulkerBox() {
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSlimeBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos belowPos = playerPos.down();
        return mc.world.getBlockState(belowPos).getBlock() instanceof SlimeBlock;
    }
}