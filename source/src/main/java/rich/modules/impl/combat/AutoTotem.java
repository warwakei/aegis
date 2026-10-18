package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.SwapExecutor;
import rich.util.inventory.SwapSettings;

import java.util.HashMap;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTotem extends ModuleStructure {

    final SelectSetting swapMode = new SelectSetting("Режим свапа", "Способ свапа тотема")
            .value("Instant", "Legit")
            .selected("Legit");

    final SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для взятия тотема")
            .range(1, 20).setValue(6);

    final MultiSelectSetting triggers = new MultiSelectSetting("Триггеры", "Условия для взятия тотема")
            .value("Кристалл", "Падение", "Анти ваншот", "Динамит", "Вагонетка", "Элитра")
            .selected("Кристалл", "Падение", "Анти ваншот");

    final MultiSelectSetting options = new MultiSelectSetting("Опции", "Дополнительные настройки")
            .value("Не брать если шар", "Возврат предмета", "Сохранять талисманы")
            .selected("Не брать если шар", "Возврат предмета", "Сохранять талисманы");

    final SliderSettings crystalDistance = new SliderSettings("Дистанция кристалла", "Максимальное расстояние до кристалла")
            .range(1, 12).setValue(6)
            .visible(() -> triggers.isSelected("Кристалл"));

    final SliderSettings fallHeight = new SliderSettings("Высота падения", "Минимальная высота падения")
            .range(5, 50).setValue(15)
            .visible(() -> triggers.isSelected("Падение"));

    final SliderSettings tntDistance = new SliderSettings("Дистанция динамита", "Максимальное расстояние до динамита")
            .range(3, 25).setValue(6)
            .visible(() -> triggers.isSelected("Динамит"));

    final SliderSettings tntMinecartDistance = new SliderSettings("Дистанция вагонетки", "Максимальное расстояние до вагонетки")
            .range(3, 15).setValue(6)
            .visible(() -> triggers.isSelected("Вагонетка"));

    final SliderSettings elytraHealth = new SliderSettings("Здоровье элитры", "Порог здоровья при полёте на элитре")
            .range(1, 20).setValue(10)
            .visible(() -> triggers.isSelected("Элитра"));

    final SwapExecutor executor = new SwapExecutor();
    final Map<Integer, Double> playerLastY = new HashMap<>();
    final Map<Integer, Double> playerFallStartY = new HashMap<>();

    int savedSlotId = -1;
    float fallStartY = 0;
    boolean wasFalling = false;

    PlayerEntity dangerousFallingPlayer = null;
    PlayerEntity dangerousElytraPlayer = null;

    public AutoTotem() {
        super("AutoTotem", "Auto Totem", ModuleCategory.COMBAT);
        settings(swapMode, healthThreshold, triggers, options, crystalDistance, fallHeight, tntDistance, tntMinecartDistance, elytraHealth);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        savedSlotId = -1;
        fallStartY = 0;
        wasFalling = false;
        playerLastY.clear();
        playerFallStartY.clear();
        dangerousFallingPlayer = null;
        dangerousElytraPlayer = null;
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        executor.cancel();
        savedSlotId = -1;
        playerLastY.clear();
        playerFallStartY.clear();
        dangerousFallingPlayer = null;
        dangerousElytraPlayer = null;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        executor.tick();

        if (executor.isRunning()) return;

        updateFallTracking();
        updatePlayerFallTracking();

        boolean needTotem = shouldEquipTotem();
        boolean hasTotemInOffhand = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
        boolean hasEnchantedTotemInOffhand = hasTotemInOffhand && mc.player.getOffHandStack().hasEnchantments();

        float currentHealth = mc.player.getHealth();
        float threshold = healthThreshold.getValue();

        if (needTotem) {
            if (!hasTotemInOffhand) {
                equipTotem();
            } else if (options.isSelected("Сохранять талисманы") && hasEnchantedTotemInOffhand) {
                Slot regularTotemSlot = findRegularTotemSlot();
                if (regularTotemSlot != null) {
                    swapToRegularTotem(regularTotemSlot);
                }
            }
        } else {
            dangerousFallingPlayer = null;
            dangerousElytraPlayer = null;

            if (savedSlotId != -1 && hasTotemInOffhand && options.isSelected("Возврат предмета") && currentHealth > threshold) {
                returnSavedItem();
            } else if (!hasTotemInOffhand) {
                savedSlotId = -1;
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;
        if (executor.isBlocking()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
            mc.player.setSprinting(false);
        }
    }

    private void updateFallTracking() {
        boolean isFalling = mc.player.getVelocity().y < -0.1
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.isGliding();

        if (isFalling && !wasFalling) {
            fallStartY = (float) mc.player.getY();
        }

        if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isClimbing()) {
            fallStartY = (float) mc.player.getY();
        }

        wasFalling = isFalling;
    }

    private void updatePlayerFallTracking() {
        if (mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            int id = player.getId();
            double currentY = player.getY();
            Double lastY = playerLastY.get(id);

            boolean isGoingDown = lastY != null && currentY < lastY - 0.01;
            boolean isOnGroundOrWater = player.isOnGround() || player.isTouchingWater() || player.isClimbing();

            if (isGoingDown && !isOnGroundOrWater && !player.isGliding()) {
                if (!playerFallStartY.containsKey(id)) {
                    playerFallStartY.put(id, lastY);
                }
            } else if (isOnGroundOrWater) {
                playerFallStartY.remove(id);
            }

            playerLastY.put(id, currentY);
        }

        playerLastY.entrySet().removeIf(entry -> {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.getId() == entry.getKey()) return false;
            }
            return true;
        });

        playerFallStartY.entrySet().removeIf(entry -> {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.getId() == entry.getKey()) return false;
            }
            return true;
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean shouldEquipTotem() {
        float health = mc.player.getHealth();
        float threshold = healthThreshold.getValue();

        if (health <= threshold) {
            return true;
        }

        if (triggers.isSelected("Элитра") && mc.player.isGliding()) {
            if (health <= elytraHealth.getValue()) {
                return true;
            }
        }

        if (triggers.isSelected("Падение")) {
            float fallDistance = fallStartY - (float) mc.player.getY();
            if (fallDistance >= fallHeight.getValue() && mc.player.getVelocity().y < -0.1) {
                return true;
            }
        }

        if (triggers.isSelected("Анти ваншот") && checkOneshotDanger()) {
            return true;
        }

        if (triggers.isSelected("Кристалл") && checkCrystalDanger()) {
            return true;
        }

        if (triggers.isSelected("Динамит") && checkTntDanger()) {
            return true;
        }

        if (triggers.isSelected("Вагонетка") && checkTntMinecartDanger()) {
            return true;
        }

        return false;
    }

    private boolean hasHeadInOffhand() {
        return mc.player.getOffHandStack().getItem() == Items.PLAYER_HEAD;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean checkCrystalDanger() {
        if (options.isSelected("Не брать если шар") && hasHeadInOffhand()) {
            float health = mc.player.getHealth();
            if (health > healthThreshold.getValue()) {
                return false;
            }
        }

        double distance = crystalDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                if (mc.player.distanceTo(entity) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean checkOneshotDanger() {
        dangerousFallingPlayer = null;
        dangerousElytraPlayer = null;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (isFallingPlayerDangerous(player)) {
                dangerousFallingPlayer = player;
                return true;
            }

            if (checkElytraPlayer(player)) {
                dangerousElytraPlayer = player;
                return true;
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isFallingPlayerDangerous(PlayerEntity player) {
        if (player == mc.player) return false;
        if (player.isGliding()) return false;

        Vec3d playerPos = mc.player.getEntityPos();
        double radius = 7;
        double height = 50;

        Box checkZone = new Box(
                playerPos.x - radius, playerPos.y, playerPos.z - radius,
                playerPos.x + radius, playerPos.y + height, playerPos.z + radius
        );

        if (!checkZone.intersects(player.getBoundingBox())) {
            return false;
        }

        if (player.getY() < mc.player.getY()) {
            return false;
        }

        int id = player.getId();
        Double fallStart = playerFallStartY.get(id);

        if (fallStart == null) {
            return false;
        }

        double fallDistance = fallStart - player.getY();

        return fallDistance >= 3;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean checkElytraPlayer(PlayerEntity player) {
        if (!player.isGliding()) return false;

        double distance = mc.player.distanceTo(player);
        if (distance > 20) return false;

        Vec3d velocity = player.getVelocity();
        double speed = velocity.length();

        if (speed < 0.8) return false;

        Vec3d toMe = new Vec3d(
                mc.player.getX() - player.getX(),
                mc.player.getY() - player.getY(),
                mc.player.getZ() - player.getZ()
        ).normalize();

        Vec3d velocityNorm = velocity.normalize();
        double dot = velocityNorm.x * toMe.x + velocityNorm.y * toMe.y + velocityNorm.z * toMe.z;

        if (dot > 0.25) {
            double timeToReach = distance / speed;

            if (timeToReach < 2.5) {
                return true;
            }

            double predictedX = player.getX() + velocity.x * timeToReach;
            double predictedY = player.getY() + velocity.y * timeToReach;
            double predictedZ = player.getZ() + velocity.z * timeToReach;

            double predictedDist = Math.sqrt(
                    Math.pow(predictedX - mc.player.getX(), 2) +
                            Math.pow(predictedY - mc.player.getY(), 2) +
                            Math.pow(predictedZ - mc.player.getZ(), 2)
            );

            return predictedDist < 6;
        }
        return false;
    }

    private boolean checkTntDanger() {
        double distance = tntDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntEntity) {
                if (mc.player.distanceTo(entity) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkTntMinecartDanger() {
        double distance = tntMinecartDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntMinecartEntity) {
                if (mc.player.distanceTo(entity) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void swapSlots(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
    }

    private boolean isScreenOpen() {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void equipTotem() {
        Slot totemSlot = findTotemSlot();
        if (totemSlot == null) return;

        boolean hasItemInOffhand = !mc.player.getOffHandStack().isEmpty()
                && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING;

        if (hasItemInOffhand && savedSlotId == -1) {
            savedSlotId = totemSlot.id;
        }

        final int slotId = totemSlot.id;

        if (isScreenOpen()) {
            swapSlots(slotId);
        } else if (swapMode.isSelected("Instant")) {
            executor.execute(() -> swapSlots(slotId), SwapSettings.instantWithStop());
        } else {
            executor.execute(() -> swapSlots(slotId), SwapSettings.legit());
        }
    }

    private void swapToRegularTotem(Slot regularTotemSlot) {
        if (regularTotemSlot == null) return;

        final int slotId = regularTotemSlot.id;

        if (isScreenOpen()) {
            swapSlots(slotId);
        } else if (swapMode.isSelected("Instant")) {
            executor.execute(() -> swapSlots(slotId), SwapSettings.instantWithStop());
        } else {
            executor.execute(() -> swapSlots(slotId), SwapSettings.legit());
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void returnSavedItem() {
        if (savedSlotId == -1) return;

        final int slotId = savedSlotId;

        if (isScreenOpen()) {
            swapSlots(slotId);
            savedSlotId = -1;
        } else if (swapMode.isSelected("Instant")) {
            executor.execute(() -> {
                swapSlots(slotId);
                savedSlotId = -1;
            }, SwapSettings.instantWithStop());
        } else {
            executor.execute(() -> {
                swapSlots(slotId);
                savedSlotId = -1;
            }, SwapSettings.legit());
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private Slot findTotemSlot() {
        if (mc.player == null) return null;

        Slot regularTotem = findRegularTotemSlot();
        if (regularTotem != null) {
            return regularTotem;
        }

        return findAnyTotemSlot();
    }

    private Slot findRegularTotemSlot() {
        if (mc.player == null) return null;

        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && isRegularTotem(slot.getStack())) {
                return slot;
            }
        }

        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && isRegularTotem(slot.getStack())) {
                return slot;
            }
        }

        return null;
    }

    private Slot findAnyTotemSlot() {
        if (mc.player == null) return null;

        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && isTotem(slot.getStack())) {
                return slot;
            }
        }

        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && isTotem(slot.getStack())) {
                return slot;
            }
        }

        return null;
    }

    private boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private boolean isRegularTotem(ItemStack stack) {
        if (!isTotem(stack)) return false;
        if (options.isSelected("Сохранять талисманы")) {
            return !stack.hasEnchantments();
        }
        return true;
    }
}