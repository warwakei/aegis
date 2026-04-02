package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BowSpammer extends ModuleStructure {

    final SliderSettings delay = new SliderSettings("Задержка", "Задержка между выстрелами")
            .range(2.2f, 5.0f).setValue(2.5f);

    public BowSpammer() {
        super("BowSpammer", "Bow Spammer", ModuleCategory.COMBAT);
        settings(delay);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.getNetworkHandler() == null) return;

        if (!canShoot()) return;

        sendShootPackets();
        mc.player.stopUsingItem();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean canShoot() {
        if (!(mc.player.getMainHandStack().getItem() instanceof BowItem)) return false;
        if (!mc.player.isUsingItem()) return false;
        return mc.player.getItemUseTime() >= delay.getValue();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void sendShootPackets() {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));

        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                0,
                mc.player.getYaw(),
                mc.player.getPitch()
        ));
    }
}