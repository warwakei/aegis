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

    // для чеков получше
    private int consecutiveMisses = 0;
    private int consecutiveHits = 0;
    private long lastValidHitTime = 0;
    private double lastTargetDistance = 0;

    // задержка адаптив по ситуации
    private int currentAdaptiveDelay = MIN_ATTACK_DELAY_MS;
    private static final int MIN_ADAPTIVE_DELAY = 30;
    private static final int MAX_ADAPTIVE_DELAY = 100;

    public void performAttack(LivingEntity target) {
        if (mc.player == null || target == null) return;

        String whileCond = HitregLogger.buildWhileCondition(mc.player);

        // чек кд через страйк
        if (!isAttackReady()) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, "Attack cooldown", whileCond);
            return;
        }

        // чек умные криты
        CritStatus critStatus = canCritEnhanced(target);
        if (!critStatus.canAttack) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false,
                    "Can't crit: " + critStatus.reason, whileCond);
            return;
        }

        // чек видно ли
        if (!isTargetVisible(target)) {
            HitregLogger.logMaceAttack("Attack", target, mc.player.distanceTo(target), false, "Target not visible", whileCond);
            return;
        }

        // чек дистан с запасом
        double distance = mc.player.distanceTo(target);
        if (distance > 6.5) { // 5 ренж плюс 1.5 запас
            HitregLogger.logMaceAttack("Attack", target, distance, false, "Too far", whileCond);
            return;
        }

        int maceSlot = InventoryUtils.findHotbarItem(Items.MACE);
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        // свитч на булаву надо ли
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        }

        // бьем шлем пакет
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack((Entity) target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
        
        // чек попало ли
        validateHit(target);
        
        HitregLogger.logMaceAttack("Attack", target, distance, true, "MaceTarget attack sent (crit: " + critStatus.isCrit + ")", whileCond);

        // время удара обновили
        lastAttackTime = System.currentTimeMillis();

        // слот назад
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }

        // задержка адаптив правим
        updateAdaptiveDelay();
    }

    /**
     * готов ли бить адаптив
     */
    private boolean isAttackReady() {
        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        return timeSinceLastAttack >= currentAdaptiveDelay;
    }

    /**
     * криты улучшеные не палимся
     */
    private CritStatus canCritEnhanced(LivingEntity target) {
        if (mc.player == null) return new CritStatus(false, "No player");

        // на земле не бьем летим заново
        if (mc.player.isOnGround()) {
            return new CritStatus(false, "On ground");
        }

        double fallDistance = mc.player.fallDistance;
        double velocityY = mc.player.getVelocity().y;

        // вверх быстро ждем
        if (velocityY > 0.15) {
            return new CritStatus(false, "Ascending");
        }

        // годный крит падаем норм
        if (fallDistance > 1.0 && velocityY < -0.05) {
            return new CritStatus(true, "Good crit", true);
        }

        // начало падения норм
        if (fallDistance > 0.3 && velocityY <= 0.05 && velocityY > -0.05) {
            return new CritStatus(true, "Start of fall", true);
        }

        // мелко падаем тоже можно
        if (fallDistance > 0.5 && velocityY < 0) {
            return new CritStatus(true, "Small crit", true);
        }

        // иначе молчим чтоб не палить
        return new CritStatus(false, "No crit conditions");
    }

    /**
     * видно ли цель
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

        // блок рядом с целью норм
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            double distToTarget = blockHit.getPos().distanceTo(target.getEyePos());
            return distToTarget < 1.5; // блок близко норм
        }

        // в цель попали четко
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            return entityHit.getEntity() == target;
        }

        return true; // пусто можно бить
    }

    /**
     * чек попало ли
     */
    private void validateHit(LivingEntity target) {
        // чек цель рядом после удара
        double currentDistance = mc.player.distanceTo(target);
        
        // далеко после удара мисс
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
     * задержка по миссам хитам
     */
    private void updateAdaptiveDelay() {
        // миссы задержку больше
        if (consecutiveMisses >= 3) {
            currentAdaptiveDelay = Math.min(MAX_ADAPTIVE_DELAY, currentAdaptiveDelay + 10);
        }
        // хиты задержку меньше
        else if (consecutiveHits >= 5) {
            currentAdaptiveDelay = Math.max(MIN_ADAPTIVE_DELAY, currentAdaptiveDelay - 5);
        }
        // норм состояние
        else {
            currentAdaptiveDelay = MIN_ATTACK_DELAY_MS;
        }
    }

    /**
     * стата сброс
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
     * статус крита хранилка
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
