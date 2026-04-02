package rich.modules.impl.combat.aura.target;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
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
            if (isCreativePlayer(entity)) return false;
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
         * Проверяет нужно ли фильтровать игрока в креативе
         * Возвращает true если игрок должен быть ОТФИЛЬТРОВАН (не валидная цель)
         */
        private boolean isCreativePlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;
            
            // Если игрок НЕ в креативе - он валидная цель (не фильтруем)
            if (!player.isCreative()) return false;
            
            // Игрок в креативе:
            // Если опция "Креатив" НЕ включена - фильтруем (не бьём креатив)
            // Если опция "Креатив" включена - не фильтруем (бьём креатив)
            return !targetSettings.contains("Креатив");
        }

        /**
         * Проверяет является ли игрок тиммейтом в BedWars по цвету шлема
         * Шлем должен быть кожаным и окрашенным в цвет команды
         * Возвращает true если игрок должен быть ОТФИЛЬТРОВАН (тиммейт)
         */
        private boolean isBwTeammate(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;
            
            // Если опция "BW Тиммейты" НЕ включена - не проверяем на тиммейтов
            if (!targetSettings.contains("BW Тиммейты")) return false;

            ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
            if (helmet.isEmpty()) return false;

            // Проверяем что шлем кожаный
            if (helmet.getItem() != Items.LEATHER_HELMET) return false;

            // Получаем цвет нашего шлема
            ItemStack myHelmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
            if (myHelmet.isEmpty()) return false;
            if (myHelmet.getItem() != Items.LEATHER_HELMET) return false;

            // Упрощённая проверка - если у обоих кожаные шлемы, считаем что это тиммейт
            // (в BedWars у всей команды одинаковые кожаные шлемы)
            return true;
        }

        /**
         * Получает цвет кожаного шлема
         * Заглушка - компонент DYE_COLOR недоступен в этой версии
         */
        private int getLeatherArmorColor(ItemStack stack) {
            return 0;
        }

        private boolean isFriendPlayer(LivingEntity entity) {
            return entity instanceof PlayerEntity && FriendUtils.isFriend(entity);
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