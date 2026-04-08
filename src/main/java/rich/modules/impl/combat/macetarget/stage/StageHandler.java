package rich.modules.impl.combat.macetarget.stage;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import rich.modules.impl.combat.macetarget.armor.ArmorSwapHandler;
import rich.modules.impl.combat.macetarget.armor.FireworkHandler;
import rich.modules.impl.combat.macetarget.attack.AttackHandler;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;
import rich.util.inventory.InventoryUtils;
import rich.util.timer.StopWatch;

@Getter
public class StageHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final ArmorSwapHandler armorSwapHandler;
    private final FireworkHandler fireworkHandler;
    private final AttackHandler attackHandler;
    private final StopWatch fireworkTimer;

    @Setter
    private Stage stage = Stage.PREPARE;
    @Setter
    private boolean silentMode = true;
    @Setter
    private boolean reallyWorldMode = false;
    @Setter
    private float height = 30.0f;
    @Setter
    private float swapDistance = 12.0f;
    @Setter
    private float attackRange = 5.0f;

    public StageHandler(ArmorSwapHandler armorSwapHandler, FireworkHandler fireworkHandler, 
                        AttackHandler attackHandler, StopWatch fireworkTimer) {
        this.armorSwapHandler = armorSwapHandler;
        this.fireworkHandler = fireworkHandler;
        this.attackHandler = attackHandler;
        this.fireworkTimer = fireworkTimer;
    }

    public void handlePrepare(boolean hasElytra) {
        if (!hasElytra) {
            int slot = InventoryUtils.findElytraSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, silentMode);
            }
            return;
        }
        stage = Stage.FLYING_UP;
        fireworkTimer.reset();
    }

    public void handleFlyingUp(LivingEntity target, boolean hasElytra) {
        if (!hasElytra) {
            stage = Stage.PREPARE;
            return;
        }

        if (mc.player.isGliding() && fireworkTimer.finished(270)) {
            fireworkHandler.useFirework(silentMode);
            fireworkTimer.reset();
        }

        if (mc.player.getY() - target.getY() >= height) {
            stage = Stage.TARGETTING;
        }
    }

    public void handleTargetting(LivingEntity target) {
        double distance = mc.player.distanceTo(target);

        // Динамическая дистанция свапа на основе скорости цели
        double targetVelocity = Math.abs(target.getVelocity().y) + target.getVelocity().horizontalLength();
        double dynamicSwapDistance = swapDistance + (targetVelocity * 1.5);

        // Свап элитры на нагрудник
        if (InventoryUtils.hasElytra() && distance < dynamicSwapDistance && !armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, silentMode);
            }
        }

        // Переход к атаке сразу как свап завершён и мы близко к цели
        if (!armorSwapHandler.isActive() && distance < attackRange) {
            stage = Stage.ATTACKING;
        }
    }

    public void handleAttacking(LivingEntity target, boolean hasElytra) {
        double distance = mc.player.distanceTo(target);

        // Атакуем сразу как свап завершён и мы в радиусе атаки
        if (!hasElytra && !armorSwapHandler.isActive() && distance < attackRange) {
            if (!attackHandler.isPendingAttack()) {
                attackHandler.setPendingAttack(true);
            }
            // После атаки — СРАЗУ свапаемся на элитру и летим за врагом
            if (attackHandler.getLastAttackTime() > 0) {
                if (reallyWorldMode) {
                    attackHandler.setShouldDisableAfterAttack(true);
                } else {
                    // Всегда запускаем свап на элитру после удара
                    int elytraSlot = InventoryUtils.findElytraSlot();
                    if (elytraSlot != -1) {
                        armorSwapHandler.startSwap(elytraSlot, silentMode);
                    }
                    stage = Stage.FLYING_UP;
                    fireworkTimer.reset();
                }
                attackHandler.setLastAttackTime(0);
            }
        }
    }

    public void reset() {
        stage = Stage.PREPARE;
    }
}