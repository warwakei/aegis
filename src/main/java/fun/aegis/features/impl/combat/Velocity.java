package fun.aegis.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.MotionEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Items;
import net.minecraft.item.ArmorItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.display.interfaces.QuickImports;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Velocity extends Module implements QuickImports {
    public static Velocity getInstance() {
        return Instance.get(Velocity.class);
    }

    // Обновленные режимы с добавлением новых
    SelectSetting mode = new SelectSetting("Режим", "Выберите режим уменьшения отдачи")
            .value("Grim", "V2", "V2X", "V2R", "Обычный", "NewGrim", "OldGrim", "Matrix", "Normal")
            .selected("Grim");

    // Новые настройки из предоставленного кода
    private final BooleanSetting countHits = new BooleanSetting("Счётчик ударов", "Включить счетчик ударов")
            .setValue(false);
    private final SliderSettings untilCount = new SliderSettings("До счётчика", "Количество ударов до отключения")
            .setValue(4).range(1, 10);
    private final SliderSettings afterCount = new SliderSettings("После счётчика", "Количество ударов после отключения")
            .setValue(2).range(1, 10);
    private final BooleanSetting onlyNetherite = new BooleanSetting("Только в незеритовой броне",
            "Работать только в незеритовой броне").setValue(false);
    private final BooleanSetting debug = new BooleanSetting("Логи", "Включить отладочные сообщения").setValue(false);

    // Существующие переменные
    @NonFinal
    boolean flag;
    @NonFinal
    int grimTicks;
    @NonFinal
    int ccCooldown;

    // Новые переменные из предоставленного кода
    @NonFinal
    private int hitCount = 0;
    @NonFinal
    private Vec3d lastKnockback = Vec3d.ZERO;
    @NonFinal
    private double staticChaosX = 0.5;

    public Velocity() {
        super("Velocity", ModuleCategory.COMBAT);
        setup(mode, countHits, untilCount, afterCount, onlyNetherite, debug);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!state)
            return;
        if (e.getType() != PacketEvent.Type.RECEIVE)
            return;
        if (mc.player == null || mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isInLava())
            return;
        if (ccCooldown > 0) {
            ccCooldown--;
            return;
        }

        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket pac && pac.getEntityId() == mc.player.getId()) {
            handleVelocityPacket(pac, e);
        }

        if (mode.isSelected("OldGrim") && e.getPacket() instanceof CommonPingS2CPacket && grimTicks > 0) {
            e.setCancelled(true);
            grimTicks--;
        }

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && mode.isSelected("NewGrim")) {
            ccCooldown = 5;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleVelocityPacket(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        switch (mode.getSelected()) {
            case "Обычный" -> handleNormalMode(pac, e);
            case "Grim" -> handleGrimMode(pac, e);
            case "V2" -> handleV2Mode(pac, e);
            case "V2X" -> handleV2XMode(pac, e);
            case "V2R" -> handleV2RMode(pac, e);
            case "Matrix" -> handleMatrixMode(pac, e);
            case "Normal" -> handleLegacyNormalMode(pac, e);
            case "OldGrim" -> handleOldGrimMode(pac, e);
            case "NewGrim" -> handleNewGrimMode(pac, e);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleNormalMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        lastKnockback = new Vec3d(
                pac.getVelocityX() / 8000.0,
                pac.getVelocityY() / 8000.0,
                pac.getVelocityZ() / 8000.0);
        e.setCancelled(true);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleGrimMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        lastKnockback = new Vec3d(
                pac.getVelocityX() / 8000.0,
                pac.getVelocityY() / 8000.0,
                pac.getVelocityZ() / 8000.0);
        e.setCancelled(true);
    }

    private void handleMatrixMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        if (!flag) {
            e.setCancelled(true);
            flag = true;
        } else {
            flag = false;
            setVelocityX(pac, (int) (pac.getVelocityX() * -0.1));
            setVelocityZ(pac, (int) (pac.getVelocityZ() * -0.1));
        }
    }

    private void handleLegacyNormalMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        e.setCancelled(true);
    }

    private void handleOldGrimMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        e.setCancelled(true);
        grimTicks = 6;
    }

    private void handleV2RMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        // Реверсивная отдача V2R (Safe Edition): "Липкая" отдача
        // Мы делаем реверс очень слабым, чтобы он ощущался как "сопротивление", а не
        // как "рывок"

        // Шанс срабатывания реверса 65%, в остальное время - обычный срез (V2)
        if (Math.random() > 0.65) {
            handleV2Mode(pac, e);
            return;
        }

        double PHI = 1.618033988749895;
        double r = 3.9 + (Math.sin(System.currentTimeMillis() * 0.003) * 0.05);
        staticChaosX = r * staticChaosX * (1 - staticChaosX);

        // Очень тонкий реверс: 2% - 7% от силы удара.
        // Этого хватит, чтобы не терять дистанцию, но слишком мало для флага
        double pullFactor = -0.02 - (staticChaosX * 0.05);

        // Синергия: если включен Спид/Аура, делаем еще слабее
        if (fun.aegis.features.impl.movement.Speed.getInstance().isState() || Aura.getInstance().isState()) {
            pullFactor *= 0.6;
        }

        if (Aura.getInstance().isState() && Aura.getInstance().getTarget() != null) {
            double totalPower = Math.sqrt(Math.pow(pac.getVelocityX(), 2) + Math.pow(pac.getVelocityZ(), 2));
            double boost = totalPower * Math.abs(pullFactor);

            float yaw = Aura.getInstance().getTarget().getYaw();
            double rad = Math.toRadians(yaw);

            setVelocityX(pac, (int) (-Math.sin(rad) * boost));
            setVelocityZ(pac, (int) (Math.cos(rad) * boost));
        } else {
            setVelocityX(pac, (int) (pac.getVelocityX() * pullFactor));
            setVelocityZ(pac, (int) (pac.getVelocityZ() * pullFactor));
        }

        // Y оставляем легитным (10-15%), чтобы был микро-прыжок
        setVelocityY(pac, (int) (pac.getVelocityY() * 0.12));
    }

    private void handleV2Mode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        double multiplier = 0.80; // 20% по дефолту
        if (isWearingNetherite())
            multiplier = 0.65; // 35% реджект
        else if (isWearingDiamond())
            multiplier = 0.85; // 15% реджект

        setVelocityX(pac, (int) (pac.getVelocityX() * multiplier));
        setVelocityY(pac, (int) (pac.getVelocityY() * multiplier));
        setVelocityZ(pac, (int) (pac.getVelocityZ() * multiplier));
    }

    private void handleV2XMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        double PHI = 1.618033988749895;
        // Синергия: если включены Спиды или Аура, делаем отдачу еще более легитной
        boolean synergy = fun.aegis.features.impl.movement.Speed.getInstance().isState()
                || Aura.getInstance().isState();

        // Квантовый Хаос: Логистическое отображение модулированное Золотым Сечением
        // r-параметр плавает в зависимости от времени, создавая "фрактальный" шум
        double r = 3.95 + (Math.sin(System.currentTimeMillis() * 0.001) * 0.04);
        staticChaosX = r * staticChaosX * (1 - staticChaosX);

        // Макро-модуляция: медленная синусоида для изменения "уверенности" игрока (цикл
        // ~10 сек)
        double macroMod = Math.sin(System.currentTimeMillis() / 10000.0 * Math.PI) * 0.05;

        double baseMultiplier = 0.80;
        if (isWearingNetherite())
            baseMultiplier = 0.65;
        else if (isWearingDiamond())
            baseMultiplier = 0.85;

        // "Impact Bracing": Если мы смотрим на цель в Ауре, мы "группируемся", уменьшая
        // отдачу сильнее
        if (Aura.getInstance().isState() && Aura.getInstance().getTarget() != null) {
            baseMultiplier -= 0.03; // Эффект концентрации
        }

        // Если есть синергия с другими модулями, уменьшаем силу обхода (бафаем отдачу)
        if (synergy)
            baseMultiplier += 0.05;

        // Итоговый множитель: База * Теория Хаоса + Золотое смещение + Макро-модуляция
        double finalMultiplier = baseMultiplier * (0.95 + (staticChaosX * 0.1)) + macroMod;

        // Clamp для безопасности
        finalMultiplier = MathHelper.clamp(finalMultiplier, 0.55, 0.98);

        setVelocityX(pac, (int) (pac.getVelocityX() * finalMultiplier));
        setVelocityY(pac, (int) (pac.getVelocityY() * finalMultiplier));
        setVelocityZ(pac, (int) (pac.getVelocityZ() * finalMultiplier));
    }

    private void handleNewGrimMode(EntityVelocityUpdateS2CPacket pac, PacketEvent e) {
        e.setCancelled(true);
        flag = true;
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        if (!state || mc.player == null)
            return;

        // Обработка Grim режима с компенсацией движения
        if (mode.isSelected("Grim") && mc.player.hurtTime > 0 && canApplyVelocity()) {
            handleGrimCompensation();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!state || mc.player == null || mc.player.isTouchingWater() || mc.player.isSubmergedInWater())
            return;

        handleHitCounter();
        handleExistingModes();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleGrimCompensation() {
        Vec3d playerPos = mc.player.getPos();
        Vec3d predicted = playerPos.add(lastKnockback.multiply(1.5));
        Vec2f rotation = calculateAngle(playerPos.add(0, mc.player.getEyeHeight(mc.player.getPose()), 0), predicted);

        float yawDiff = MathHelper.wrapDegrees(mc.player.getYaw() - rotation.x);

        // Компенсация движения в зависимости от направления нокбэка
        Vec3d velocity = mc.player.getVelocity();

        if (Math.abs(yawDiff) <= 60) {
            mc.player.setVelocity(velocity.x + lastKnockback.x * -1.2, velocity.y, velocity.z + lastKnockback.z * -1.2);
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        } else if (yawDiff > 120 || yawDiff < -120) {
            mc.player.setVelocity(velocity.x + lastKnockback.x * 1.3, velocity.y, velocity.z + lastKnockback.z * 1.3);
        } else if (yawDiff > 60 && yawDiff <= 150) {
            mc.player.setVelocity(velocity.x + lastKnockback.x * -0.8, velocity.y, velocity.z + lastKnockback.z * -0.8);
        } else if (yawDiff < -60 && yawDiff >= -150) {
            mc.player.setVelocity(velocity.x + lastKnockback.x * -0.8, velocity.y, velocity.z + lastKnockback.z * -0.8);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleHitCounter() {
        if (mc.player.hurtTime == 9) {
            hitCount++;

            if (countHits.isValue() && hitCount > untilCount.getValue() + afterCount.getValue()) {
                hitCount = 0;
                if (debug.isValue()) {
                    sendDebugMessage("Счетчик ударов сброшен");
                }
            }
        }
    }

    private void handleExistingModes() {
        if (mode.isSelected("Matrix") && mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
            double var3 = mc.player.getYaw() * 0.017453292F;
            double var5 = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x
                    + mc.player.getVelocity().z * mc.player.getVelocity().z);
            mc.player.setVelocity(-Math.sin(var3) * var5, mc.player.getVelocity().y, Math.cos(var3) * var5);
            mc.player.setSprinting(mc.player.age % 2 != 0);
        }

        if (mode.isSelected("NewGrim") && flag) {
            if (ccCooldown <= 0) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(),
                        mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
                mc.player.networkHandler
                        .sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                BlockPos.ofFloored(mc.player.getPos()), Direction.DOWN));
            }
            flag = false;
        }

        if (grimTicks > 0) {
            grimTicks--;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean canApplyVelocity() {
        if (countHits.isValue() && hitCount > untilCount.getValue()) {
            if (debug.isValue()) {
                sendDebugMessage("Velocity заблокирован счетчиком ударов");
            }
            return false;
        }
        if (onlyNetherite.isValue() && !isWearingNetherite()) {
            if (debug.isValue()) {
                sendDebugMessage("Velocity заблокирован - не в незеритовой броне");
            }
            return false;
        }
        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isGliding()) {
            if (debug.isValue()) {
                sendDebugMessage("Velocity заблокирован - в воде/лаве/на элитрах");
            }
            return false;
        }
        return lastKnockback.lengthSquared() > 0.01;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean isWearingNetherite() {
        int count = 0;
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = mc.player.getEquippedStack(slot);
            if (stack.getItem().equals(Items.NETHERITE_HELMET) || stack.getItem().equals(Items.NETHERITE_CHESTPLATE) ||
                    stack.getItem().equals(Items.NETHERITE_LEGGINGS) || stack.getItem().equals(Items.NETHERITE_BOOTS)) {
                count++;
            }
        }
        return count >= 2; // Хотя бы 2 вещи для бонуса
    }

    private boolean isWearingDiamond() {
        int count = 0;
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = mc.player.getEquippedStack(slot);
            if (stack.getItem().equals(Items.DIAMOND_HELMET) || stack.getItem().equals(Items.DIAMOND_CHESTPLATE) ||
                    stack.getItem().equals(Items.DIAMOND_LEGGINGS) || stack.getItem().equals(Items.DIAMOND_BOOTS)) {
                count++;
            }
        }
        return count >= 2;
    }

    private Vec2f calculateAngle(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (Math.atan2(diff.z, diff.x) * 180 / Math.PI) - 90F;
        float pitch = (float) (-(Math.atan2(diff.y, distance) * 180 / Math.PI));
        return new Vec2f(yaw, pitch);
    }

    private void sendDebugMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("[Velocity] " + message), false);
        }
    }

    @Override
    public void activate() {
        super.activate();
        grimTicks = 0;
        flag = false;
        ccCooldown = 0;
        hitCount = 0;
        lastKnockback = Vec3d.ZERO;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        grimTicks = 0;
        flag = false;
        ccCooldown = 0;
        hitCount = 0;
        lastKnockback = Vec3d.ZERO;
    }

    private void setVelocityY(EntityVelocityUpdateS2CPacket packet, int value) {
        try {
            java.lang.reflect.Field field = EntityVelocityUpdateS2CPacket.class.getDeclaredField("velocityY");
            field.setAccessible(true);
            field.setInt(packet, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setVelocityX(EntityVelocityUpdateS2CPacket packet, int value) {
        try {
            java.lang.reflect.Field field = EntityVelocityUpdateS2CPacket.class.getDeclaredField("velocityX");
            field.setAccessible(true);
            field.setInt(packet, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setVelocityZ(EntityVelocityUpdateS2CPacket packet, int value) {
        try {
            java.lang.reflect.Field field = EntityVelocityUpdateS2CPacket.class.getDeclaredField("velocityZ");
            field.setAccessible(true);
            field.setInt(packet, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
