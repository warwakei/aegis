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
            if (isCreativePlayer(entity)) return false;
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
         * Проверяет нужно ли фильтровать игрока в креативе/спектре
         * Возвращает true если игрок должен быть ОТФИЛЬТРОВАН (не валидная цель)
         */
        private boolean isCreativePlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            // Проверяем креатив или спектр
            boolean isCreative = player.isCreative();
            boolean isSpectator = player.isSpectator();

            if (!isCreative && !isSpectator) return false; // Не в креативе/спектре - валидная цель

            // Если опция "Креатив" НЕ включена - фильтруем (не бьём креатив/спектр)
            // Если опция "Креатив" включена - не фильтруем (бьём креатив/спектр)
            return !targetSettings.contains("Креатив");
        }

        /**
         * Проверяет нужно ли фильтровать невидимого игрока
         * Возвращает true если игрок должен быть ОТФИЛЬТРОВАН (не валидная цель)
         * 
         * Логика для "Инвизы":
         * - Если опция "Инвизы" ВЫКЛЮЧЕНА - фильтруем всех невидимых (не бьём)
         * - Если опция "Инвизы" ВКЛЮЧЕНА - не фильтруем невидимых (бьём)
         * 
         * Логика для "Голые инвизы":
         * - Если опция "Голые инвизы" ВЫКЛЮЧЕНА - фильтруем невидимых без брони
         * - Если опция "Голые инвизы" ВКЛЮЧЕНА - не фильтруем невидимых без брони (бьём)
         */
        private boolean isInvisiblePlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            // Проверяем есть ли эффект невидимости
            boolean hasInvisibility = player.hasStatusEffect(StatusEffects.INVISIBILITY);
            if (!hasInvisibility) return false; // Не невидим - не фильтруем

            // Проверяем есть ли броня на игроке
            boolean hasArmor = hasAnyArmor(player);

            // Если на игроке есть броня - проверяем опцию "Инвизы"
            if (hasArmor) {
                // Если опция "Инвизы" НЕ включена - фильтруем (не бьём)
                // Если опция "Инвизы" включена - не фильтруем (бьём)
                return !targetSettings.contains("Инвизы");
            }

            // Если брони нет (голый инвиз) - проверяем опцию "Голые инвизы"
            // Если опция "Голые инвизы" НЕ включена - фильтруем (не бьём)
            // Если опция "Голые инвизы" включена - не фильтруем (бьём)
            return !targetSettings.contains("Голые инвизы");
        }

        /**
         * Проверяет есть ли на игроке какая-либо броня
         */
        private boolean hasAnyArmor(PlayerEntity player) {
            return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
        }

        /**
         * Проверяет является ли игрок тиммейтом в BedWars по цвету шлема
         * Возвращает true если игрок должен быть ОТФИЛЬТРОВАН (тиммейт)
         *
         * Логика:
         * - Если опция "BW Тиммейты" ВЫКЛЮЧЕНА - НЕ фильтруем по тиммейтам (бьём всех)
         * - Если опция "BW Тиммейты" ВКЛЮЧЕНА - проверяем цвет шлема, фильтруем только если цвет совпадает
         */
        private boolean isBwTeammate(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
            if (helmet.isEmpty()) return false;

            // Проверяем что шлем кожаный
            if (helmet.getItem() != Items.LEATHER_HELMET) return false;

            // Если опция "BW Тиммейты" ВЫКЛЮЧЕНА - НЕ фильтруем (бьём всех с кожаными шлемами)
            if (!targetSettings.contains("BW Тиммейты")) {
                return false; // Не тиммейт, не фильтруем
            }

            // Если опция ВКЛЮЧЕНА - проверяем цвет шлема
            ItemStack myHelmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
            if (myHelmet.isEmpty()) return false;
            if (myHelmet.getItem() != Items.LEATHER_HELMET) return false;

            // Сравниваем цвета шлемов
            // Если цвета совпадают - это тиммейт, фильтруем
            return getLeatherArmorColor(helmet) == getLeatherArmorColor(myHelmet);
        }

        /**
         * Получает цвет кожаного шлема через Data Components (Minecraft 1.21.11)
         */
        private int getLeatherArmorColor(ItemStack stack) {
            // Получаем компонент DYED_COLOR из ItemStack
            DyedColorComponent colorComponent = stack.get(DataComponentTypes.DYED_COLOR);
            
            if (colorComponent != null) {
                return colorComponent.rgb();
            }
            
            // Возвращаем -1 если шлем не покрашен
            return -1;
        }

        private boolean isFriendPlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity)) return false;
            // Если опция "Друзья" НЕ включена - фильтруем друзей
            // Если опция "Друзья" включена - не фильтруем
            return !targetSettings.contains("Друзья") && FriendUtils.isFriend(entity);
        }

        private boolean isValidEntityType(LivingEntity entity) {
            if (entity instanceof PlayerEntity player) {
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