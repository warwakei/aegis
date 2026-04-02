package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.BlockPos;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LongJump extends Module {
    SelectSetting modeSetting = new SelectSetting("Режим", "Режим прыжка")
            .value("Boat", "Shulker Screen", "Slime Boost", "FunTime Soul Sand").selected("Always");
    @NonFinal
    private boolean wasInShulkerScreen = false;
    @NonFinal
    private boolean wasOnSlimeBlock = false;
    private StopWatch timer = new StopWatch();
    public LongJump() {
        super("LongJump", "Long Jump", ModuleCategory.MOVEMENT);
        setup(modeSetting);
    }

    @EventHandler

    private void tickEvent(TickEvent event) {
        if (modeSetting.isSelected("Shulker Screen")) {
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                StopWatch speedTimer = new StopWatch();
                float speed = 0.9F;
                mc.player.addVelocity(0, speed, 0);
            }
        }

        if (modeSetting.isSelected("FunTime Soul Sand") && mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            mc.player.addVelocity(0, 0.56, 0);
        }

        if (modeSetting.isSelected("Boat")) {
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

//        if (modeSetting.isSelected("Boat")) {
//            for (Entity entity : mc.world.getEntities()) {
//                if (entity instanceof BoatEntity boat && mc.player.distanceTo(boat) < 2F) {
//                    if (mc.player.isOnGround()) {
//                        mc.options.jumpKey.setPressed(false);
//                        float yaw = (float) Math.toRadians(mc.player.getYaw());
//                        double x = -Math.sin(yaw) * 1.25;
//                        double z = Math.cos(yaw) * 1.25;
//                        double y = 1.3;
//                        mc.player.addVelocity(x, y, z);
//                        deactivate();
//                        break;
//                    }
//                }
//            }
//        }

        if (modeSetting.isSelected("Slime Boost")) {
            if (mc.player.isOnGround() && isOnSlimeBlock()) {
                wasOnSlimeBlock = true;
            } else if (wasOnSlimeBlock && !mc.player.isOnGround() && mc.player.getVelocity().getY() > 0) {
                mc.player.addVelocity(0, 1.35, 0);
                wasOnSlimeBlock = false;
            } else if (!isOnSlimeBlock()) {
                wasOnSlimeBlock = false;
            }
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
