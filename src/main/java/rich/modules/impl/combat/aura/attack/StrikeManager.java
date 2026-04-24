package rich.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;
import rich.events.api.types.EventType;
import rich.events.impl.PacketEvent;
import rich.events.impl.UsingItemEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.TriggerBot;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.movement.ElytraTarget;
import rich.util.player.PlayerSimulation;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;
import rich.netpanel.loggers.HitregLogger;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StrikeManager implements IMinecraft {
    private final Pressing clickScheduler = new Pressing();
    private final StopWatch attackTimer = new StopWatch();
    private final StopWatch shieldWatch = new StopWatch();
    private final CPSClickScheduler cpsScheduler = new CPSClickScheduler();

    private int count = 0;
    private int ticksOnBlock = 0;

    // ÐšÑƒÐ»Ð´Ð°ÑƒÐ½ Ð¿Ð¾ÑÐ»Ðµ Ð¿Ð¾ÑÐ»ÐµÐ´Ð½ÐµÐ¹ Ð°Ñ‚Ð°ÐºÐ¸ (Ð² Ð¼Ð¸Ð»Ð»Ð¸ÑÐµÐºÑƒÐ½Ð´Ð°Ñ…)
    private long lastAttackTime = 0;
    
    // Ð”Ð»Ñ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ð°
    private boolean is1_8Mode = false;
    private int clicksSentThisTick = 0;
    
    // ÐžÑ‚Ð»Ð¾Ð¶ÐµÐ½Ð½Ð°Ñ Ð°Ñ‚Ð°ÐºÐ° Ð´Ð»Ñ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ð°
    private long pendingAttackTime = 0;
    private Runnable pendingAttack = null;

    // Ð¡Ñ‚Ð°Ñ‚Ð¸ÑÑ‚Ð¸ÐºÐ° Ð¿Ð¾Ð¿Ð°Ð´Ð°Ð½Ð¸Ð¹/Ð¿Ñ€Ð¾Ð¼Ð°Ñ…Ð¾Ð² Ð´Ð»Ñ Ð°Ð´Ð°Ð¿Ñ‚Ð¸Ð²Ð½Ð¾Ð³Ð¾ CPS
    private int consecutiveMisses = 0;
    private int consecutiveHits = 0;
    private long lastHitValidationTime = 0;

    // Ð¡ÑÑ‹Ð»ÐºÐ° Ð½Ð° CPS Ð½Ð°ÑÑ‚Ñ€Ð¾Ð¹ÐºÑƒ Ð¸Ð· Aura
    private rich.modules.module.setting.implement.SliderSettings cpsSetting;

    void tick() {
        if (mc.player != null && mc.player.isOnGround()) {
            ticksOnBlock++;
        } else {
            ticksOnBlock = 0;
        }
        
        // Ð¡Ð±Ñ€Ð¾Ñ ÑÑ‡Ñ‘Ñ‚Ñ‡Ð¸ÐºÐ° ÐºÐ»Ð¸ÐºÐ¾Ð² Ð·Ð° Ñ‚Ð¸Ðº
        clicksSentThisTick = 0;
        
        // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ð¾Ñ‚Ð»Ð¾Ð¶ÐµÐ½Ð½ÑƒÑŽ Ð°Ñ‚Ð°ÐºÑƒ
        if (pendingAttack != null && System.currentTimeMillis() >= pendingAttackTime) {
            pendingAttack.run();
            pendingAttack = null;
            pendingAttackTime = 0L;
        }
    }
    
    /**
     * Ð£ÑÑ‚Ð°Ð½Ð°Ð²Ð»Ð¸Ð²Ð°ÐµÑ‚ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼
     */
    public void set1_8Mode(boolean enabled) {
        this.is1_8Mode = enabled;
        if (!enabled) {
            cpsScheduler.reset();
        }
    }
    
    /**
     * ÐžÐ±Ð½Ð¾Ð²Ð»ÑÐµÑ‚ CPS Ð¸Ð· Ð½Ð°ÑÑ‚Ñ€Ð¾ÐµÐº
     */
    public void updateCPS(int cps, rich.modules.module.setting.implement.SliderSettings setting) {
        cpsScheduler.updateCPS(cps);
        this.cpsSetting = setting;
    }

    /**
     * ÐžÐ±Ð½Ð¾Ð²Ð»ÑÐµÑ‚ CPS Ð¸Ð· Ð½Ð°ÑÑ‚Ñ€Ð¾ÐµÐº (Ð¾Ð±Ñ€Ð°Ñ‚Ð½Ð°Ñ ÑÐ¾Ð²Ð¼ÐµÑÑ‚Ð¸Ð¼Ð¾ÑÑ‚ÑŒ)
     */
    public void updateCPS(int cps) {
        cpsScheduler.updateCPS(cps);
    }
    
    /**
     * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ Ð¼Ð¾Ð¶Ð½Ð¾ Ð»Ð¸ Ð°Ñ‚Ð°ÐºÐ¾Ð²Ð°Ñ‚ÑŒ Ð² 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ðµ
     * @return true ÐµÑÐ»Ð¸ Ð¼Ð¾Ð¶Ð½Ð¾ Ð½Ð°Ñ‡Ð°Ñ‚ÑŒ Ð°Ñ‚Ð°ÐºÑƒ
     */
    public boolean canAttack1_8() {
        if (!is1_8Mode) {
            return false;
        }
        
        // Ð•ÑÐ»Ð¸ Ð¾Ñ‡ÐµÑ€ÐµÐ´ÑŒ Ð½Ðµ Ð°ÐºÑ‚Ð¸Ð²Ð½Ð° - Ð½Ð°Ñ‡Ð¸Ð½Ð°ÐµÐ¼ Ð½Ð¾Ð²ÑƒÑŽ
        if (!cpsScheduler.isQueueActive()) {
            cpsScheduler.startQueue(cpsScheduler.getCps());
        }
        
        // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ð¼Ð¾Ð¶Ð½Ð¾ Ð»Ð¸ ÑÐ´ÐµÐ»Ð°Ñ‚ÑŒ ÐºÐ»Ð¸Ðº
        return cpsScheduler.shouldClick();
    }
    
    /**
     * Ð’Ñ‹Ð¿Ð¾Ð»Ð½ÑÐµÑ‚ Ð°Ñ‚Ð°ÐºÑƒ Ð² 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ðµ Ñ Ð¾Ð±Ñ…Ð¾Ð´Ð¾Ð¼ Ñ‡ÐµÑ€ÐµÐ· Ð±Ñ‹ÑÑ‚Ñ€Ñ‹Ðµ Ð¿Ð°Ñ€Ñ‹ ÐºÐ»Ð¸ÐºÐ¾Ð²
     * ÐŸÐµÑ€Ð²Ñ‹Ð¹ ÐºÐ»Ð¸Ðº Ð¾Ð±Ñ‹Ñ‡Ð½Ñ‹Ð¹, Ð²Ñ‚Ð¾Ñ€Ð¾Ð¹ Ñ Ð·Ð°Ð´ÐµÑ€Ð¶ÐºÐ¾Ð¹ 15-35Ð¼Ñ
     * @param attackExecutor Ð»ÑÐ¼Ð±Ð´Ð° Ð´Ð»Ñ Ð²Ñ‹Ð¿Ð¾Ð»Ð½ÐµÐ½Ð¸Ñ Ð°Ñ‚Ð°ÐºÐ¸
     * @return ÐºÐ¾Ð»Ð¸Ñ‡ÐµÑÑ‚Ð²Ð¾ ÐºÐ»Ð¸ÐºÐ¾Ð² ÑÐ´ÐµÐ»Ð°Ð½Ð½Ñ‹Ñ… (1 Ð¸Ð»Ð¸ 2)
     */
    public int performAttack1_8(Runnable attackExecutor) {
        if (!is1_8Mode || !cpsScheduler.isQueueActive()) {
            attackExecutor.run();
            return 1;
        }
        
        // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ð¼Ð¾Ð¶Ð½Ð¾ Ð»Ð¸ ÑÐ´ÐµÐ»Ð°Ñ‚ÑŒ Ð±Ñ‹ÑÑ‚Ñ€Ñ‹Ð¹ Ð²Ñ‚Ð¾Ñ€Ð¾Ð¹ ÐºÐ»Ð¸Ðº
        if (cpsScheduler.shouldDoFastSecondClick()) {
            // Ð”ÐµÐ»Ð°ÐµÐ¼ Ð¿ÐµÑ€Ð²Ñ‹Ð¹ ÐºÐ»Ð¸Ðº
            attackExecutor.run();
            
            // ÐŸÐ»Ð°Ð½Ð¸Ñ€ÑƒÐµÐ¼ Ð²Ñ‚Ð¾Ñ€Ð¾Ð¹ ÐºÐ»Ð¸Ðº Ñ Ð·Ð°Ð´ÐµÑ€Ð¶ÐºÐ¾Ð¹ 15-35Ð¼Ñ
            int delay = cpsScheduler.getSecondClickDelay();
            cpsScheduler.useFastClick();
            cpsScheduler.registerClick(false); // Ð ÐµÐ³Ð¸ÑÑ‚Ñ€Ð¸Ñ€ÑƒÐµÐ¼ Ð¿ÐµÑ€Ð²Ñ‹Ð¹ ÐºÐ»Ð¸Ðº
            
            // Ð¡Ð¾Ñ…Ñ€Ð°Ð½ÑÐµÐ¼ Ð¾Ñ‚Ð»Ð¾Ð¶ÐµÐ½Ð½ÑƒÑŽ Ð°Ñ‚Ð°ÐºÑƒ
            pendingAttackTime = System.currentTimeMillis() + delay;
            pendingAttack = () -> {
                attackExecutor.run();
                cpsScheduler.registerClick(true); // Ð ÐµÐ³Ð¸ÑÑ‚Ñ€Ð¸Ñ€ÑƒÐµÐ¼ Ð²Ñ‚Ð¾Ñ€Ð¾Ð¹ ÐºÐ»Ð¸Ðº
            };
            
            return 1; // Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÐ¼ 1, Ð²Ñ‚Ð¾Ñ€Ð¾Ð¹ ÐºÐ»Ð¸Ðº Ð±ÑƒÐ´ÐµÑ‚ Ð¾Ñ‚Ð´ÐµÐ»ÑŒÐ½Ð¾ Ð² tick()
        }
        
        // ÐžÐ±Ñ‹Ñ‡Ð½Ñ‹Ð¹ ÐºÐ»Ð¸Ðº
        attackExecutor.run();
        cpsScheduler.registerClick(false);
        
        return 1;
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
    }

    public void resetPendingState() {
        pendingAttack = null;
        pendingAttackTime = 0L;
        clicksSentThisTick = 0;
        cpsScheduler.reset();
    }
    
    /**
     * ÐžÐ±Ð½Ð¾Ð²Ð»ÑÐµÑ‚ Ð²Ñ€ÐµÐ¼Ñ Ð¿Ð¾ÑÐ»ÐµÐ´Ð½ÐµÐ¹ Ð°Ñ‚Ð°ÐºÐ¸
     */
    public void updateLastAttackTime() {
        lastAttackTime = System.currentTimeMillis();
        // Ð”Ð»Ñ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ð° Ð½Ðµ Ð¾Ð±Ð½Ð¾Ð²Ð»ÑÐµÐ¼ clickScheduler - Ñ‚Ð°Ð¼ ÑÐ²Ð¾Ñ Ð»Ð¾Ð³Ð¸ÐºÐ° CPS
        if (!is1_8Mode) {
            clickScheduler.recalculate();
        }

        // ÐžÐ±Ð½Ð¾Ð²Ð»ÑÐµÐ¼ ÑÑ‡Ñ‘Ñ‚Ñ‡Ð¸Ðº Ð´Ð»Ñ ElytraTarget double sneak
        updateElytraTargetTradeHit();
    }

    private void updateElytraTargetTradeHit() {
        if (Aura.target != null && Aura.target.isAlive()) {
            rich.modules.impl.movement.ElytraTarget elytraTarget = rich.modules.impl.movement.ElytraTarget.getInstance();
            if (elytraTarget != null && elytraTarget.isState() && elytraTarget.doubleSneak.isValue()) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastHit = currentTime - elytraTarget.lastTradeHitTime;
                if (timeSinceLastHit < 800) {
                    elytraTarget.consecutiveTradeHits++;
                } else {
                    elytraTarget.consecutiveTradeHits = 1;
                }
                elytraTarget.lastTradeHitTime = currentTime;
            }
        }
    }
    
    /**
     * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ Ð³Ð¾Ñ‚Ð¾Ð²Ð½Ð¾ÑÑ‚ÑŒ Ð°Ñ‚Ð°ÐºÐ¸ Ð½Ð° Ð¾ÑÐ½Ð¾Ð²Ðµ attack speed Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð°
     * @return true ÐµÑÐ»Ð¸ Ð¼Ð¾Ð¶Ð½Ð¾ Ð°Ñ‚Ð°ÐºÐ¾Ð²Ð°Ñ‚ÑŒ
     */
    public boolean isAttackReady() {
        if (mc.player == null) return false;
        
        float attackSpeed = getWeaponAttackSpeed();
        long cooldownMs = getCooldownMillis(attackSpeed);
        
        return System.currentTimeMillis() - lastAttackTime >= cooldownMs;
    }
    
    /**
     * Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÑ‚ Ð¾ÑÑ‚Ð°Ð²ÑˆÐµÐµÑÑ Ð²Ñ€ÐµÐ¼Ñ ÐºÑƒÐ»Ð´Ð°ÑƒÐ½Ð° Ð² Ð¼Ð¸Ð»Ð»Ð¸ÑÐµÐºÑƒÐ½Ð´Ð°Ñ…
     * @return Ð²Ñ€ÐµÐ¼Ñ Ð² Ð¼Ñ, 0 ÐµÑÐ»Ð¸ ÐºÑƒÐ»Ð´Ð°ÑƒÐ½ Ð¿Ñ€Ð¾ÑˆÑ‘Ð»
     */
    public long getRemainingCooldownMillis() {
        if (mc.player == null) return 0;
        
        float attackSpeed = getWeaponAttackSpeed();
        long cooldownMs = getCooldownMillis(attackSpeed);
        long elapsed = System.currentTimeMillis() - lastAttackTime;
        
        return Math.max(0, cooldownMs - elapsed);
    }
    
    /**
     * Ð‘Ñ‹ÑÑ‚Ñ€Ð°Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ°: Ð¼Ð¾Ð¶Ð½Ð¾ Ð»Ð¸ Ð°Ñ‚Ð°ÐºÐ¾Ð²Ð°Ñ‚ÑŒ Ð¿Ñ€ÑÐ¼Ð¾ ÑÐµÐ¹Ñ‡Ð°Ñ
     * Ð˜ÑÐ¿Ð¾Ð»ÑŒÐ·ÑƒÐµÑ‚ÑÑ Ð´Ð»Ñ ÑÐ¿Ð°Ð¼Ð° Ð·Ð°Ð¿Ñ€Ð¾ÑÐ°Ð¼Ð¸ Ð¸Ð· Ð°ÑƒÑ€Ñ‹
     * @return true ÐµÑÐ»Ð¸ ÐºÑƒÐ»Ð´Ð°ÑƒÐ½ Ð¿Ñ€Ð¾ÑˆÑ‘Ð»
     */
    public boolean canAttackNow() {
        return isAttackReady();
    }
    
    /**
     * Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÑ‚ attack speed Ñ‚ÐµÐºÑƒÑ‰ÐµÐ³Ð¾ Ð¾Ñ€ÑƒÐ¶Ð¸Ñ
     */
    private float getWeaponAttackSpeed() {
        if (mc.player == null) return 4.0f;
        
        var stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return 4.0f;
        
        Item item = stack.getItem();
        
        // Ð‘ÑƒÐ»Ð°Ð²Ð° - 0.6 (33 Ñ‚Ð¸ÐºÐ°)
        if (item == Items.MACE) return 0.6f;
        
        // Ð”ÐµÑ„Ð¾Ð»Ñ‚Ð½Ñ‹Ðµ Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ñ Ð´Ð»Ñ Ð¸Ð·Ð²ÐµÑÑ‚Ð½Ñ‹Ñ… Ð¾Ñ€ÑƒÐ¶Ð¸Ð¹
        String itemId = item.getTranslationKey().toLowerCase();
        if (itemId.contains("sword")) return 1.6f;      // 12 Ñ‚Ð¸ÐºÐ¾Ð²
        if (itemId.contains("axe")) return 0.8f;        // 25 Ñ‚Ð¸ÐºÐ¾Ð²  
        if (itemId.contains("trident")) return 1.1f;    // 18 Ñ‚Ð¸ÐºÐ¾Ð²
        if (itemId.contains("shovel")) return 1.0f;     // 20 Ñ‚Ð¸ÐºÐ¾Ð²
        if (itemId.contains("pickaxe")) return 1.2f;    // 16 Ñ‚Ð¸ÐºÐ¾Ð²
        if (itemId.contains("hoe")) return 1.0f;        // 20 Ñ‚Ð¸ÐºÐ¾Ð²
        
        return 4.0f; // Ð‘ÐµÐ· Ð¾Ñ€ÑƒÐ¶Ð¸Ñ / Ð¾Ð±Ñ‹Ñ‡Ð½Ñ‹Ð¹ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚
    }
    
    /**
     * ÐšÐ¾Ð½Ð²ÐµÑ€Ñ‚Ð¸Ñ€ÑƒÐµÑ‚ attack speed Ð² ÐºÑƒÐ»Ð´Ð°ÑƒÐ½ Ð² Ð¼Ð¸Ð»Ð»Ð¸ÑÐµÐºÑƒÐ½Ð´Ð°Ñ…
     * Ð¤Ð¾Ñ€Ð¼ÑƒÐ»Ð°: cooldownTicks = 20 / attackSpeed
     * 1 Ñ‚Ð¸Ðº = 50Ð¼Ñ
     */
    private long getCooldownMillis(float attackSpeed) {
        if (attackSpeed <= 0) attackSpeed = 0.01f;
        
        // Ð”Ð»Ñ Ð±ÑƒÐ»Ð°Ð²Ñ‹ Ð¾ÑÐ¾Ð±Ñ‹Ð¹ ÑÐ»ÑƒÑ‡Ð°Ð¹ - 33 Ñ‚Ð¸ÐºÐ°
        if (attackSpeed == 0.6f) {
            return 1650; // 33 Ñ‚Ð¸ÐºÐ° * 50Ð¼Ñ
        }
        
        double cooldownTicks = 20.0 / attackSpeed;
        long roundedTicks = Math.round(cooldownTicks);
        
        return roundedTicks * 50L; // 1 Ñ‚Ð¸Ðº = 50Ð¼Ñ
    }

    private boolean hasAnyMovementInput() {
        if (mc.player == null)
            return false;
        return mc.player.input.playerInput.forward() ||
                mc.player.input.playerInput.backward() ||
                mc.player.input.playerInput.left() ||
                mc.player.input.playerInput.right();
    }

    private boolean isHoldingMace() {
        return clickScheduler.isHoldingMace();
    }

    private boolean isPlayerEating() {
        if (mc.player == null)
            return false;
        if (!mc.player.isUsingItem())
            return false;
        var activeItem = mc.player.getActiveItem();
        if (activeItem.isEmpty())
            return false;
        var useAction = activeItem.getUseAction();
        return useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }

    private boolean shouldWaitForEating() {
        Aura aura = Aura.getInstance();
        return aura.options.isSelected("ÐÐµ Ð±Ð¸Ñ‚ÑŒ ÐµÑÐ»Ð¸ ÐµÑˆÑŒ") && isPlayerEating();
    }

    private boolean isInWater() {
        return mc.player != null
                && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isSwimming());
    }

    private boolean hasLowCeiling() {
        if (mc.player == null || mc.world == null)
            return false;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos above1 = playerPos.up(2);
        BlockPos above2 = playerPos.up(3);

        BlockState state1 = mc.world.getBlockState(above1);
        BlockState state2 = mc.world.getBlockState(above2);

        boolean blocked1 = !state1.isAir() && !state1.getCollisionShape(mc.world, above1).isEmpty();
        boolean blocked2 = !state2.isAir() && !state2.getCollisionShape(mc.world, above2).isEmpty();

        return blocked1 || blocked2;
    }

    private boolean isPerfectCrit() {
        if (mc.player == null)
            return false;

        return mc.player.fallDistance > 0.0F
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasVehicle()
                && !mc.player.getAbilities().flying;
    }

    private boolean isAscending() {
        if (mc.player == null)
            return false;
        return !mc.player.isOnGround() && mc.player.getVelocity().y > 0.0;
    }

    private boolean isDescending() {
        if (mc.player == null)
            return false;
        return !mc.player.isOnGround() && mc.player.getVelocity().y <= 0.0;
    }

    private boolean willBeCritInTicks(int ticks) {
        if (ticks == 0) {
            return isPerfectCrit();
        }

        PlayerSimulation sim = PlayerSimulation.simulateLocalPlayer(ticks);

        return sim.fallDistance > 0.0F
                && !sim.onGround
                && sim.velocity.y <= 0.0
                && !sim.isClimbing()
                && !sim.player.isTouchingWater()
                && !sim.hasStatusEffect(StatusEffects.BLINDNESS)
                && !sim.player.hasVehicle()
                && !sim.player.getAbilities().flying;
    }

    private boolean hasMovementRestrictions() {
        if (mc.player == null)
            return true;

        if (isInWater())
            return false;
        if (hasLowCeiling())
            return true;
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS))
            return true;
        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION))
            return true;
        if (PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), Blocks.COBWEB))
            return true;
        if (mc.player.isInLava())
            return true;
        if (mc.player.isClimbing())
            return true;
        if (!PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, mc.player.getEntityPos()))
            return true;
        if (mc.player.getAbilities().flying)
            return true;

        return false;
    }

    private boolean shouldResetSprintForCrit() {
        if (mc.player == null)
            return false;

        if (isInWater())
            return false;
        if (mc.player.isGliding())
            return false;

        return mc.player.isSprinting();
    }

    private boolean canCritNow() {
        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();
        boolean smartCritsEnabled = aura.getSmartCrits().isValue();

        // ElytraTarget Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ÑÑ Ð¾Ñ‚Ð´ÐµÐ»ÑŒÐ½Ð¾ Ð² handleAttack, Ð·Ð´ÐµÑÑŒ Ð¿Ñ€Ð¾Ð¿ÑƒÑÐºÐ°ÐµÐ¼
        if (isElytraTargetMode()) {
            return true;
        }

        // Ð’ Ð²Ð¾Ð´Ðµ Ð¸Ð»Ð¸ Ñ Ð¾Ð³Ñ€Ð°Ð½Ð¸Ñ‡ÐµÐ½Ð¸ÑÐ¼Ð¸ â€” Ð²ÑÐµÐ³Ð´Ð° Ñ€Ð°Ð·Ñ€ÐµÑˆÐ°ÐµÐ¼ (ÐºÑ€Ð¸Ñ‚ Ð²ÑÑ‘ Ñ€Ð°Ð²Ð½Ð¾ Ð½ÐµÐ²Ð¾Ð·Ð¼Ð¾Ð¶ÐµÐ½)
        if (isInWater() || hasLowCeiling() || hasMovementRestrictions()) {
            return true;
        }

        // Ð•ÑÐ»Ð¸ ÐºÑ€Ð¸Ñ‚Ñ‹ Ð½Ðµ Ð¾Ð±ÑÐ·Ð°Ñ‚ÐµÐ»ÑŒÐ½Ñ‹ â€” Ñ€Ð°Ð·Ñ€ÐµÑˆÐ°ÐµÐ¼
        if (!checkCritEnabled) {
            return true;
        }

        // Ð£ÐœÐÐ«Ð• ÐšÐ Ð˜Ð¢Ð«: Ð½Ð°Ð´Ñ‘Ð¶Ð½Ð°Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ° Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ñ„Ð»Ð°Ð³Ð°Ñ‚ÑŒ Ð°Ð½Ñ‚Ð¸Ñ‡Ð¸Ñ‚
        if (smartCritsEnabled) {
            // ÐÐ° Ð·ÐµÐ¼Ð»Ðµ ÐÐ• Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼ â€” ÑÑ‚Ð¾ Ð³Ð»Ð°Ð²Ð½Ñ‹Ð¹ Ð¸ÑÑ‚Ð¾Ñ‡Ð½Ð¸Ðº Ñ„Ð»Ð°Ð³Ð¾Ð²
            if (mc.player.isOnGround()) {
                return false;
            }

            // Ð’Ð¾ÑÑ…Ð¾Ð´ÑÑ‰ÐµÐµ Ð´Ð²Ð¸Ð¶ÐµÐ½Ð¸Ðµ â€” Ð½Ðµ Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼
            if (isAscending()) {
                return false;
            }

            // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ‡Ñ‚Ð¾ Ð¿Ð°Ð´Ð°ÐµÐ¼ Ñ Ð´Ð¾ÑÑ‚Ð°Ñ‚Ð¾Ñ‡Ð½Ñ‹Ð¼ fallDistance
            if (mc.player.fallDistance > 0.5 && isDescending()) {
                return true;
            }

            // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ ÑÐ¸Ð¼ÑƒÐ»ÑÑ†Ð¸ÐµÐ¹: Ð±ÑƒÐ´ÐµÑ‚ Ð»Ð¸ ÐºÑ€Ð¸Ñ‚ Ð² Ð±Ð»Ð¸Ð¶Ð°Ð¹ÑˆÐ¸Ðµ 3 Ñ‚Ð¸ÐºÐ°
            for (int i = 1; i <= 3; i++) {
                if (willBeCritInTicks(i)) {
                    return true;
                }
            }

            // Ð•ÑÐ»Ð¸ Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ð½Ð°Ñ‡Ð°Ð»Ð¸ Ð¿Ð°Ð´ÐµÐ½Ð¸Ðµ (velocityY ~0) Ð¸ fallDistance > 0
            if (mc.player.fallDistance > 0.0F && Math.abs(mc.player.getVelocity().y) < 0.08) {
                return true;
            }

            return false;
        }

        // ÐžÐ‘Ð«Ð§ÐÐ«Ð• ÐšÐ Ð˜Ð¢Ð«: Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ð¸Ð´ÐµÐ°Ð»ÑŒÐ½Ñ‹Ð¹ ÐºÑ€Ð¸Ñ‚
        return isPerfectCrit();
    }

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.getTarget() == null || !config.getTarget().isAlive()) {
            return;
        }

        String whileCond = HitregLogger.buildWhileCondition(mc.player);

        if (shouldWaitForEating()) {
            HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Eating", whileCond);
            return;
        }

        if (isHoldingMace()) {
            handleMaceAttack(config);
            return;
        }

        boolean elytraMode = checkElytraMode(config);

        if (elytraMode) {
            // ÐÐ° ElytraTarget Ð±ÑŒÑ‘Ð¼ Ð±ÐµÐ· raycast Ð¸ Ð±ÐµÐ· Ð¿Ñ€Ð¾Ð²ÐµÑ€Ð¾Ðº ÐºÑ€Ð¸Ñ‚Ð¾Ð²
            // ÐÐ¾ Ð–Ð”ÐÐœ attack cooldown Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ ÑÐ¿Ð°Ð¼Ð¸Ñ‚ÑŒ
            float cooldownProgress = mc.player.getAttackCooldownProgress(0);
            if (cooldownProgress < 0.95F) {
                return; // Ð–Ð´Ñ‘Ð¼ ÐºÑƒÐ»Ð´Ð°ÑƒÐ½
            }

            // ÐœÐ¸Ð½Ð¸Ð¼Ð°Ð»ÑŒÐ½Ð°Ñ Ð·Ð°Ð´ÐµÑ€Ð¶ÐºÐ° 150ms Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ð·Ð°ÐºÐ»Ð¸ÐºÐ¸Ð²Ð°Ð»Ð¾
            long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
            if (timeSinceLastAttack < 150) {
                return;
            }
            // ÐšÑ€Ð¸Ñ‚Ñ‹ ÐÐ• Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ â€” Ð±ÑŒÑ‘Ð¼ Ð²ÑÐµÐ³Ð´Ð°
        } else {
            // ÐžÐ±Ñ‹Ñ‡Ð½Ñ‹Ð¹ Ñ€ÐµÐ¶Ð¸Ð¼ â€” Ð¿Ð¾Ð»Ð½Ñ‹Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ¸
            if (!RaycastAngle.rayTrace(config)) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "RayTrace failed", whileCond);
                return;
            }

            if (!isLookingAtTarget(config)) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Not looking at target", whileCond);
                return;
            }

            // Ð”Ð»Ñ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ð° Ð½Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ clickScheduler - Ñ‚Ð°Ð¼ ÑÐ²Ð¾Ñ Ð»Ð¾Ð³Ð¸ÐºÐ° CPS
            if (!is1_8Mode && !clickScheduler.isCooldownComplete(0)) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "ClickScheduler cooldown", whileCond);
                return;
            }

            if (!canCritNow()) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Can't crit now", whileCond);
                return;
            }
        }

        // Ð¤ÐµÐ¹ÐºÐ¾Ð²Ð°Ñ Ñ€Ð¾Ñ‚Ð°Ñ†Ð¸Ñ â€” Ð´Ñ‘Ñ€Ð³Ð°ÐµÐ¼ ÐºÐ°Ð¼ÐµÑ€Ñƒ Ð² ÑÑ‚Ð¾Ñ€Ð¾Ð½Ñƒ Ð¾Ñ‚ Ð²Ñ€Ð°Ð³Ð°
        if (Aura.getInstance().getFakeRotation().isValue()) {
            performFakeRotation(config.getTarget());
        }

        preAttackEntity(config);

        boolean wasSprinting = mc.player.isSprinting();
        boolean shouldReset = wasSprinting && shouldResetSprintForCrit();

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("ÐŸÐ°ÐºÐµÑ‚Ð½Ñ‹Ð¹")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            } else {
                mc.options.sprintKey.setPressed(false);
            }
        }

        executeAttack(config);
        // Log successful attack
        HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                mc.player.distanceTo(config.getTarget()), true, "Attack executed", whileCond);

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("ÐŸÐ°ÐºÐµÑ‚Ð½Ñ‹Ð¹")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.options.sprintKey.setPressed(true);
            }
        }
    }

    private void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() &&
                mc.player.isUsingItem() &&
                mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
    }

    private void handleMaceAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        String whileCond = HitregLogger.buildWhileCondition(mc.player);

        if (shouldWaitForEating()) {
            HitregLogger.logAuraAttack("Mace", config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Eating", whileCond);
            return;
        }
        if (mc.player.distanceTo(config.getTarget()) > Aura.getInstance().getAttackrange().getValue() + 1.0) {
            HitregLogger.logAuraAttack("Mace", config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Out of range", whileCond);
            return;
        }

        // Ð”Ð»Ñ ElytraTarget Ð¼Ð¸Ð½Ð¸Ð¼ÑƒÐ¼ Ð¿Ñ€Ð¾Ð²ÐµÑ€Ð¾Ðº
        boolean elytraMode = checkElytraMode(config);
        if (!elytraMode) {
            // ÐžÐ±Ñ‹Ñ‡Ð½Ñ‹Ð¹ Ñ€ÐµÐ¶Ð¸Ð¼ - Ð¿Ð¾Ð»Ð½Ñ‹Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ¸
            if (!RaycastAngle.rayTrace(config)) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "RayTrace failed", whileCond);
                return;
            }
            if (!isLookingAtTarget(config)) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Not looking at target", whileCond);
                return;
            }
        }

        // Ð”Ð»Ñ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ð° Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·ÑƒÐµÐ¼ CPS Ð»Ð¾Ð³Ð¸ÐºÑƒ, Ð´Ð»Ñ Ð¾Ð±Ñ‹Ñ‡Ð½Ð¾Ð³Ð¾ - clickScheduler
        if (is1_8Mode) {
            // 1.8 Ñ€ÐµÐ¶Ð¸Ð¼ - Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ CPS Ð¾Ñ‡ÐµÑ€ÐµÐ´ÑŒ
            if (!canAttack1_8()) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "CPS cooldown", whileCond);
                return;
            }
        } else {
            // ÐžÐ±Ñ‹Ñ‡Ð½Ñ‹Ð¹ Ñ€ÐµÐ¶Ð¸Ð¼ - Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ clickScheduler
            if (!clickScheduler.isMaceFastAttack()) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "ClickScheduler mace fast attack", whileCond);
                return;
            }
            if (!attackTimer.finished(25)) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Attack timer not finished", whileCond);
                return;
            }
        }

        // Ð£Ð¿Ñ€Ð¾Ñ‰Ñ‘Ð½Ð½Ð°Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ° ÐºÑ€Ð¸Ñ‚Ð¾Ð² Ð´Ð»Ñ Ð±ÑƒÐ»Ð°Ð²Ñ‹
        if (!canCritForMace()) {
            HitregLogger.logAuraAttack("Mace", config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Can't crit for mace", whileCond);
            return;
        }

        preAttackEntity(config);

        boolean wasSprinting = mc.player.isSprinting();
        boolean shouldReset = wasSprinting && shouldResetSprintForCrit();

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("ÐŸÐ°ÐºÐµÑ‚Ð½Ñ‹Ð¹")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            } else {
                mc.options.sprintKey.setPressed(false);
            }
        }

        executeAttack(config);
        HitregLogger.logAuraAttack("Mace", config.getTarget(),
                mc.player.distanceTo(config.getTarget()), true, "Mace attack executed", whileCond);

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("ÐŸÐ°ÐºÐµÑ‚Ð½Ñ‹Ð¹")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.options.sprintKey.setPressed(true);
            }
        }
    }

    private boolean checkElytraMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
        return Aura.target != null &&
                Aura.target.isGliding() &&
                mc.player.isGliding() &&
                ElytraTarget.getInstance() != null &&
                ElytraTarget.getInstance().isState();
    }

    private boolean checkElytraRaycast(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Vec3d targetVelocity = config.getTarget().getVelocity();
        float leadTicks = 0;
        if (ElytraTarget.shouldElytraTarget) {
            leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
        }
        Vec3d predictedPos = config.getTarget().getEntityPos().add(targetVelocity.multiply(leadTicks));
        Box predictedBox = new Box(
                predictedPos.x - config.getTarget().getWidth() / 2,
                predictedPos.y,
                predictedPos.z - config.getTarget().getWidth() / 2,
                predictedPos.x + config.getTarget().getWidth() / 2,
                predictedPos.y + config.getTarget().getHeight(),
                predictedPos.z + config.getTarget().getWidth() / 2);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        return predictedBox.raycast(eyePos, eyePos.add(lookVec.multiply(config.getMaximumRange()))).isPresent();
    }

    private void executeAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (is1_8Mode && !isHoldingMace()) {
            // 1.8 Ñ€ÐµÐ¶Ð¸Ð¼ Ñ CPS Ð¸ Ð´Ð²Ð¾Ð¹Ð½Ñ‹Ð¼Ð¸ ÐºÐ»Ð¸ÐºÐ°Ð¼Ð¸ (ÐÐ• Ð´Ð»Ñ Ð±ÑƒÐ»Ð°Ð²Ñ‹)
            // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ð¼Ð¾Ð¶Ð½Ð¾ Ð»Ð¸ ÐºÐ»Ð¸ÐºÐ°Ñ‚ÑŒ ÑÐµÐ¹Ñ‡Ð°Ñ
            if (!canAttack1_8()) {
                return;
            }

            // Ð’Ñ‹Ð¿Ð¾Ð»Ð½ÑÐµÐ¼ Ð°Ñ‚Ð°ÐºÑƒ Ñ‡ÐµÑ€ÐµÐ· CPS Ð¼ÐµÐ½ÐµÐ´Ð¶ÐµÑ€
            performAttack1_8(() -> {
                mc.interactionManager.attackEntity(mc.player, config.getTarget());
                mc.player.swingHand(Hand.MAIN_HAND);
            });
            attackTimer.reset();
            count++;
            updateLastAttackTime();
            Aura.getInstance().notifyAttackExecuted();
        } else {
            // ÐžÐ±Ñ‹Ñ‡Ð½Ñ‹Ð¹ Ñ€ÐµÐ¶Ð¸Ð¼ Ð˜Ð›Ð˜ Ð±ÑƒÐ»Ð°Ð²Ð° Ð² 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ðµ
            mc.interactionManager.attackEntity(mc.player, config.getTarget());
            mc.player.swingHand(Hand.MAIN_HAND);
            attackTimer.reset();
            count++;
            updateLastAttackTime();
            Aura.getInstance().notifyAttackExecuted();
        }
    }

    /**
     * Ð£Ð¿Ñ€Ð¾Ñ‰Ñ‘Ð½Ð½Ð°Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ° ÐºÑ€Ð¸Ñ‚Ð¾Ð² Ð´Ð»Ñ Ð±ÑƒÐ»Ð°Ð²Ñ‹ (Ð¼ÐµÐ½ÐµÐµ ÐºÐ°Ð¿Ñ€Ð¸Ð·Ð½Ð°)
     * Ð‘ÑƒÐ»Ð°Ð²Ð° Ð´Ð¾Ð»Ð¶Ð½Ð° Ñ€Ð°Ð±Ð¾Ñ‚Ð°Ñ‚ÑŒ Ð¿Ð¾Ñ‡Ñ‚Ð¸ Ð²ÑÐµÐ³Ð´Ð°, Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ð±Ð°Ð·Ð¾Ð²Ñ‹Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ¸
     */
    private boolean canCritForMace() {
        // Ð’ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ðµ Ð½Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ ÐºÑ€Ð¸Ñ‚Ñ‹ Ð²Ð¾Ð¾Ð±Ñ‰Ðµ
        if (is1_8Mode) {
            return true;
        }

        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();

        // Ð•ÑÐ»Ð¸ ÐºÑ€Ð¸Ñ‚Ñ‹ Ð½Ðµ Ð¾Ð±ÑÐ·Ð°Ñ‚ÐµÐ»ÑŒÐ½Ñ‹ - Ñ€Ð°Ð·Ñ€ÐµÑˆÐ°ÐµÐ¼ Ð°Ñ‚Ð°ÐºÑƒ
        if (!checkCritEnabled) {
            return true;
        }

        // ElytraTarget - Ð¼Ð¸Ð½Ð¸Ð¼ÑƒÐ¼ Ð¿Ñ€Ð¾Ð²ÐµÑ€Ð¾Ðº
        if (isElytraTargetMode()) {
            // ÐÐ° ÑÐ»Ð¸Ñ‚Ñ€Ð°Ñ… Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ð±Ð°Ð·Ð¾Ð²Ñ‹Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ¸
            if (mc.player.isOnGround()) {
                return false; // ÐÐ° Ð·ÐµÐ¼Ð»Ðµ Ð½Ðµ Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼
            }
            return true; // Ð’ Ð²Ð¾Ð·Ð´ÑƒÑ…Ðµ Ð²ÑÐµÐ³Ð´Ð° Ñ€Ð°Ð·Ñ€ÐµÑˆÐ°ÐµÐ¼
        }

        // ÐÐ° Ð·ÐµÐ¼Ð»Ðµ ÐÐ• Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼ â€” Ð³Ð»Ð°Ð²Ð½Ñ‹Ð¹ Ð¸ÑÑ‚Ð¾Ñ‡Ð½Ð¸Ðº Ñ„Ð»Ð°Ð³Ð¾Ð² Ð°Ð½Ñ‚Ð¸Ñ‡Ð¸Ñ‚Ð°
        if (mc.player.isOnGround()) {
            return false;
        }

        // Ð’ Ð²Ð¾Ð´Ðµ â€” Ð²ÑÑ‘ Ñ€Ð°Ð²Ð½Ð¾ Ð½Ðµ Ð±ÑƒÐ´ÐµÑ‚ ÐºÑ€Ð¸Ñ‚Ð°, Ñ€Ð°Ð·Ñ€ÐµÑˆÐ°ÐµÐ¼
        if (isInWater()) {
            return true;
        }

        // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ‡Ñ‚Ð¾ Ð½Ðµ Ð²Ð¾ÑÑ…Ð¾Ð´Ð¸Ð¼
        if (isAscending() && mc.player.getVelocity().y > 0.1) {
            return false;
        }

        // Ð•ÑÐ»Ð¸ Ð¿Ð°Ð´Ð°ÐµÐ¼ â€” Ñ‚Ð¾Ñ‡Ð½Ð¾ Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼
        if (mc.player.fallDistance > 0.5 && mc.player.getVelocity().y < 0) {
            return true;
        }

        // Ð•ÑÐ»Ð¸ velocityY Ð¾Ñ‚Ñ€Ð¸Ñ†Ð°Ñ‚ÐµÐ»ÑŒÐ½Ñ‹Ð¹ (Ð¿Ð°Ð´Ð°ÐµÐ¼) â€” Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼
        if (mc.player.getVelocity().y < -0.05) {
            return true;
        }

        // ÐÐ°Ñ‡Ð°Ð»Ð¾ Ð¿Ð°Ð´ÐµÐ½Ð¸Ñ
        if (mc.player.fallDistance > 0.3 && Math.abs(mc.player.getVelocity().y) < 0.08) {
            return true;
        }

        // Ð’ÑÑ‘ Ð¾ÑÑ‚Ð°Ð»ÑŒÐ½Ð¾Ðµ Ð·Ð°Ð¿Ñ€ÐµÑ‰Ð°ÐµÐ¼ Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ñ„Ð»Ð°Ð³Ð°Ñ‚ÑŒ Ð°Ð½Ñ‚Ð¸Ñ‡Ð¸Ñ‚
        return false;
    }

    void handleTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (shouldWaitForEating())
            return;
        if (!RaycastAngle.rayTrace(config))
            return;
        if (!isLookingAtTarget(config))
            return;
        if (!clickScheduler.isCooldownComplete(0))
            return;
        if (!canAttackTrigger(config, triggerBot))
            return;

        preAttackEntity(config);

        boolean wasSprinting = mc.player.isSprinting();
        boolean shouldReset = wasSprinting && shouldResetSprintForCrit();

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("ÐŸÐ°ÐºÐµÑ‚Ð½Ñ‹Ð¹")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            } else {
                mc.options.sprintKey.setPressed(false);
            }
        }

        executeAttack(config);

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("ÐŸÐ°ÐºÐµÑ‚Ð½Ñ‹Ð¹")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.options.sprintKey.setPressed(true);
            }
        }
    }

    private boolean canAttackTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (shouldWaitForEating())
            return false;
        if (!clickScheduler.isCooldownComplete(0))
            return false;

        boolean checkCritEnabled = triggerBot.isOnlyCrits();
        boolean smartCritsEnabled = triggerBot.getSmartCrits().isValue();

        if (isInWater() || hasLowCeiling() || hasMovementRestrictions()) {
            return true;
        }

        if (!checkCritEnabled)
            return true;

        if (isAscending())
            return false;

        if (smartCritsEnabled) {
            // ÐÐ° Ð·ÐµÐ¼Ð»Ðµ ÐÐ• Ð°Ñ‚Ð°ÐºÑƒÐµÐ¼ â€” Ð¸ÑÑ‚Ð¾Ñ‡Ð½Ð¸Ðº Ñ„Ð»Ð°Ð³Ð¾Ð² Ð°Ð½Ñ‚Ð¸Ñ‡Ð¸Ñ‚Ð°
            if (mc.player.isOnGround()) {
                return false;
            }
            // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ‡Ñ‚Ð¾ Ð¿Ð°Ð´Ð°ÐµÐ¼
            return isDescending() && mc.player.fallDistance > 0.0F;
        }

        return isPerfectCrit();
    }

    public boolean shouldResetSprinting(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (Aura.target == null)
            return false;
        if (shouldWaitForEating())
            return false;
        if (isHoldingMace())
            return true;
        return shouldResetSprintForCrit();
    }

    public boolean shouldResetSprintingForTrigger(StrikerConstructor.AttackPerpetratorConfigurable config,
            TriggerBot triggerBot) {
        if (triggerBot.target == null)
            return false;
        if (shouldWaitForEating())
            return false;
        return shouldResetSprintForCrit();
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (shouldWaitForEating())
            return false;
            
        // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ‡Ñ‚Ð¾ ÑÐ¼Ð¾Ñ‚Ñ€Ð¸Ð¼ Ð² Ñ†ÐµÐ½Ñ‚Ñ€ Ñ…Ð¸Ñ‚Ð±Ð¾ÐºÑÐ°, Ð° Ð½Ðµ Ð½Ð° ÐºÑ€Ð°Ð¹
        if (!isLookingAtTargetCenter(config)) {
            return false;
        }
            
        if (isHoldingMace()) {
            // Ð”Ð»Ñ Ð±ÑƒÐ»Ð°Ð²Ñ‹ - ÑƒÐ¿Ñ€Ð¾Ñ‰Ñ‘Ð½Ð½Ð°Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ°
            if (is1_8Mode) {
                return canCritForMace();
            } else {
                return attackTimer.finished(25) && clickScheduler.isMaceFastAttack();
            }
        }

        // Ð”Ð»Ñ 1.8 Ñ€ÐµÐ¶Ð¸Ð¼Ð° - Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ CPS Ð¼ÐµÐ½ÐµÐ´Ð¶ÐµÑ€, ÐºÑ€Ð¸Ñ‚Ñ‹ Ð½Ðµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼
        if (is1_8Mode) {
            return canAttack1_8();
        }

        if (!clickScheduler.isCooldownComplete(0)) {
            return false;
        }

        if (ticks > 0) {
            Aura aura = Aura.getInstance();
            boolean checkCritEnabled = aura.getCheckCrit().isValue();
            boolean smartCritsEnabled = aura.getSmartCrits().isValue();

            if (!checkCritEnabled)
                return true;
            if (isInWater() || hasLowCeiling() || hasMovementRestrictions())
                return true;

            for (int i = 0; i <= ticks; i++) {
                if (willBeCritInTicks(i))
                    return true;
                if (smartCritsEnabled) {
                    PlayerSimulation sim = PlayerSimulation.simulateLocalPlayer(i);
                    // ÐÐ• Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ onGround â€” ÑÑ‚Ð¾ Ð²Ñ‹Ð·Ñ‹Ð²Ð°ÐµÑ‚ Ñ„Ð»Ð°Ð³Ð¸ Ð°Ð½Ñ‚Ð¸Ñ‡Ð¸Ñ‚Ð°
                    // Ð’Ð¼ÐµÑÑ‚Ð¾ ÑÑ‚Ð¾Ð³Ð¾ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ‡Ñ‚Ð¾ sim Ð½Ðµ Ð½Ð° Ð·ÐµÐ¼Ð»Ðµ Ð¸ Ð¿Ð°Ð´Ð°ÐµÑ‚
                    if (!sim.onGround && sim.velocity.y <= 0.0 && sim.fallDistance > 0.0F)
                        return true;
                }
            }
            return false;
        }

        return clickScheduler.isCooldownComplete(0) && canCritNow();
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (isHoldingMace())
            return true;

        if (mc.player.isUsingItem()
                && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        if (isInWater() || hasLowCeiling() || hasMovementRestrictions()) {
            return true;
        }

        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();
        boolean smartCritsEnabled = aura.getSmartCrits().isValue();

        if (!checkCritEnabled)
            return true;

        if (ticks > 0) {
            for (int i = 0; i <= ticks; i++) {
                if (willBeCritInTicks(i))
                    return true;
                if (smartCritsEnabled) {
                    PlayerSimulation sim = PlayerSimulation.simulateLocalPlayer(i);
                    // ÐÐ• Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ onGround â€” ÑÑ‚Ð¾ Ð²Ñ‹Ð·Ñ‹Ð²Ð°ÐµÑ‚ Ñ„Ð»Ð°Ð³Ð¸ Ð°Ð½Ñ‚Ð¸Ñ‡Ð¸Ñ‚Ð°
                    if (!sim.onGround && sim.velocity.y <= 0.0 && sim.fallDistance > 0.0F)
                        return true;
                }
            }
            return false;
        }

        return canCritNow();
    }

    /**
     * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ Ð¿Ð¾Ð¿Ð°Ð´Ð°Ð½Ð¸Ðµ Ð²Ð·Ð³Ð»ÑÐ´Ð° Ð² Ñ†ÐµÐ½Ñ‚Ñ€Ð°Ð»ÑŒÐ½ÑƒÑŽ Ñ‡Ð°ÑÑ‚ÑŒ Ñ…Ð¸Ñ‚Ð±Ð¾ÐºÑÐ° (Ð½Ðµ Ð½Ð° ÐºÑ€Ð°Ð¹)
     */
    private boolean isLookingAtTargetCenter(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.getTarget() == null || mc.player == null) return false;
        
        net.minecraft.entity.Entity target = config.getTarget();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        
        // ÐŸÐ¾Ð»ÑƒÑ‡Ð°ÐµÐ¼ Ñ…Ð¸Ñ‚Ð±Ð¾ÐºÑ Ñ†ÐµÐ»Ð¸
        Box targetBox = target.getBoundingBox();
        
        // Ð£Ð¼ÐµÐ½ÑŒÑˆÐ°ÐµÐ¼ Ñ…Ð¸Ñ‚Ð±Ð¾ÐºÑ Ð½Ð° 10% Ñ ÐºÐ°Ð¶Ð´Ð¾Ð¹ ÑÑ‚Ð¾Ñ€Ð¾Ð½Ñ‹ Ð´Ð»Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ¸ Ñ†ÐµÐ½Ñ‚Ñ€Ð°Ð»ÑŒÐ½Ð¾Ð¹ Ñ‡Ð°ÑÑ‚Ð¸ (Ð±Ñ‹Ð»Ð¾ 20%)
        double shrinkX = (targetBox.maxX - targetBox.minX) * 0.1;
        double shrinkY = (targetBox.maxY - targetBox.minY) * 0.1; 
        double shrinkZ = (targetBox.maxZ - targetBox.minZ) * 0.1;
        
        Box centerBox = new Box(
            targetBox.minX + shrinkX,
            targetBox.minY + shrinkY,
            targetBox.minZ + shrinkZ,
            targetBox.maxX - shrinkX,
            targetBox.maxY - shrinkY,
            targetBox.maxZ - shrinkZ
        );
        
        // Ð ÐµÐ¹Ñ‚Ñ€ÐµÐ¹ÑÐ¸Ð½Ð³ Ð´Ð¾ 6 Ð±Ð»Ð¾ÐºÐ¾Ð²
        for (double d = 0.1; d <= 6.0; d += 0.1) {
            Vec3d rayPos = eyePos.add(lookVec.multiply(d));
            
            // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ð¿Ð¾Ð¿Ð°Ð´Ð°Ð½Ð¸Ðµ Ð² Ñ†ÐµÐ½Ñ‚Ñ€Ð°Ð»ÑŒÐ½ÑƒÑŽ Ñ‡Ð°ÑÑ‚ÑŒ Ñ…Ð¸Ñ‚Ð±Ð¾ÐºÑÐ°
            if (centerBox.contains(rayPos)) {
                return true;
            }
            
            // Ð•ÑÐ»Ð¸ Ð»ÑƒÑ‡ Ð¿Ñ€Ð¾ÑˆÑ‘Ð» Ð¼Ð¸Ð¼Ð¾ Ñ†ÐµÐ»Ð¸ - Ð¿Ñ€ÐµÐºÑ€Ð°Ñ‰Ð°ÐµÐ¼
            if (rayPos.distanceTo(target.getEntityPos()) > target.getWidth() + 1.0) {
                break;
            }
        }
        
        return false;
    }

    private boolean isLookingAtTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        Vec3d endVec = eyePos.add(lookVec.multiply(config.getMaximumRange()));

        // Ð£Ð»ÑƒÑ‡ÑˆÐµÐ½Ð½Ð°Ñ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÐºÐ° - Ñ€Ð°ÑÑˆÐ¸Ñ€ÑÐµÐ¼ Ñ…Ð¸Ñ‚Ð±Ð¾ÐºÑ Ð´Ð»Ñ Ð±Ð¾Ð»ÐµÐµ Ð½Ð°Ð´Ñ‘Ð¶Ð½Ð¾Ð³Ð¾ Ð¿Ð¾Ð¿Ð°Ð´Ð°Ð½Ð¸Ñ
        // Ð”Ð»Ñ ElytraTarget ÐµÑ‰Ñ‘ Ð±Ð¾Ð»ÑŒÑˆÐµ Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ð¼Ð¸ÑÑÐ°Ñ‚ÑŒ
        boolean elytraTarget = mc.player.isGliding() && config.getTarget().isGliding();
        double expandAmount = elytraTarget ? 0.45 : 0.3; // Ð£Ð²ÐµÐ»Ð¸Ñ‡Ð¸Ð» Ñ 0.35/0.2
        Box expandedBox = config.getBox().expand(expandAmount);
        return expandedBox.raycast(eyePos, endVec).isPresent();
    }

    /**
     * ÐÐ´Ð°Ð¿Ñ‚Ð¸Ð²Ð½Ñ‹Ð¹ CPS - Ð°Ð²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð¿Ð¾Ð´ÑÑ‚Ñ€Ð°Ð¸Ð²Ð°ÐµÑ‚ÑÑ Ð¿Ð¾Ð´ ÑƒÑÐ»Ð¾Ð²Ð¸Ñ
     */
    private int getAdaptiveCPS() {
        if (cpsSetting == null) return 8; // Ð”ÐµÑ„Ð¾Ð»Ñ‚Ð½Ð¾Ðµ Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ðµ
        int baseCPS = (int)cpsSetting.getValue();

        // Ð•ÑÐ»Ð¸ Ð¼Ð½Ð¾Ð³Ð¾ Ð¿Ñ€Ð¾Ð¼Ð°Ñ…Ð¾Ð² - ÑÐ½Ð¸Ð¶Ð°ÐµÐ¼ CPS Ð´Ð»Ñ Ñ‚Ð¾Ñ‡Ð½Ð¾ÑÑ‚Ð¸
        if (consecutiveMisses > 3) {
            return Math.max(5, baseCPS - 2);
        }

        // Ð•ÑÐ»Ð¸ Ð²ÑÑ‘ Ñ…Ð¾Ñ€Ð¾ÑˆÐ¾ - Ð¼Ð¾Ð¶Ð½Ð¾ Ð½ÐµÐ¼Ð½Ð¾Ð³Ð¾ Ð¿Ð¾Ð´Ð½ÑÑ‚ÑŒ
        if (consecutiveHits > 10) {
            return Math.min(20, baseCPS + 1);
        }

        return baseCPS;
    }

    /**
     * Ð¤ÐµÐ¹ÐºÐ¾Ð²Ð°Ñ Ñ€Ð¾Ñ‚Ð°Ñ†Ð¸Ñ â€” Ð´Ñ‘Ñ€Ð³Ð°ÐµÑ‚ ÐºÐ°Ð¼ÐµÑ€Ñƒ Ð² ÑÑ‚Ð¾Ñ€Ð¾Ð½Ñƒ Ð¾Ñ‚ Ð²Ñ€Ð°Ð³Ð° Ð½Ð° ~130Ð¼Ñ
     * Ð’Ñ‹Ð³Ð»ÑÐ´Ð¸Ñ‚ ÐºÐ°Ðº Ð¾Ð±Ñ‹Ñ‡Ð½Ð¾Ðµ "Ð´Ñ‘Ñ€Ð³Ð°Ð½ÑŒÐµ" ÐºÐ°Ð¼ÐµÑ€Ñ‹, ÑÐºÑ€Ñ‹Ð²Ð°Ñ Ð½Ð°ÑÑ‚Ð¾ÑÑ‰Ð¸Ð¹ Ð¿Ð°Ñ‚Ñ‚ÐµÑ€Ð½ Ð½Ð°Ð²ÐµÐ´ÐµÐ½Ð¸Ñ
     */
    private void performFakeRotation(net.minecraft.entity.LivingEntity target) {
        if (mc.player == null || target == null) return;

        Aura aura = Aura.getInstance();
        float amount = aura.getFakeRotationAmount().getValue();

        // Ð’Ñ‹Ñ‡Ð¸ÑÐ»ÑÐµÐ¼ Ð½Ð°Ð¿Ñ€Ð°Ð²Ð»ÐµÐ½Ð¸Ðµ Ð¾Ñ‚ Ð²Ñ€Ð°Ð³Ð°
        Vec3d toTarget = target.getEyePos().subtract(mc.player.getEyePos());
        double horizontalAngle = Math.atan2(toTarget.z, toTarget.x);

        // Ð Ð°Ð½Ð´Ð¾Ð¼Ð½Ð¾Ðµ Ð¾Ñ‚ÐºÐ»Ð¾Ð½ÐµÐ½Ð¸Ðµ Ð² ÑÑ‚Ð¾Ñ€Ð¾Ð½Ñƒ
        double fakeYawOffset = (Math.random() - 0.5) * 2 * amount;
        double fakePitchOffset = (Math.random() * 0.5 + 0.25) * amount; // Ð§ÑƒÑ‚ÑŒ Ð¼ÐµÐ½ÑŒÑˆÐµ Ð¿Ð¾ Ð¿Ð¸Ñ‚Ñ‡Ñƒ

        // Ð¢ÐµÐºÑƒÑ‰Ð°Ñ Ñ€Ð¾Ñ‚Ð°Ñ†Ð¸Ñ
        rich.modules.impl.combat.aura.Angle currentAngle = AngleConnection.INSTANCE.getRotation();

        // Ð”Ñ‘Ñ€Ð³Ð°ÐµÐ¼ Ð² ÑÑ‚Ð¾Ñ€Ð¾Ð½Ñƒ
        float fakeYaw = currentAngle.getYaw() + (float) fakeYawOffset;
        float fakePitch = currentAngle.getPitch() + (float) fakePitchOffset;

        rich.modules.impl.combat.aura.Angle fakeAngle = new rich.modules.impl.combat.aura.Angle(fakeYaw, fakePitch);
        AngleConnection.INSTANCE.setRotation(fakeAngle);

        // Ð§ÐµÑ€ÐµÐ· 130Ð¼Ñ Ð²Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÐ¼ÑÑ Ðº Ñ†ÐµÐ»Ð¸
        new Thread(() -> {
            try {
                Thread.sleep(130);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‚ Ð¾Ð±Ñ€Ð°Ð±Ð¾Ñ‚Ð°ÐµÑ‚ÑÑ Ð°Ð²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ñ‡ÐµÑ€ÐµÐ· AngleConnection
        }).start();
    }

    /**
     * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ€ÐµÐ¶Ð¸Ð¼ ElytraTarget â€” ÐºÐ¾Ð³Ð´Ð° Ð¾Ð±Ð° Ð¸Ð³Ñ€Ð¾ÐºÐ° Ð½Ð° ÑÐ»Ð¸Ñ‚Ñ€Ð°Ñ…
     */
    private boolean isElytraTargetMode() {
        if (mc.player == null || Aura.target == null) return false;
        return mc.player.isGliding() && Aura.target.isGliding()
                && rich.modules.impl.movement.ElytraTarget.getInstance() != null
                && rich.modules.impl.movement.ElytraTarget.getInstance().isState();
    }
}


