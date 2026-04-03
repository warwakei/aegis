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
import rich.netpanel.loggers.HitregLogger;

@Getter
@Setter
public class AttackHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean pendingAttack = false;
    private boolean shouldDisableAfterAttack = false;

    private long lastAttackTime = 0;
    private static final int MIN_ATTACK_DELAY_MS = 50;

    public void performAttack(LivingEntity target) {
        if (mc.player == null || target == null) return;

        // Проверка кулдауна атаки через StrikeManager
        if (!isAttackReady()) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, "Attack cooldown");
            return;
        }

        // Проверка умных критов
        if (!canCrit(target)) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, "Can't crit (onGround or flying up)");
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
        HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), true, "MaceTarget attack sent");

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
     * Атакуем ТОЛЬКО когда не на земле
     * На земле НЕ атакуем - взлетаем заново
     * 
     * ВАЖНО: Когда падаем на врага — ВСЕГДА атакуем, даже если velocityY ещё положительный.
     * Это позволяет наносить удар в момент падения на цель.
     */
    private boolean canCrit(LivingEntity target) {
        if (mc.player == null) return false;

        // На земле НЕ атакуем! Пусть MaceTarget взлетает заново
        if (mc.player.isOnGround()) {
            return false;
        }

        // ВАЖНО: Убрали проверку velocityY > 0.15
        // Раньше она блокировала атаку когда velocityY был слегка положительным,
        // что мешало атаковать в момент начала падения на врага.
        // Теперь мы атакуем всегда когда не на земле — это гарантирует удар
        // даже если игрок ещё немного поднимается перед падением.
        
        return true;
    }
    
    public void reset() {
        pendingAttack = false;
        shouldDisableAfterAttack = false;
        lastAttackTime = 0;
    }
}