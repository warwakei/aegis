package rich.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import rich.IMinecraft;
import rich.events.impl.PacketEvent;
import rich.events.impl.UsingItemEvent;
import rich.modules.impl.combat.TriggerBot;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.module.setting.implement.SelectSetting;

import java.util.List;

@Getter
public class StrikerConstructor implements IMinecraft {
    StrikeManager attackHandler = new StrikeManager();

    public StrikeManager getStrikeManager() {
        return attackHandler;
    }

    public void tick() {
        attackHandler.tick();
    }

    public void onPacket(PacketEvent e) {
        attackHandler.onPacket(e);
    }

    public void performAttack(AttackPerpetratorConfigurable configurable) {
        attackHandler.handleAttack(configurable);
    }

    public void performTriggerAttack(AttackPerpetratorConfigurable configurable, TriggerBot triggerBot) {
        attackHandler.handleTriggerAttack(configurable, triggerBot);
    }

    public void onUsingItem(UsingItemEvent e) {
        attackHandler.onUsingItem(e);
    }
    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class AttackPerpetratorConfigurable {
        LivingEntity target;
        Angle angle;
        float maximumRange;
        boolean onlyCritical, shouldBreakShield, shouldUnPressShield, eatAndAttack, multiPoints, ignoreWalls;
        Box box;
        SelectSetting aimMode;

        public AttackPerpetratorConfigurable(LivingEntity target, Angle angle, float maximumRange, List<String> options, SelectSetting aimMode, Box box) {
            this.target = target;
            this.angle = angle;
            this.maximumRange = maximumRange;
            this.onlyCritical = options.contains("Только криты") || options.contains("Only Critical") || options.contains("Crits with space");
            this.shouldBreakShield = options.contains("Break Shield");
            this.shouldUnPressShield = options.contains("UnPress Shield");
            this.multiPoints = options.contains("Multi Points");
            this.eatAndAttack = options.contains("No Attack When Eat");
            this.ignoreWalls = options.contains("Бить сквозь стены") || options.contains("Ignore The Walls");
            this.box = box;
            this.aimMode = aimMode;
        }
    }
}