package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import rich.events.api.EventHandler;
import rich.events.impl.PlayerTravelEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.move.MoveUtil;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Speed extends ModuleStructure {

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим скорости")
            .value("Vanilla", "Grim", "FunTime", "HolyWorld")
            .selected("Grim");

    SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения")
            .range(1.0f, 5.0f)
            .setValue(1.5f)
            .visible(() -> mode.isSelected("Vanilla"));

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        settings(mode, speed);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onTick(TickEvent e) {
        if (mode.isSelected("Vanilla")) {
            MoveUtil.setVelocity(speed.getValue() / 3);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onMotion(PlayerTravelEvent e) {
        if (mode.isSelected("FunTime")) {
            handleFunTimeMode();
        }
        if (mode.isSelected("Grim") && e.isPre() && MoveUtil.hasPlayerMovement()) {
            handleGrimMode();
        }

        if (mode.isSelected("HolyWorld") && e.isPre() && MoveUtil.hasPlayerMovement()) {
            handleHolyWorldMode();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleFunTimeMode() {
        if (!mc.player.isSwimming() && !mc.player.isGliding() && !mc.player.isSneaking()) {
            if (mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY < 1.5f) {
                float motion = mc.player.hasStatusEffect(StatusEffects.SPEED) ? 0.32f : 0.28f;
                MoveUtil.setVelocity(motion);
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleGrimMode() {
        int collisions = 0;

        for (Entity ent : mc.world.getEntities())
            if (ent != mc.player && (!(ent instanceof ArmorStandEntity)) && (ent instanceof LivingEntity || ent instanceof BoatEntity) && mc.player.getBoundingBox().expand(0.5f).intersects(ent.getBoundingBox()))
                collisions++;
        double[] motion = MoveUtil.forward(0.07 * collisions);
        mc.player.addVelocity(motion[0], 0, motion[1]);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleHolyWorldMode() {
        int collisions = 0;

        for (Entity ent : mc.world.getEntities())
            if (ent != mc.player && (!(ent instanceof ArmorStandEntity)) && (ent instanceof LivingEntity || ent instanceof BoatEntity) && mc.player.getBoundingBox().expand(0.35f).intersects(ent.getBoundingBox()))
                collisions++;
        double[] motion = MoveUtil.forward(0.0205 * collisions);
        mc.player.addVelocity(motion[0], 0, motion[1]);
    }

    private boolean hasSprintingTarget() {
        return false;
    }
}