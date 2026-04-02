package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;

import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.events.player.SwimmingEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WaterSpeed extends Module {

    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим обхода").value("FunTime", "FunTime New").selected("FunTime");

    private final SliderSettings fallSpeed = new SliderSettings("Скорость падения", "")
            .range(0.01f, 0.5f)
            .visible(() -> modeSetting.isSelected("FunTime New"))
            .setValue(0.1f);

    private final SliderSettings testSpeed = new SliderSettings("Скорость в воде", "")
            .range(1.0f, 2.0f)
            .visible(() -> modeSetting.isSelected("FunTime New"))
            .setValue(1.175f);

    private final BooleanSetting onlyFalling = new BooleanSetting("Только при падении", "")
            .visible(() -> modeSetting.isSelected("FunTime New"))
            .setValue(true);

    private final BooleanSetting inLava = new BooleanSetting("Работает в лаве", "")
            .visible(() -> modeSetting.isSelected("FunTime New"))
            .setValue(false);

    private int waterTicks = 0;

    public WaterSpeed() {
        super("WaterSpeed", "Water Speed", ModuleCategory.MOVEMENT);
        setup(modeSetting, fallSpeed, testSpeed, onlyFalling, inLava);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (modeSetting.isSelected("FunTime") && mc.player.isSwimming() && mc.player.isOnGround()) {
            mc.player.jump();
            mc.player.velocity.y = 0.1;
        } else if (modeSetting.isSelected("FunTime New")) {
            handleFunTimeNew();
        }
    }

    private void handleFunTimeNew() {
        if (mc.player == null || mc.world == null) return;

        boolean inWater = mc.player.isTouchingWater();
        boolean inLavaNow = mc.player.isInLava();

        if (!inWater && (!inLavaNow || !inLava.isValue())) {
            waterTicks = 0;
            return;
        }

        waterTicks++;

        if (onlyFalling.isValue() && mc.player.getVelocity().y >= 0) {
            return;
        }

        // Since we don't have sub-modes, we'll use both functionalities
        handleFT();
        handleTest();
    }

    private void handleFT() {
        double maxFall = fallSpeed.getValue();

        if (mc.player.getVelocity().y < 0) {
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    Math.max(mc.player.getVelocity().y, -maxFall),
                    mc.player.getVelocity().z
            );
        }

        if (mc.player.getVelocity().y < -0.3) {
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    mc.player.getVelocity().y * 0.7,
                    mc.player.getVelocity().z
            );
        }
    }

    private void handleTest() {
        float speed = testSpeed.getValue();

        if (mc.player.getVelocity().y < 0) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
        }

        if (mc.player.getVelocity().y == 0 && !mc.player.isOnGround()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.001, mc.player.getVelocity().z);
        }

        mc.player.setVelocity(
                mc.player.getVelocity().x * speed,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z * speed
        );
    }

    
    @EventHandler

    public void onSwimming(SwimmingEvent e) {
        if (modeSetting.isSelected("FunTime")) {
            if (mc.options.jumpKey.isPressed()) {
                float pitch = TurnsConnection.INSTANCE.getRotation().getPitch();
                float boost = pitch >= 0 ? MathHelper.clamp(pitch / 45, 1, 2) : 0.8F;
                e.getVector().y = 0.5 * boost;
            } else if (mc.options.sneakKey.isPressed()) {
                e.getVector().y = -0.8;
            }
        }
    }
}
