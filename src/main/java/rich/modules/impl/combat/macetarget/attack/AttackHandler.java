package rich.modules.impl.combat.macetarget.attack;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import rich.util.inventory.InventoryUtils;

@Getter
@Setter
public class AttackHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean pendingAttack = false;
    private boolean shouldDisableAfterAttack = false;

    private long lastAttackTime = 0;
    private static final int MIN_ATTACK_DELAY_MS = 100;

    public void performAttack(LivingEntity target) {
        if (mc.player == null || target == null) return;

        // Проверка кулдауна атаки через StrikeManager
        if (!isAttackReady()) {
            return;
        }

        // Проверка умных критов
        if (!canCrit(target)) {
            return;
        }

        int maceSlot = InventoryUtils.findHotbarItem(Items.MACE);
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        // Свитч на булаву если нужно
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        }

        // Атака - отправляем пакет на сервер
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack((Entity) target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);

        // Обновляем время последней атаки
        lastAttackTime = System.currentTimeMillis();

        // Возвращаем слот
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }
    }
    
    /**
     * Проверка готовности атаки
     * Для булавы минимальный кулдаун для быстрой атаки в полёте
     */
    private boolean isAttackReady() {
        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        return timeSinceLastAttack >= MIN_ATTACK_DELAY_MS;
    }

    /**
     * Проверка умных критов для булавы
     * Возвращает true если можно атаковать
     * Булава должна атаковать когда летим вниз к врагу, но ещё не на земле
     */
    private boolean canCrit(LivingEntity target) {
        if (mc.player == null || target == null) return false;

        // Не атакуем если в воде/лаве
        if (mc.player.isTouchingWaterOrRain() || mc.player.isInLava()) {
            return true;
        }

        double playerY = mc.player.getY();
        double targetY = target.getY();
        double velocityY = mc.player.getVelocity().y;
        boolean onGround = mc.player.isOnGround();

        // Если уже на земле - атакуем
        if (onGround) {
            return true;
        }

        // Атакуем когда летим вниз (velocityY < 0) И мы уже ниже или на уровне цели
        // Это значит что мы падаем к врагу и скоро приземлимся
        if (velocityY < 0 && playerY <= targetY + 2.0) {
            return true;
        }

        // Если летим вверх - не атакуем, ждём падения
        if (velocityY > 0) {
            return false;
        }

        // Падаем но ещё высоко - ждём пока подлетим ближе к цели
        return false;
    }
    
    /**
     * Проверка держит ли игрок булаву
     */
    private boolean isHoldingMace() {
        if (mc.player == null) return false;
        var mainHand = mc.player.getMainHandStack();
        return mainHand.isOf(Items.MACE);
    }

    public void reset() {
        pendingAttack = false;
        shouldDisableAfterAttack = false;
        lastAttackTime = 0;
    }
}