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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

    // Для улучшенной проверки
    private int consecutiveMisses = 0;
    private int consecutiveHits = 0;
    private long lastValidHitTime = 0;
    private double lastTargetDistance = 0;

    // Адаптивная задержка на основе условий
    private int currentAdaptiveDelay = MIN_ATTACK_DELAY_MS;
    private static final int MIN_ADAPTIVE_DELAY = 30;
    private static final int MAX_ADAPTIVE_DELAY = 100;

    public void performAttack(LivingEntity target) {
        if (mc.player == null || target == null) return;

        // Проверка кулдауна атаки через StrikeManager
        if (!isAttackReady()) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, "Attack cooldown");
            return;
        }

        // Проверка умных критов с улучшенной логикой
        CritStatus critStatus = canCritEnhanced(target);
        if (!critStatus.canAttack) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, 
                    "Can't crit: " + critStatus.reason);
            return;
        }

        // Проверка видимости цели
        if (!isTargetVisible(target)) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, "Target not visible");
            return;
        }

        // Проверка дистанции с допуском на движение
        double distance = mc.player.distanceTo(target);
        if (distance > 6.5) { // 5.0 range + 1.5 tolerance for movement
            HitregLogger.logMaceAttack("Attack", target, distance, false, "Too far");
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
        
        // Валидация попадания
        validateHit(target);
        
        HitregLogger.logMaceAttack("Attack", target, distance, true, "MaceTarget attack sent (crit: " + critStatus.isCrit + ")");

        // Обновляем время последней атаки
        lastAttackTime = System.currentTimeMillis();

        // Возвращаем слот
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }

        // Адаптивная корректировка задержки
        updateAdaptiveDelay();
    }

    /**
     * Проверка готовности атаки с адаптивной задержкой
     */
    private boolean isAttackReady() {
        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        return timeSinceLastAttack >= currentAdaptiveDelay;
    }

    /**
     * Улучшенная проверка критов
     */
    private CritStatus canCritEnhanced(LivingEntity target) {
        if (mc.player == null) return new CritStatus(false, "No player");

        // На земле НЕ атакуем! Пусть MaceTarget взлетает заново
        if (mc.player.isOnGround()) {
            return new CritStatus(false, "On ground");
        }

        // Проверяем что мы падаем (основное условие для крита)
        double fallDistance = mc.player.fallDistance;
        double velocityY = mc.player.getVelocity().y;

        // Если мы восходим с достаточной скоростью - не атакуем
        if (velocityY > 0.3) {
            return new CritStatus(false, "Ascending too fast");
        }

        // Если падаем достаточно - точно будет крит
        if (fallDistance > 1.5 || velocityY < -0.1) {
            return new CritStatus(true, "Good crit", true);
        }

        // Если начали падение - можно атаковать
        if (velocityY <= 0.1 && velocityY > -0.1) {
            return new CritStatus(true, "Start of fall", true);
        }

        // Если восходим медленно - всё ещё можем атаковать (скоро начнёт падать)
        if (velocityY <= 0.3) {
            return new CritStatus(true, "Slow ascent, will fall soon", false);
        }

        return new CritStatus(false, "No crit conditions");
    }

    /**
     * Проверка видимости цели
     */
    private boolean isTargetVisible(LivingEntity target) {
        if (mc.world == null || mc.player == null) return false;

        Vec3d start = mc.player.getEyePos();
        Vec3d end = target.getEyePos();
        
        HitResult hitResult = mc.world.raycast(new RaycastContext(
            start, end, 
            RaycastContext.ShapeType.COLLIDER, 
            RaycastContext.FluidHandling.NONE, 
            mc.player
        ));

        // Если попали в блок - проверяем не слишком ли далеко он от цели
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            double distToTarget = blockHit.getPos().distanceTo(target.getEyePos());
            return distToTarget < 1.5; // Блок достаточно близко к цели
        }

        // Если попали в цель - отлично
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            return entityHit.getEntity() == target;
        }

        return true; // Нет препятствий
    }

    /**
     * Валидация попадания
     */
    private void validateHit(LivingEntity target) {
        // Проверяем что цель всё ещё рядом через короткое время
        double currentDistance = mc.player.distanceTo(target);
        
        // Если цель слишком далеко после атаки - возможно промах
        if (currentDistance > 8.0) {
            consecutiveMisses++;
            consecutiveHits = 0;
        } else {
            consecutiveHits++;
            consecutiveMisses = Math.max(0, consecutiveMisses - 1);
            lastValidHitTime = System.currentTimeMillis();
        }

        lastTargetDistance = currentDistance;
    }

    /**
     * Адаптивная корректировка задержки на основе успешности атак
     */
    private void updateAdaptiveDelay() {
        // Много промахов - увеличиваем задержку для точности
        if (consecutiveMisses >= 3) {
            currentAdaptiveDelay = Math.min(MAX_ADAPTIVE_DELAY, currentAdaptiveDelay + 10);
        }
        // Много попаданий - можно уменьшить задержку
        else if (consecutiveHits >= 5) {
            currentAdaptiveDelay = Math.max(MIN_ADAPTIVE_DELAY, currentAdaptiveDelay - 5);
        }
        // Нормальное состояние
        else {
            currentAdaptiveDelay = MIN_ATTACK_DELAY_MS;
        }
    }

    /**
     * Сброс статистики
     */
    public void resetStats() {
        consecutiveMisses = 0;
        consecutiveHits = 0;
        lastValidHitTime = 0;
        currentAdaptiveDelay = MIN_ATTACK_DELAY_MS;
    }

    public void reset() {
        pendingAttack = false;
        shouldDisableAfterAttack = false;
        lastAttackTime = 0;
        resetStats();
    }

    /**
     * Класс для хранения статуса крита
     */
    private static class CritStatus {
        final boolean canAttack;
        final String reason;
        final boolean isCrit;

        CritStatus(boolean canAttack, String reason) {
            this(canAttack, reason, false);
        }

        CritStatus(boolean canAttack, String reason, boolean isCrit) {
            this.canAttack = canAttack;
            this.reason = reason;
            this.isCrit = isCrit;
        }
    }
}
