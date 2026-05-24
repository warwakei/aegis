package rich.modules.impl.combat.aura.target;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;
import rich.modules.impl.combat.AntiBot;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.util.repository.friend.FriendUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetFinder implements IMinecraft {
    final MultiPoint pointFinder = new MultiPoint();
    LivingEntity currentTarget;
    Stream<LivingEntity> potentialTargets;

    public TargetFinder() {
        this.currentTarget = null;
    }

    public void lockTarget(LivingEntity target) {
        if (this.currentTarget == null) {
            this.currentTarget = target;
        }
    }

    public void releaseTarget() {
        this.currentTarget = null;
    }

    public void validateTarget(Predicate<LivingEntity> predicate) {
        findFirstMatch(predicate).ifPresent(this::lockTarget);

        if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
            releaseTarget();
        }
    }

    public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
        if (currentTarget != null && (!pointFinder.hasValidPoint(currentTarget, maxDistance, ignoreWalls) || getFov(currentTarget, maxDistance, ignoreWalls) > maxFov)) {
            releaseTarget();
        }

        this.potentialTargets = createStreamFromEntities(entities, maxDistance, maxFov, ignoreWalls);
    }

    public void searchTargetsWithPriority(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls, String priority) {
        if (currentTarget != null && (!pointFinder.hasValidPoint(currentTarget, maxDistance, ignoreWalls) || getFov(currentTarget, maxDistance, ignoreWalls) > maxFov)) {
            releaseTarget();
        }

        this.potentialTargets = createStreamFromEntitiesWithPriority(entities, maxDistance, maxFov, ignoreWalls, priority);
    }

    private double getFov(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
        Vec3d attackVector = pointFinder.computeVector(entity, maxDistance, AngleConnection.INSTANCE.getRotation(), new LinearConstructor().randomValue(), ignoreWalls).getLeft();
        return RaycastAngle.rayTrace(maxDistance, entity.getBoundingBox()) ? 0 : AngleConnection.computeRotationDifference(MathAngle.cameraAngle(), MathAngle.calculateAngle(attackVector));
    }

    private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
        return StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) && getFov(entity, maxDistance, ignoreWalls) < maxFov)
                .sorted(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));
    }

    private Stream<LivingEntity> createStreamFromEntitiesWithPriority(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls, String priority) {
        Stream<LivingEntity> stream = StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) && getFov(entity, maxDistance, ignoreWalls) < maxFov);

        // Ð¡Ð½Ð°Ñ‡Ð°Ð»Ð° Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ð¿Ð¾ Ð¼Ð°ÐºÑÐ¸Ð¼Ð°Ð»ÑŒÐ½Ð¾Ð¹ Ð´Ð¸ÑÑ‚Ð°Ð½Ñ†Ð¸Ð¸ (128 Ð±Ð»Ð¾ÐºÐ¾Ð² Ð´Ð»Ñ MaceTarget)
        // Ð­Ñ‚Ð¾ Ð½ÑƒÐ¶Ð½Ð¾ Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð¿Ñ€Ð¸Ð¾Ñ€Ð¸Ñ‚ÐµÑ‚ Ð¿Ð¾ Ð¥ÐŸ Ð½Ðµ Ð²Ñ‹Ð±Ð¸Ñ€Ð°Ð» Ñ†ÐµÐ»Ð¸ ÑÐ»Ð¸ÑˆÐºÐ¾Ð¼ Ð´Ð°Ð»ÐµÐºÐ¾
        stream = stream.filter(entity -> entity.distanceTo(mc.player) <= maxDistance);

        // Ð¡Ð¾Ñ€Ñ‚Ð¸Ñ€Ð¾Ð²ÐºÐ° Ð¿Ð¾ Ð¿Ñ€Ð¸Ð¾Ñ€Ð¸Ñ‚ÐµÑ‚Ñƒ
        if ("ÐœÐµÐ½ÑŒÑˆÐµ Ð¥ÐŸ".equals(priority)) {
            // ÐŸÑ€Ð¸Ð¾Ñ€Ð¸Ñ‚ÐµÑ‚ Ð¿Ð¾ Ð¥ÐŸ + Ð´Ð¸ÑÑ‚Ð°Ð½Ñ†Ð¸Ñ (Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ð»ÐµÑ‚ÐµÑ‚ÑŒ ÑÐ»Ð¸ÑˆÐºÐ¾Ð¼ Ð´Ð°Ð»ÐµÐºÐ¾)
            stream = stream.sorted(
                Comparator.comparingDouble((LivingEntity e) -> e.getHealth())
                    .thenComparingDouble(e -> e.distanceTo(mc.player))
            );
        } else if ("Ð‘Ð¾Ð»ÑŒÑˆÐµ Ð¥ÐŸ".equals(priority)) {
            // ÐŸÑ€Ð¸Ð¾Ñ€Ð¸Ñ‚ÐµÑ‚ Ð¿Ð¾ Ð¥ÐŸ + Ð´Ð¸ÑÑ‚Ð°Ð½Ñ†Ð¸Ñ
            stream = stream.sorted(
                Comparator.comparingDouble((LivingEntity e) -> e.getHealth()).reversed()
                    .thenComparingDouble(e -> e.distanceTo(mc.player))
            );
        } else if ("Ð‘Ð»Ð¸Ð¶Ðµ Ð²ÑÐµÐ³Ð¾".equals(priority)) {
            stream = stream.sorted(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));
        } else {
            // ÐŸÐ¾ ÑƒÐ¼Ð¾Ð»Ñ‡Ð°Ð½Ð¸ÑŽ - Ð±Ð»Ð¸Ð¶Ðµ Ð²ÑÐµÐ³Ð¾
            stream = stream.sorted(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));
        }

        return stream;
    }

    private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
        return this.potentialTargets.filter(predicate).findFirst();
    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class EntityFilter {
        List<String> targetSettings;

        public boolean isValid(LivingEntity entity) {
            if (isLocalPlayer(entity)) return false;
            if (isInvalidHealth(entity)) return false;
            if (isBotPlayer(entity)) return false;
            if (isFriendPlayer(entity)) return false;
            if (isCreativeOrSpectator(entity)) return false;
            if (isInvisiblePlayer(entity)) return false;
            if (isBwTeammate(entity)) return false;
            return isValidEntityType(entity);
        }

        private boolean isLocalPlayer(LivingEntity entity) {
            return entity == mc.player;
        }

        private boolean isInvalidHealth(LivingEntity entity) {
            return !entity.isAlive() || entity.getHealth() <= 0;
        }

        private boolean isBotPlayer(LivingEntity entity) {
            return entity instanceof PlayerEntity player && AntiBot.getInstance().isBot(player);
        }

        /**
         * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ Ð½ÑƒÐ¶Ð½Ð¾ Ð»Ð¸ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€Ð¾Ð²Ð°Ñ‚ÑŒ Ð¸Ð³Ñ€Ð¾ÐºÐ° Ð² ÐºÑ€ÐµÐ°Ñ‚Ð¸Ð²Ðµ/ÑÐ¿ÐµÐºÑ‚Ñ€Ðµ
         * Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÑ‚ true ÐµÑÐ»Ð¸ Ð¸Ð³Ñ€Ð¾Ðº Ð´Ð¾Ð»Ð¶ÐµÐ½ Ð±Ñ‹Ñ‚ÑŒ ÐžÐ¢Ð¤Ð˜Ð›Ð¬Ð¢Ð ÐžÐ’ÐÐ (Ð½Ðµ Ð²Ð°Ð»Ð¸Ð´Ð½Ð°Ñ Ñ†ÐµÐ»ÑŒ)
         *
         * Ð›Ð¾Ð³Ð¸ÐºÐ°:
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "ÐšÑ€ÐµÐ°Ñ‚Ð¸Ð²" Ð’Ð«ÐšÐ›Ð®Ð§Ð•ÐÐ - Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ ÐºÑ€ÐµÐ°Ñ‚Ð¸Ð² Ð˜ ÑÐ¿ÐµÐºÑ‚Ð°Ñ‚Ð¾Ñ€Ð¾Ð²
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "ÐšÑ€ÐµÐ°Ñ‚Ð¸Ð²" Ð’ÐšÐ›Ð®Ð§Ð•ÐÐ - Ð½Ðµ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ (Ð±ÑŒÑ‘Ð¼ Ð²ÑÐµÑ…)
         */
        private boolean isCreativeOrSpectator(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            boolean isCreative = player.isCreative() || player.getAbilities().creativeMode;
            boolean isSpectator = player.isSpectator();

            // Fallback для серверов с задержкой флагов
            var gameMode = player.getGameMode();
            if (gameMode != null) {
                isCreative = isCreative || gameMode == net.minecraft.world.GameMode.CREATIVE;
                isSpectator = isSpectator || gameMode == net.minecraft.world.GameMode.SPECTATOR;
            }

            // Дополнительная проверка через abilities
            if (player.getAbilities().invulnerable && !player.getAbilities().allowFlying) {
                isCreative = true;
            }

            if (!isCreative && !isSpectator) return false;
            
            // Если опция "Креатив" ВКЛЮЧЕНА - НЕ фильтруем (бьём)
            // Если опция "Креатив" ВЫКЛЮЧЕНА - фильтруем (не бьём)
            return !targetSettings.contains("Креатив");
        }

        /**
         * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ Ð½ÑƒÐ¶Ð½Ð¾ Ð»Ð¸ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€Ð¾Ð²Ð°Ñ‚ÑŒ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ð¾Ð³Ð¾ Ð¸Ð³Ñ€Ð¾ÐºÐ° (Ñ ÑÑ„Ñ„ÐµÐºÑ‚Ð¾Ð¼ Ð·ÐµÐ»ÑŒÑ)
         * Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÑ‚ true ÐµÑÐ»Ð¸ Ð¸Ð³Ñ€Ð¾Ðº Ð´Ð¾Ð»Ð¶ÐµÐ½ Ð±Ñ‹Ñ‚ÑŒ ÐžÐ¢Ð¤Ð˜Ð›Ð¬Ð¢Ð ÐžÐ’ÐÐ (Ð½Ðµ Ð²Ð°Ð»Ð¸Ð´Ð½Ð°Ñ Ñ†ÐµÐ»ÑŒ)
         *
         * Ð›Ð¾Ð³Ð¸ÐºÐ° Ð´Ð»Ñ "Ð˜Ð½Ð²Ð¸Ð·Ñ‹":
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "Ð˜Ð½Ð²Ð¸Ð·Ñ‹" Ð’Ð«ÐšÐ›Ð®Ð§Ð•ÐÐ - Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ñ‹Ñ… Ð’ Ð‘Ð ÐžÐÐ• (Ð½Ðµ Ð±ÑŒÑ‘Ð¼)
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "Ð˜Ð½Ð²Ð¸Ð·Ñ‹" Ð’ÐšÐ›Ð®Ð§Ð•ÐÐ - Ð½Ðµ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ñ‹Ñ… Ð’ Ð‘Ð ÐžÐÐ• (Ð±ÑŒÑ‘Ð¼)
         *
         * Ð›Ð¾Ð³Ð¸ÐºÐ° Ð´Ð»Ñ "Ð“Ð¾Ð»Ñ‹Ðµ Ð¸Ð½Ð²Ð¸Ð·Ñ‹":
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "Ð“Ð¾Ð»Ñ‹Ðµ Ð¸Ð½Ð²Ð¸Ð·Ñ‹" Ð’Ð«ÐšÐ›Ð®Ð§Ð•ÐÐ - Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ñ‹Ñ… Ð‘Ð•Ð— Ð‘Ð ÐžÐÐ˜ (Ð½Ðµ Ð±ÑŒÑ‘Ð¼)
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "Ð“Ð¾Ð»Ñ‹Ðµ Ð¸Ð½Ð²Ð¸Ð·Ñ‹" Ð’ÐšÐ›Ð®Ð§Ð•ÐÐ - Ð½Ðµ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ñ‹Ñ… Ð‘Ð•Ð— Ð‘Ð ÐžÐÐ˜ (Ð±ÑŒÑ‘Ð¼)
         *
         * ÐŸÐ ÐžÐ’Ð•Ð Ð¯Ð•Ðœ Ð¢ÐžÐ›Ð¬ÐšÐž Ð­Ð¤Ð¤Ð•ÐšÐ¢ ÐÐ•Ð’Ð˜Ð”Ð˜ÐœÐžÐ¡Ð¢Ð˜ (Potion Effect)
         * ÐÐµ Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ GameMode (ÐºÑ€ÐµÐ°Ñ‚Ð¸Ð²/ÑÐ¿ÐµÐºÑ‚Ñ€) - ÑÑ‚Ð¾ Ð¾Ñ‚Ð´ÐµÐ»ÑŒÐ½Ð°Ñ Ð¾Ð¿Ñ†Ð¸Ñ "ÐšÑ€ÐµÐ°Ñ‚Ð¸Ð²"
         */
        private boolean isInvisiblePlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            // Проверяем невидимость через entity.isInvisible() И через эффект
            boolean hasInvisibilityEffect = player.hasStatusEffect(StatusEffects.INVISIBILITY);
            boolean isInvisibleFlag = player.isInvisible();

            // Если нет ни эффекта, ни флага - не фильтруем
            if (!hasInvisibilityEffect && !isInvisibleFlag) return false;

            // Проверяем есть ли броня на игроке
            boolean hasArmor = hasAnyArmor(player);

            // Если на игроке есть броня - проверяем опцию "Инвизы"
            if (hasArmor) {
                // Если опция "Инвизы" ВКЛЮЧЕНА - НЕ фильтруем (бьём)
                // Если опция "Инвизы" ВЫКЛЮЧЕНА - фильтруем (не бьём)
                return !targetSettings.contains("Инвизы");
            }

            // Если брони нет (голый инвиз) - проверяем опцию "Голые инвизы"
            // Если опция "Голые инвизы" ВКЛЮЧЕНА - НЕ фильтруем (бьём)
            // Если опция "Голые инвизы" ВЫКЛЮЧЕНА - фильтруем (не бьём)
            return !targetSettings.contains("Голые инвизы");
        }

        /**
         * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ ÐµÑÑ‚ÑŒ Ð»Ð¸ Ð½Ð° Ð¸Ð³Ñ€Ð¾ÐºÐµ ÐºÐ°ÐºÐ°Ñ-Ð»Ð¸Ð±Ð¾ Ð±Ñ€Ð¾Ð½Ñ
         */
        private boolean hasAnyArmor(PlayerEntity player) {
            return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
        }

        /**
         * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÑ‚ ÑÐ²Ð»ÑÐµÑ‚ÑÑ Ð»Ð¸ Ð¸Ð³Ñ€Ð¾Ðº Ñ‚Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ð¾Ð¼ Ð² BedWars Ð¿Ð¾ Ñ†Ð²ÐµÑ‚Ñƒ ÑˆÐ»ÐµÐ¼Ð°
         * Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÑ‚ true ÐµÑÐ»Ð¸ Ð¸Ð³Ñ€Ð¾Ðº Ð´Ð¾Ð»Ð¶ÐµÐ½ Ð±Ñ‹Ñ‚ÑŒ ÐžÐ¢Ð¤Ð˜Ð›Ð¬Ð¢Ð ÐžÐ’ÐÐ (Ñ‚Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚)
         *
         * Ð›Ð¾Ð³Ð¸ÐºÐ°:
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "BW Ð¢Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ñ‹" Ð’Ð«ÐšÐ›Ð®Ð§Ð•ÐÐ - Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ†Ð²ÐµÑ‚ ÑˆÐ»ÐµÐ¼Ð°, Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ñ‚Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ð¾Ð² (Ð½Ðµ Ð±ÑŒÑ‘Ð¼)
         * - Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "BW Ð¢Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ñ‹" Ð’ÐšÐ›Ð®Ð§Ð•ÐÐ - ÐÐ• Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ (Ð±ÑŒÑ‘Ð¼ Ð²ÑÐµÑ…, Ð²ÐºÐ»ÑŽÑ‡Ð°Ñ Ñ‚Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ð¾Ð²)
         */
        private boolean isBwTeammate(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
            if (helmet.isEmpty()) return false;

            // ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ‡Ñ‚Ð¾ ÑˆÐ»ÐµÐ¼ ÐºÐ¾Ð¶Ð°Ð½Ñ‹Ð¹
            if (helmet.getItem() != Items.LEATHER_HELMET) return false;

            // Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "BW Ð¢Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ñ‹" Ð’ÐšÐ›Ð®Ð§Ð•ÐÐ - ÐÐ• Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ (Ð±ÑŒÑ‘Ð¼ Ð²ÑÐµÑ…)
            if (targetSettings.contains("BW Ð¢Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ñ‹")) {
                return false; // ÐÐµ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼
            }

            // Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ Ð’Ð«ÐšÐ›Ð®Ð§Ð•ÐÐ - Ð¿Ñ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ñ†Ð²ÐµÑ‚ ÑˆÐ»ÐµÐ¼Ð°
            ItemStack myHelmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
            if (myHelmet.isEmpty()) return false;
            if (myHelmet.getItem() != Items.LEATHER_HELMET) return false;

            // Ð¡Ñ€Ð°Ð²Ð½Ð¸Ð²Ð°ÐµÐ¼ Ñ†Ð²ÐµÑ‚Ð° ÑˆÐ»ÐµÐ¼Ð¾Ð²
            // Ð•ÑÐ»Ð¸ Ñ†Ð²ÐµÑ‚Ð° ÑÐ¾Ð²Ð¿Ð°Ð´Ð°ÑŽÑ‚ - ÑÑ‚Ð¾ Ñ‚Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚, Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ (Ð½Ðµ Ð±ÑŒÑ‘Ð¼)
            return getLeatherArmorColor(helmet) == getLeatherArmorColor(myHelmet);
        }

        /**
         * ÐŸÐ¾Ð»ÑƒÑ‡Ð°ÐµÑ‚ Ñ†Ð²ÐµÑ‚ ÐºÐ¾Ð¶Ð°Ð½Ð¾Ð³Ð¾ ÑˆÐ»ÐµÐ¼Ð° Ñ‡ÐµÑ€ÐµÐ· Data Components (Minecraft 1.21.11)
         */
        private int getLeatherArmorColor(ItemStack stack) {
            // ÐŸÐ¾Ð»ÑƒÑ‡Ð°ÐµÐ¼ ÐºÐ¾Ð¼Ð¿Ð¾Ð½ÐµÐ½Ñ‚ DYED_COLOR Ð¸Ð· ItemStack
            DyedColorComponent colorComponent = stack.get(DataComponentTypes.DYED_COLOR);
            
            if (colorComponent != null) {
                return colorComponent.rgb();
            }
            
            // Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÐ¼ -1 ÐµÑÐ»Ð¸ ÑˆÐ»ÐµÐ¼ Ð½Ðµ Ð¿Ð¾ÐºÑ€Ð°ÑˆÐµÐ½
            return -1;
        }

        private boolean isFriendPlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity)) return false;
            // Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "Ð”Ñ€ÑƒÐ·ÑŒÑ" ÐÐ• Ð²ÐºÐ»ÑŽÑ‡ÐµÐ½Ð° - Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼ Ð´Ñ€ÑƒÐ·ÐµÐ¹
            // Ð•ÑÐ»Ð¸ Ð¾Ð¿Ñ†Ð¸Ñ "Ð”Ñ€ÑƒÐ·ÑŒÑ" Ð²ÐºÐ»ÑŽÑ‡ÐµÐ½Ð° - Ð½Ðµ Ñ„Ð¸Ð»ÑŒÑ‚Ñ€ÑƒÐµÐ¼
            return !targetSettings.contains("Ð”Ñ€ÑƒÐ·ÑŒÑ") && FriendUtils.isFriend(entity);
        }

        private boolean isValidEntityType(LivingEntity entity) {
            if (entity instanceof PlayerEntity player) {
                boolean creativeSettingEnabled = targetSettings.contains("Креатив");
                boolean playerCreativeOrSpec = player.isCreative() || player.isSpectator()
                        || player.getAbilities().creativeMode
                        || player.getGameMode() == net.minecraft.world.GameMode.CREATIVE
                        || player.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;

                // "Креатив" should allow targeting even if "Игроки" is disabled.
                if (creativeSettingEnabled && playerCreativeOrSpec) {
                    return true;
                }

                if (FriendUtils.isFriend(player)) {
                    return targetSettings.contains("Друзья");
                }
                return targetSettings.contains("Игроки");
            }
            if (entity instanceof AnimalEntity) {
                return targetSettings.contains("Животные");
            }
            if (entity instanceof MobEntity) {
                return targetSettings.contains("Мобы");
            }
            if (entity instanceof ArmorStandEntity) {
                return targetSettings.contains("Стойки для брони");
            }
            return false;
        }
    }
}
