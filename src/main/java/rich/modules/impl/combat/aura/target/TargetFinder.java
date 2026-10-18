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
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.util.repository.friend.FriendUtils;

import java.util.ArrayList;
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
        // дешёвая фаза: сортируем кандидатов по дистанции (без рейкастов)
        List<LivingEntity> candidates = new ArrayList<>();
        StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .forEach(candidates::add);
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));

        // бюджет: раньше каждый рейкаст жёстко валидировал ВСЕ сущности мира за тик.
        // теперь сначала проверяем ближние (по сортировке), потратив не более
        // budget × 10 проверок; поток ленивый — если валидная цель есть,
        // она будет найдена тем же resultом, что и раньше (первая по дистанции).
        int limit = Math.max(1, Aura.getInstance().getTargetBudget().getInt()) * 10;
        int[] spent = {0};

        return candidates.stream()
                .filter(entity -> spent[0] < limit && (spent[0]++) >= 0)
                .filter(entity -> pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) && getFov(entity, maxDistance, ignoreWalls) < maxFov);
    }

    private Stream<LivingEntity> createStreamFromEntitiesWithPriority(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls, String priority) {
        Stream<LivingEntity> stream = StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) && getFov(entity, maxDistance, ignoreWalls) < maxFov);

        // сначала режем дальняк 128
        // чтоб по хп не лететь далеко
        stream = stream.filter(entity -> entity.distanceTo(mc.player) <= maxDistance);

        // сортировка по приору
        if ("ÐœÐµÐ½ÑŒÑˆÐµ Ð¥ÐŸ".equals(priority)) {
            // хп плюс дистан чтоб не лететь далеко
            stream = stream.sorted(
                Comparator.comparingDouble((LivingEntity e) -> e.getHealth())
                    .thenComparingDouble(e -> e.distanceTo(mc.player))
            );
        } else if ("Ð‘Ð¾Ð»ÑŒÑˆÐµ Ð¥ÐŸ".equals(priority)) {
            // приоритет по хп + дистанция
            stream = stream.sorted(
                Comparator.comparingDouble((LivingEntity e) -> e.getHealth()).reversed()
                    .thenComparingDouble(e -> e.distanceTo(mc.player))
            );
        } else if ("Ð‘Ð»Ð¸Ð¶Ðµ Ð²ÑÐµÐ³Ð¾".equals(priority)) {
            stream = stream.sorted(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));
        } else {
            // по умолчанию - ближе всего
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
         * чек нужно ли фильтровать игрока в
         * вертаем true если игрок должен быть
         *
         * логика
         * - если опция "креатив" выключена -
         * - если опция "креатив" включена -
         */
        private boolean isCreativeOrSpectator(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            boolean isCreative = player.isCreative() || player.getAbilities().creativeMode;
            boolean isSpectator = player.isSpectator();

            // fallback для серверов с задержкой флагов
            var gameMode = player.getGameMode();
            if (gameMode != null) {
                isCreative = isCreative || gameMode == net.minecraft.world.GameMode.CREATIVE;
                isSpectator = isSpectator || gameMode == net.minecraft.world.GameMode.SPECTATOR;
            }

            // дополнительная чек через abilities
            if (player.getAbilities().invulnerable && !player.getAbilities().allowFlying) {
                isCreative = true;
            }

            if (!isCreative && !isSpectator) return false;
            
            // если опция "креатив" включена - не
            // если опция "креатив" выключена - фильтруем
            return !targetSettings.contains("Креатив");
        }

        /**
         * чек нужно ли фильтровать невидимого игрока
         * вертаем true если игрок должен быть
         *
         * логика для "инвизы"
         * - если опция "инвизы" выключена -
         * - если опция "инвизы" включена -
         *
         * логика для "голые инвизы"
         * - если опция "голые инвизы" выключена
         * - если опция "голые инвизы" включена
         *
         * чек только эффект невидимости (potion effect)
         * не чек gamemode (креатив/спектр) - это
         */
        private boolean isInvisiblePlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            // чек невидимость через entity.isinvisible() и через
            boolean hasInvisibilityEffect = player.hasStatusEffect(StatusEffects.INVISIBILITY);
            boolean isInvisibleFlag = player.isInvisible();

            // если нет ни эффекта, ни флага
            if (!hasInvisibilityEffect && !isInvisibleFlag) return false;

            // чек есть ли броня на игроке
            boolean hasArmor = hasAnyArmor(player);

            // если на игроке есть броня -
            if (hasArmor) {
                // если опция "инвизы" включена - не
                // если опция "инвизы" выключена - фильтруем
                return !targetSettings.contains("Инвизы");
            }

            // если брони нет (голый инвиз) -
            // если опция "голые инвизы" включена -
            // если опция "голые инвизы" выключена -
            return !targetSettings.contains("Голые инвизы");
        }

        /**
         * чек есть ли на игроке какая-либо
         */
        private boolean hasAnyArmor(PlayerEntity player) {
            return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty() ||
                   !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
        }

        /**
         * чек является ли игрок тиммейтом в
         * вертаем true если игрок должен быть
         *
         * логика
         * - если опция "bw тиммейты" выключена
         * - если опция "bw тиммейты" включена
         */
        private boolean isBwTeammate(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity player)) return false;

            ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
            if (helmet.isEmpty()) return false;

            // чек что шлем кожаный
            if (helmet.getItem() != Items.LEATHER_HELMET) return false;

            // если опция "bw тиммейты" включена -
            if (targetSettings.contains("BW Ð¢Ð¸Ð¼Ð¼ÐµÐ¹Ñ‚Ñ‹")) {
                return false; // не фильтруем
            }

            // если опция выключена - чек цвет
            ItemStack myHelmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
            if (myHelmet.isEmpty()) return false;
            if (myHelmet.getItem() != Items.LEATHER_HELMET) return false;

            // сравниваем цвета шлемов
            // если цвета совпадают - это тиммейт,
            return getLeatherArmorColor(helmet) == getLeatherArmorColor(myHelmet);
        }

        /**
         * получает цвет кожаного шлема через data
         */
        private int getLeatherArmorColor(ItemStack stack) {
            // берем компонент dyed_color из itemstack
            DyedColorComponent colorComponent = stack.get(DataComponentTypes.DYED_COLOR);
            
            if (colorComponent != null) {
                return colorComponent.rgb();
            }
            
            // вертаем -1 если шлем не покрашен
            return -1;
        }

        private boolean isFriendPlayer(LivingEntity entity) {
            if (!(entity instanceof PlayerEntity)) return false;
            // если опция "друзья" не включена -
            // если опция "друзья" включена - не
            return !targetSettings.contains("Ð”Ñ€ÑƒÐ·ÑŒÑ") && FriendUtils.isFriend(entity);
        }

        private boolean isValidEntityType(LivingEntity entity) {
            if (entity instanceof PlayerEntity player) {
                boolean creativeSettingEnabled = targetSettings.contains("Креатив");
                boolean playerCreativeOrSpec = player.isCreative() || player.isSpectator()
                        || player.getAbilities().creativeMode
                        || player.getGameMode() == net.minecraft.world.GameMode.CREATIVE
                        || player.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;

                // "креатив" should allow targeting even if
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
