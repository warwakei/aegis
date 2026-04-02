package rich.modules.impl.combat.macetarget.attack;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.util.inventory.InventoryUtils;

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

        // Атака
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        // Обновляем время последней атаки
        lastAttackTime = System.currentTimeMillis();

        // Возвращаем слот
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }
    }
    
    /**
     * Проверка готовности атаки через StrikeManager
     */
    private boolean isAttackReady() {
        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        
        // Минимальная задержка между атаками
        if (timeSinceLastAttack < MIN_ATTACK_DELAY_MS) {
            return false;
        }
        
        // Проверяем через StrikeManager (учитывает attack speed предмета)
        // Для булавы: 1650мс, для меча: ~600мс, для топора: ~1250мс
        return timeSinceLastAttack >= getCurrentWeaponCooldown();
    }
    
    /**
     * Возвращает кулдаун текущего оружия в мс
     */
    private long getCurrentWeaponCooldown() {
        if (mc.player == null) return 600;
        
        var stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return 600;
        
        // Булава - 1650мс (33 тика)
        if (stack.isOf(Items.MACE)) {
            return 1650;
        }
        
        // Для других предметов используем дефолтные значения
        String itemId = stack.getItem().getTranslationKey().toLowerCase();
        if (itemId.contains("sword")) return 600;    // 12 тиков
        if (itemId.contains("axe")) return 1250;     // 25 тиков
        if (itemId.contains("trident")) return 900;  // 18 тиков
        
        return 600; // Дефолт
    }
    
    /**
     * Проверка умных критов
     * Возвращает true если можно атаковать (не в воде, не на земле, падаем)
     */
    private boolean canCrit(LivingEntity target) {
        if (mc.player == null) return false;
        
        // Не атакуем если в воде/лаве
        if (mc.player.isTouchingWaterOrRain() || mc.player.isInLava()) {
            return true; // Всё равно атакуем, критов не будет
        }
        
        // Проверка: игрок не на земле и падает (для крита)
        boolean onGround = mc.player.isOnGround();
        double velocityY = mc.player.getVelocity().y;
        double fallDistance = mc.player.fallDistance;
        
        // Если на земле - не крит, пропускаем атаку
        if (onGround) {
            return false;
        }
        
        // Если летим вверх - ждём
        if (velocityY > 0.0) {
            return false;
        }
        
        // Падаем вниз - можно атаковать
        return fallDistance > 0.0;
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