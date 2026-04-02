package fun.aegis.features.impl.movement;

import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraFly extends Module {

    @NonFinal
    ItemStack currentStack = ItemStack.EMPTY;

    public ElytraFly() {
        super("ElytraFly", "ElytraFly", ModuleCategory.MOVEMENT);
        setup();
    }

    public static ElytraFly getInstance() {
        return Instance.get(ElytraFly.class);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        currentStack = ItemStack.EMPTY;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!state || mc.player == null || mc.world == null) return;
        handleFlyUpMode();
    }

    private void handleFlyUpMode() {
        currentStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!currentStack.getItem().equals(Items.ELYTRA)) {
            return;
        }

        if (mc.player.isOnGround()) {
            mc.player.jump();
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.42f, v.z);
            mc.player.setPitch(-90.0f);
        } else if (!mc.player.isGliding()) {
            PlayerInteractionHelper.startFallFlying();
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.5f, v.z);
            mc.player.setPitch(-90.0f);
        }

        mc.player.setPitch(0.0f);

        if (!mc.player.isOnGround()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.36f, v.z);
        }
    }
}
