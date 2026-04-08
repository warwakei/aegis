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

    // Кулдаун после последней атаки (в миллисекундах)
    private long lastAttackTime = 0;
    
    // Для 1.8 режима
    private boolean is1_8Mode = false;
    private int clicksSentThisTick = 0;
    
    // Отложенная атака для 1.8 режима
    private long pendingAttackTime = 0;
    private Runnable pendingAttack = null;

    // Статистика попаданий/промахов для адаптивного CPS
    private int consecutiveMisses = 0;
    private int consecutiveHits = 0;
    private long lastHitValidationTime = 0;

    // Ссылка на CPS настройку из Aura
    private rich.modules.module.setting.implement.SliderSettings cpsSetting;

    void tick() {
        if (mc.player != null && mc.player.isOnGround()) {
            ticksOnBlock++;
        } else {
            ticksOnBlock = 0;
        }
        
        // Сброс счётчика кликов за тик
        clicksSentThisTick = 0;
        
        // Проверяем отложенную атаку
        if (pendingAttack != null && System.currentTimeMillis() >= pendingAttackTime) {
            pendingAttack.run();
            pendingAttack = null;
        }
    }
    
    /**
     * Устанавливает 1.8 режим
     */
    public void set1_8Mode(boolean enabled) {
        this.is1_8Mode = enabled;
        if (!enabled) {
            cpsScheduler.reset();
        }
    }
    
    /**
     * Обновляет CPS из настроек
     */
    public void updateCPS(int cps, rich.modules.module.setting.implement.SliderSettings setting) {
        cpsScheduler.updateCPS(cps);
        this.cpsSetting = setting;
    }

    /**
     * Обновляет CPS из настроек (обратная совместимость)
     */
    public void updateCPS(int cps) {
        cpsScheduler.updateCPS(cps);
    }
    
    /**
     * Проверяет можно ли атаковать в 1.8 режиме
     * @return true если можно начать атаку
     */
    public boolean canAttack1_8() {
        if (!is1_8Mode) {
            return false;
        }
        
        // Если очередь не активна - начинаем новую
        if (!cpsScheduler.isQueueActive()) {
            cpsScheduler.startQueue(cpsScheduler.getCps());
        }
        
        // Проверяем можно ли сделать клик
        return cpsScheduler.shouldClick();
    }
    
    /**
     * Выполняет атаку в 1.8 режиме с обходом через быстрые пары кликов
     * Первый клик обычный, второй с задержкой 15-35мс
     * @param attackExecutor лямбда для выполнения атаки
     * @return количество кликов сделанных (1 или 2)
     */
    public int performAttack1_8(Runnable attackExecutor) {
        if (!is1_8Mode || !cpsScheduler.isQueueActive()) {
            attackExecutor.run();
            return 1;
        }
        
        // Проверяем можно ли сделать быстрый второй клик
        if (cpsScheduler.shouldDoFastSecondClick()) {
            // Делаем первый клик
            attackExecutor.run();
            
            // Планируем второй клик с задержкой 15-35мс
            int delay = cpsScheduler.getSecondClickDelay();
            cpsScheduler.useFastClick();
            cpsScheduler.registerClick(false); // Регистрируем первый клик
            
            // Сохраняем отложенную атаку
            pendingAttackTime = System.currentTimeMillis() + delay;
            pendingAttack = () -> {
                attackExecutor.run();
                cpsScheduler.registerClick(true); // Регистрируем второй клик
            };
            
            return 1; // Возвращаем 1, второй клик будет отдельно в tick()
        }
        
        // Обычный клик
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
    }
    
    /**
     * Обновляет время последней атаки
     */
    public void updateLastAttackTime() {
        lastAttackTime = System.currentTimeMillis();
        // Для 1.8 режима не обновляем clickScheduler - там своя логика CPS
        if (!is1_8Mode) {
            clickScheduler.recalculate();
        }

        // Обновляем счётчик для ElytraTarget double sneak
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
     * Проверяет готовность атаки на основе attack speed предмета
     * @return true если можно атаковать
     */
    public boolean isAttackReady() {
        if (mc.player == null) return false;
        
        float attackSpeed = getWeaponAttackSpeed();
        long cooldownMs = getCooldownMillis(attackSpeed);
        
        return System.currentTimeMillis() - lastAttackTime >= cooldownMs;
    }
    
    /**
     * Возвращает оставшееся время кулдауна в миллисекундах
     * @return время в мс, 0 если кулдаун прошёл
     */
    public long getRemainingCooldownMillis() {
        if (mc.player == null) return 0;
        
        float attackSpeed = getWeaponAttackSpeed();
        long cooldownMs = getCooldownMillis(attackSpeed);
        long elapsed = System.currentTimeMillis() - lastAttackTime;
        
        return Math.max(0, cooldownMs - elapsed);
    }
    
    /**
     * Быстрая проверка: можно ли атаковать прямо сейчас
     * Используется для спама запросами из ауры
     * @return true если кулдаун прошёл
     */
    public boolean canAttackNow() {
        return isAttackReady();
    }
    
    /**
     * Возвращает attack speed текущего оружия
     */
    private float getWeaponAttackSpeed() {
        if (mc.player == null) return 4.0f;
        
        var stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return 4.0f;
        
        Item item = stack.getItem();
        
        // Булава - 0.6 (33 тика)
        if (item == Items.MACE) return 0.6f;
        
        // Дефолтные значения для известных оружий
        String itemId = item.getTranslationKey().toLowerCase();
        if (itemId.contains("sword")) return 1.6f;      // 12 тиков
        if (itemId.contains("axe")) return 0.8f;        // 25 тиков  
        if (itemId.contains("trident")) return 1.1f;    // 18 тиков
        if (itemId.contains("shovel")) return 1.0f;     // 20 тиков
        if (itemId.contains("pickaxe")) return 1.2f;    // 16 тиков
        if (itemId.contains("hoe")) return 1.0f;        // 20 тиков
        
        return 4.0f; // Без оружия / обычный предмет
    }
    
    /**
     * Конвертирует attack speed в кулдаун в миллисекундах
     * Формула: cooldownTicks = 20 / attackSpeed
     * 1 тик = 50мс
     */
    private long getCooldownMillis(float attackSpeed) {
        if (attackSpeed <= 0) attackSpeed = 0.01f;
        
        // Для булавы особый случай - 33 тика
        if (attackSpeed == 0.6f) {
            return 1650; // 33 тика * 50мс
        }
        
        double cooldownTicks = 20.0 / attackSpeed;
        long roundedTicks = Math.round(cooldownTicks);
        
        return roundedTicks * 50L; // 1 тик = 50мс
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
        return aura.options.isSelected("Не бить если ешь") && isPlayerEating();
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

        // ElytraTarget проверяется отдельно в handleAttack, здесь пропускаем
        if (isElytraTargetMode()) {
            return true;
        }

        // В воде или с ограничениями — всегда разрешаем (крит всё равно невозможен)
        if (isInWater() || hasLowCeiling() || hasMovementRestrictions()) {
            return true;
        }

        // Если криты не обязательны — разрешаем
        if (!checkCritEnabled) {
            return true;
        }

        // УМНЫЕ КРИТЫ: надёжная проверка чтобы не флагать античит
        if (smartCritsEnabled) {
            // На земле НЕ атакуем — это главный источник флагов
            if (mc.player.isOnGround()) {
                return false;
            }

            // Восходящее движение — не атакуем
            if (isAscending()) {
                return false;
            }

            // Проверяем что падаем с достаточным fallDistance
            if (mc.player.fallDistance > 0.5 && isDescending()) {
                return true;
            }

            // Проверяем симуляцией: будет ли крит в ближайшие 3 тика
            for (int i = 1; i <= 3; i++) {
                if (willBeCritInTicks(i)) {
                    return true;
                }
            }

            // Если только начали падение (velocityY ~0) и fallDistance > 0
            if (mc.player.fallDistance > 0.0F && Math.abs(mc.player.getVelocity().y) < 0.08) {
                return true;
            }

            return false;
        }

        // ОБЫЧНЫЕ КРИТЫ: только идеальный крит
        return isPerfectCrit();
    }

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.getTarget() == null || !config.getTarget().isAlive()) {
            return;
        }

        if (shouldWaitForEating()) {
            HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Eating");
            return;
        }

        if (isHoldingMace()) {
            handleMaceAttack(config);
            return;
        }

        boolean elytraMode = checkElytraMode(config);

        if (elytraMode) {
            // На ElytraTarget бьём сразу — БЕЗ raycast и БЕЗ критов
            // Минимальная задержка 150ms чтобы не закликивало
            long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
            if (timeSinceLastAttack < 150) {
                return;
            }
            // Криты НЕ проверяем — бьём всегда
        } else {
            // Обычный режим — полные проверки
            if (!RaycastAngle.rayTrace(config)) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "RayTrace failed");
                return;
            }

            if (!isLookingAtTarget(config)) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Not looking at target");
                return;
            }

            // Для 1.8 режима не проверяем clickScheduler - там своя логика CPS
            if (!is1_8Mode && !clickScheduler.isCooldownComplete(0)) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "ClickScheduler cooldown");
                return;
            }

            if (!canCritNow()) {
                HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Can't crit now");
                return;
            }
        }

        // Фейковая ротация — дёргаем камеру в сторону от врага
        if (Aura.getInstance().getFakeRotation().isValue()) {
            performFakeRotation(config.getTarget());
        }

        preAttackEntity(config);

        boolean wasSprinting = mc.player.isSprinting();
        boolean shouldReset = wasSprinting && shouldResetSprintForCrit();

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("Пакетный")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            } else {
                mc.player.setSprinting(false);
            }
        }

        executeAttack(config);
        // Log successful attack
        HitregLogger.logAuraAttack(Aura.getInstance().getMode().getSelected(), config.getTarget(),
                mc.player.distanceTo(config.getTarget()), true, "Attack executed");

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("Пакетный")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.player.setSprinting(true);
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
        if (shouldWaitForEating()) {
            HitregLogger.logAuraAttack("Mace", config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Eating");
            return;
        }
        if (mc.player.distanceTo(config.getTarget()) > Aura.getInstance().getAttackrange().getValue() + 1.0) {
            HitregLogger.logAuraAttack("Mace", config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Out of range");
            return;
        }

        // Для ElytraTarget минимум проверок
        boolean elytraMode = checkElytraMode(config);
        if (!elytraMode) {
            // Обычный режим - полные проверки
            if (!RaycastAngle.rayTrace(config)) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "RayTrace failed");
                return;
            }
            if (!isLookingAtTarget(config)) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Not looking at target");
                return;
            }
        }

        // Для 1.8 режима используем CPS логику, для обычного - clickScheduler
        if (is1_8Mode) {
            // 1.8 режим - проверяем CPS очередь
            if (!canAttack1_8()) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "CPS cooldown");
                return;
            }
        } else {
            // Обычный режим - проверяем clickScheduler
            if (!clickScheduler.isMaceFastAttack()) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "ClickScheduler mace fast attack");
                return;
            }
            if (!attackTimer.finished(25)) {
                HitregLogger.logAuraAttack("Mace", config.getTarget(),
                        mc.player.distanceTo(config.getTarget()), false, "Attack timer not finished");
                return;
            }
        }

        // Упрощённая проверка критов для булавы
        if (!canCritForMace()) {
            HitregLogger.logAuraAttack("Mace", config.getTarget(),
                    mc.player.distanceTo(config.getTarget()), false, "Can't crit for mace");
            return;
        }

        preAttackEntity(config);

        boolean wasSprinting = mc.player.isSprinting();
        boolean shouldReset = wasSprinting && shouldResetSprintForCrit();

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("Пакетный")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            } else {
                mc.player.setSprinting(false);
            }
        }

        executeAttack(config);
        HitregLogger.logAuraAttack("Mace", config.getTarget(),
                mc.player.distanceTo(config.getTarget()), true, "Mace attack executed");

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("Пакетный")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.player.setSprinting(true);
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
            // 1.8 режим с CPS и двойными кликами (НЕ для булавы)
            // Проверяем можно ли кликать сейчас
            if (!canAttack1_8()) {
                return;
            }

            // Выполняем атаку через CPS менеджер
            performAttack1_8(() -> {
                mc.interactionManager.attackEntity(mc.player, config.getTarget());
                mc.player.swingHand(Hand.MAIN_HAND);
            });
            attackTimer.reset();
            count++;
            updateLastAttackTime();
        } else {
            // Обычный режим ИЛИ булава в 1.8 режиме
            mc.interactionManager.attackEntity(mc.player, config.getTarget());
            mc.player.swingHand(Hand.MAIN_HAND);
            attackTimer.reset();
            count++;
            updateLastAttackTime();
        }
    }

    /**
     * Упрощённая проверка критов для булавы (менее капризна)
     * Булава должна работать почти всегда, только базовые проверки
     */
    private boolean canCritForMace() {
        // В 1.8 режиме не проверяем криты вообще
        if (is1_8Mode) {
            return true;
        }

        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();

        // Если криты не обязательны - разрешаем атаку
        if (!checkCritEnabled) {
            return true;
        }

        // ElytraTarget - минимум проверок
        if (isElytraTargetMode()) {
            // На элитрах только базовые проверки
            if (mc.player.isOnGround()) {
                return false; // На земле не атакуем
            }
            return true; // В воздухе всегда разрешаем
        }

        // На земле НЕ атакуем — главный источник флагов античита
        if (mc.player.isOnGround()) {
            return false;
        }

        // В воде — всё равно не будет крита, разрешаем
        if (isInWater()) {
            return true;
        }

        // Проверяем что не восходим
        if (isAscending() && mc.player.getVelocity().y > 0.1) {
            return false;
        }

        // Если падаем — точно атакуем
        if (mc.player.fallDistance > 0.5 && mc.player.getVelocity().y < 0) {
            return true;
        }

        // Если velocityY отрицательный (падаем) — атакуем
        if (mc.player.getVelocity().y < -0.05) {
            return true;
        }

        // Начало падения
        if (mc.player.fallDistance > 0.3 && Math.abs(mc.player.getVelocity().y) < 0.08) {
            return true;
        }

        // Всё остальное запрещаем чтобы не флагать античит
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
            if (Aura.getInstance().getResetSprintMode().isSelected("Пакетный")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            } else {
                mc.player.setSprinting(false);
            }
        }

        executeAttack(config);

        if (shouldReset) {
            if (Aura.getInstance().getResetSprintMode().isSelected("Пакетный")) {
                mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.player.setSprinting(true);
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
            // На земле НЕ атакуем — источник флагов античита
            if (mc.player.isOnGround()) {
                return false;
            }
            // Проверяем что падаем
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
        if (isHoldingMace()) {
            // Для булавы - упрощённая проверка
            if (is1_8Mode) {
                return canCritForMace();
            } else {
                return attackTimer.finished(25) && clickScheduler.isMaceFastAttack();
            }
        }

        // Для 1.8 режима - проверяем CPS менеджер, криты не проверяем
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
                    // НЕ проверяем onGround — это вызывает флаги античита
                    // Вместо этого проверяем что sim не на земле и падает
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
                    // НЕ проверяем onGround — это вызывает флаги античита
                    if (!sim.onGround && sim.velocity.y <= 0.0 && sim.fallDistance > 0.0F)
                        return true;
                }
            }
            return false;
        }

        return canCritNow();
    }

    private boolean isLookingAtTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        Vec3d endVec = eyePos.add(lookVec.multiply(config.getMaximumRange()));

        // Улучшенная проверка - расширяем хитбокс для более надёжного попадания
        // Для ElytraTarget ещё больше чтобы не миссать
        boolean elytraTarget = mc.player.isGliding() && config.getTarget().isGliding();
        double expandAmount = elytraTarget ? 0.35 : 0.2;
        Box expandedBox = config.getBox().expand(expandAmount);
        return expandedBox.raycast(eyePos, endVec).isPresent();
    }

    /**
     * Адаптивный CPS - автоматически подстраивается под условия
     */
    private int getAdaptiveCPS() {
        if (cpsSetting == null) return 8; // Дефолтное значение
        int baseCPS = (int)cpsSetting.getValue();

        // Если много промахов - снижаем CPS для точности
        if (consecutiveMisses > 3) {
            return Math.max(5, baseCPS - 2);
        }

        // Если всё хорошо - можно немного поднять
        if (consecutiveHits > 10) {
            return Math.min(20, baseCPS + 1);
        }

        return baseCPS;
    }

    /**
     * Фейковая ротация — дёргает камеру в сторону от врага на ~130мс
     * Выглядит как обычное "дёрганье" камеры, скрывая настоящий паттерн наведения
     */
    private void performFakeRotation(net.minecraft.entity.LivingEntity target) {
        if (mc.player == null || target == null) return;

        Aura aura = Aura.getInstance();
        float amount = aura.getFakeRotationAmount().getValue();

        // Вычисляем направление от врага
        Vec3d toTarget = target.getEyePos().subtract(mc.player.getEyePos());
        double horizontalAngle = Math.atan2(toTarget.z, toTarget.x);

        // Рандомное отклонение в сторону
        double fakeYawOffset = (Math.random() - 0.5) * 2 * amount;
        double fakePitchOffset = (Math.random() * 0.5 + 0.25) * amount; // Чуть меньше по питчу

        // Текущая ротация
        rich.modules.impl.combat.aura.Angle currentAngle = AngleConnection.INSTANCE.getRotation();

        // Дёргаем в сторону
        float fakeYaw = currentAngle.getYaw() + (float) fakeYawOffset;
        float fakePitch = currentAngle.getPitch() + (float) fakePitchOffset;

        rich.modules.impl.combat.aura.Angle fakeAngle = new rich.modules.impl.combat.aura.Angle(fakeYaw, fakePitch);
        AngleConnection.INSTANCE.setRotation(fakeAngle);

        // Через 130мс возвращаемся к цели
        new Thread(() -> {
            try {
                Thread.sleep(130);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Возврат обработается автоматически через AngleConnection
        }).start();
    }

    /**
     * Проверяем режим ElytraTarget — когда оба игрока на элитрах
     */
    private boolean isElytraTargetMode() {
        if (mc.player == null || Aura.target == null) return false;
        return mc.player.isGliding() && Aura.target.isGliding()
                && rich.modules.impl.movement.ElytraTarget.getInstance() != null
                && rich.modules.impl.movement.ElytraTarget.getInstance().isState();
    }
}