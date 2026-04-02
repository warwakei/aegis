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
    private final StopWatch swapTimer = new StopWatch();

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
                swapTimer.reset();
            }
        }

        // Быстрый переход к атаке - минимальная задержка 50мс
        if (!armorSwapHandler.isActive() && swapTimer.finished(50)) {
            // Переход к атаке сразу если мы близко к цели
            if (distance < attackRange) {
                stage = Stage.ATTACKING;
                swapTimer.reset();
            }
        }
    }

    public void handleAttacking(LivingEntity target, boolean hasElytra) {
        double distance = mc.player.distanceTo(target);

        if (hasElytra && !armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, silentMode);
                swapTimer.reset();
            }
            return;
        }

        if (!hasElytra && !armorSwapHandler.isActive() && distance < attackRange) {
            // Минимальная задержка перед атакой - 50мс для быстрой реакции
            if (swapTimer.finished(50)) {
                // Устанавливаем атаку сразу
                if (!attackHandler.isPendingAttack()) {
                    attackHandler.setPendingAttack(true);
                }
                // Стадию меняем сразу после атаки
                if (attackHandler.getLastAttackTime() > 0) {
                    if (reallyWorldMode) {
                        attackHandler.setShouldDisableAfterAttack(true);
                    } else {
                        stage = Stage.FLYING_UP;
                        fireworkTimer.reset();
                    }
                    attackHandler.setLastAttackTime(0);
                }
            }
        }
    }

    public void reset() {
        stage = Stage.PREPARE;
        swapTimer.reset();
    }
}