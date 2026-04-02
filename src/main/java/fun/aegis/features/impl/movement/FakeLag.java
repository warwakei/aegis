package fun.aegis.features.impl.movement;

import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.client.Instance;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FakeLag extends Module {

    public static FakeLag getInstance() {
        return Instance.get(FakeLag.class);
    }

    final SliderSettings delayMs = new SliderSettings("Delay", "Задержка пакетов (ms)")
            .setValue(400)
            .range(0, 2000);

    final BooleanSetting fastMode = new BooleanSetting("FastMode", "Fast-режим (использует Delay, игнорируя пинг-слайдеры)")
            .setValue(false);

    final BooleanSetting fastFlushAll = new BooleanSetting("FastFlushAll", "В Fast-режиме слать всю очередь за тик")
            .setValue(true)
            .visible(() -> fastMode.isValue());

    final SliderSettings nextLagDelayMs = new SliderSettings("NextLagDelay", "Задержка старта после блинка (ms)")
            .setValue(0)
            .range(0, 3000);

    final SliderSettings maxPacketsPerTick = new SliderSettings("MaxPackets/Tick", "Максимум пакетов за тик (Normal)")
            .setValue(1)
            .range(1, 100);

    final BooleanSetting flushOnDisable = new BooleanSetting("FlushOnDisable", "Сливать очередь при выключении модуля")
            .setValue(true);

    final BooleanSetting autoBlinkOnStop = new BooleanSetting("Auto Blink On Stop", "Слить очередь при остановке")
            .setValue(true);

    final BooleanSetting pulseMode = new BooleanSetting("Pulse Mode", "Режим пульс-лаги (как PulseBlink)")
            .setValue(false);

    final SliderSettings pulseDurationMs = new SliderSettings("Pulse Duration", "Длительность пульса (ms)")
            .setValue(500)
            .range(50, 3000);

    final SliderSettings pulseCooldownMs = new SliderSettings("Pulse Cooldown", "Кулдаун между пульсами (ms)")
            .setValue(1500)
            .range(0, 5000);

    final SliderSettings maxPulsePackets = new SliderSettings("Pulse MaxPackets", "Лимит пакетов за пульс")
            .setValue(80)
            .range(10, 300);

    final BooleanSetting playerActivateRange = new BooleanSetting("PlayerActivateRange", "Работать только рядом с игроками")
            .setValue(false);
    final SliderSettings playerActivateRadius = new SliderSettings("PlayerRange", "Радиус поиска игроков")
            .setValue(20f)
            .range(0f, 30f)
            .visible(() -> playerActivateRange.isValue());

    final SliderSettings blinkPlayerRadius = new SliderSettings("BlinkPlayerRadius", "Блинк при приближении к игроку")
            .setValue(0f)
            .range(0f, 10f);

    final MultiSelectSetting blinkActions = new MultiSelectSetting("BlinkActions", "Условия блинка")
            .value("Vehicle", "Eat", "PvpAction", "Velocity", "Elytra", "Inventory", "Chat", "Sneak", "Water", "StopMotion", "Potion", "AnyAction");

    final SelectSetting renderEsp = new SelectSetting("RenderEsp", "Отображение ESP")
            .value("Off", "Soul", "Chams", "Box").selected("Soul");
    final SelectSetting espSmoothMode = new SelectSetting("EspSmooth", "Сглаживание/blur ESP")
            .value("Off", "Low", "Medium", "High").selected("Medium")
            .visible(() -> !renderEsp.isSelected("Off"));
    final BooleanSetting hideEspInFirstPerson = new BooleanSetting("HideEspInFirstPerson", "Скрывать ESP в 1 лице")
            .setValue(false)
            .visible(() -> !renderEsp.isSelected("Off"));
    final ColorSetting chamsColor = new ColorSetting("ChamsColor", "Цвет ESP")
            .value(0x80FFFFFF)
            .visible(() -> !renderEsp.isSelected("Off"));

    final BooleanSetting visualSpinMethod = new BooleanSetting("VisualSpinMethod", "Крутить визуальную модельку (Soul/Chams)")
            .setValue(false)
            .visible(() -> renderEsp.isSelected("Soul") || renderEsp.isSelected("Chams"));
    final SliderSettings spinSpeed = new SliderSettings("SpinSpeed", "Скорость вращения ESP")
            .setValue(1.0f)
            .range(0f, 10f)
            .visible(() -> visualSpinMethod.isValue() && (renderEsp.isSelected("Soul") || renderEsp.isSelected("Chams")));

    final BooleanSetting lowSpeedStart = new BooleanSetting("LowSpeedStart", "Плавное смещение серверной позиции")
            .setValue(false);
    final SliderSettings lowSpeedFactor = new SliderSettings("LowSpeedStartFactor", "Скорость смещения (0 - медленно, 1 - быстро)")
            .setValue(1.0f)
            .range(0f, 1f)
            .visible(() -> lowSpeedStart.isValue());

    final SliderSettings minIncomingPing = new SliderSettings("MinIncomingPing", "Минимальный входящий пинг (ms)")
            .setValue(150)
            .range(0, 2000);
    final SliderSettings maxIncomingPing = new SliderSettings("MaxIncomingPing", "Максимальный входящий пинг (ms)")
            .setValue(200)
            .range(0, 4000);
    final SliderSettings minIncomingPingRecalcDelay = new SliderSettings("MinIncomingPingRecalculateDelay", "Мин. задержка пересчёта вход. пинга (ms)")
            .setValue(1000)
            .range(0, 10000);
    final SliderSettings maxIncomingPingRecalcDelay = new SliderSettings("MaxIncomingPingRecalculateDelay", "Макс. задержка пересчёта вход. пинга (ms)")
            .setValue(3000)
            .range(0, 20000);

    final SelectSetting incomingPingAccelerationMode = new SelectSetting("IncomingPingAccelerationMode", "Режим изменения входящего пинга")
            .value("Instant", "Smooth").selected("Instant");

    final SliderSettings minIncomingPingDecelerationTime = new SliderSettings("MinIncomingPingDecelerationTime", "Мин. время снижения вход. пинга (ms)")
            .setValue(250)
            .range(0, 5000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Instant"));
    final SliderSettings maxIncomingPingDecelerationTime = new SliderSettings("MaxIncomingPingDecelerationTime", "Макс. время снижения вход. пинга (ms)")
            .setValue(750)
            .range(0, 10000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Instant"));

    final SliderSettings minIncomingPingAccelerationPerSecond = new SliderSettings("MinIncomingPingAccelerationPerSecond", "Мин. ускорение вход. пинга (ms/s)")
            .setValue(50)
            .range(0, 2000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxIncomingPingAccelerationPerSecond = new SliderSettings("MaxIncomingPingAccelerationPerSecond", "Макс. ускорение вход. пинга (ms/s)")
            .setValue(150)
            .range(0, 4000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings minIncomingPingAccelerationApplyDelay = new SliderSettings("MinIncomingPingAccelerationApplyDelay", "Мин. задержка применения ускорения вход. пинга (ms)")
            .setValue(250)
            .range(0, 5000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxIncomingPingAccelerationApplyDelay = new SliderSettings("MaxIncomingPingAccelerationApplyDelay", "Макс. задержка применения ускорения вход. пинга (ms)")
            .setValue(750)
            .range(0, 10000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings minIncomingPingAccelerationRecalcDelay = new SliderSettings("MinIncomingPingAccelerationRecalculateDelay", "Мин. задержка пересчёта ускорения вход. пинга (ms)")
            .setValue(1000)
            .range(0, 10000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxIncomingPingAccelerationRecalcDelay = new SliderSettings("MaxIncomingPingAccelerationRecalculateDelay", "Макс. задержка пересчёта ускорения вход. пинга (ms)")
            .setValue(3000)
            .range(0, 20000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings minIncomingPingDecelerationTime2 = new SliderSettings("MinIncomingPingDecelerationTimeSmooth", "Мин. время снижения вход. пинга (Smooth) (ms)")
            .setValue(250)
            .range(0, 5000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxIncomingPingDecelerationTime2 = new SliderSettings("MaxIncomingPingDecelerationTimeSmooth", "Макс. время снижения вход. пинга (Smooth) (ms)")
            .setValue(750)
            .range(0, 10000)
            .visible(() -> incomingPingAccelerationMode.isSelected("Smooth"));

    final SliderSettings minOutgoingPing = new SliderSettings("MinOutgoingPing", "Минимальный исходящий пинг (ms)")
            .setValue(150)
            .range(0, 2000);
    final SliderSettings maxOutgoingPing = new SliderSettings("MaxOutgoingPing", "Максимальный исходящий пинг (ms)")
            .setValue(200)
            .range(0, 4000);
    final SliderSettings minOutgoingPingRecalcDelay = new SliderSettings("MinOutgoingPingRecalculateDelay", "Мин. задержка пересчёта исх. пинга (ms)")
            .setValue(1000)
            .range(0, 10000);
    final SliderSettings maxOutgoingPingRecalcDelay = new SliderSettings("MaxOutgoingPingRecalculateDelay", "Макс. задержка пересчёта исх. пинга (ms)")
            .setValue(3000)
            .range(0, 20000);

    final SelectSetting outgoingPingAccelerationMode = new SelectSetting("OutgoingPingAccelerationMode", "Режим изменения исходящего пинга")
            .value("Instant", "Smooth").selected("Instant");

    final SliderSettings minOutgoingPingDecelerationTime = new SliderSettings("MinOutgoingPingDecelerationTime", "Мин. время снижения исх. пинга (ms)")
            .setValue(250)
            .range(0, 5000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Instant"));
    final SliderSettings maxOutgoingPingDecelerationTime = new SliderSettings("MaxOutgoingPingDecelerationTime", "Макс. время снижения исх. пинга (ms)")
            .setValue(750)
            .range(0, 10000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Instant"));

    final SliderSettings minOutgoingPingAccelerationPerSecond = new SliderSettings("MinOutgoingPingAccelerationPerSecond", "Мин. ускорение исх. пинга (ms/s)")
            .setValue(50)
            .range(0, 2000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxOutgoingPingAccelerationPerSecond = new SliderSettings("MaxOutgoingPingAccelerationPerSecond", "Макс. ускорение исх. пинга (ms/s)")
            .setValue(150)
            .range(0, 4000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings minOutgoingPingAccelerationApplyDelay = new SliderSettings("MinOutgoingPingAccelerationApplyDelay", "Мин. задержка применения ускорения исх. пинга (ms)")
            .setValue(250)
            .range(0, 5000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxOutgoingPingAccelerationApplyDelay = new SliderSettings("MaxOutgoingPingAccelerationApplyDelay", "Макс. задержка применения ускорения исх. пинга (ms)")
            .setValue(750)
            .range(0, 10000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings minOutgoingPingAccelerationRecalcDelay = new SliderSettings("MinOutgoingPingAccelerationRecalculateDelay", "Мин. задержка пересчёта ускорения исх. пинга (ms)")
            .setValue(1000)
            .range(0, 10000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxOutgoingPingAccelerationRecalcDelay = new SliderSettings("MaxOutgoingPingAccelerationRecalculateDelay", "Макс. задержка пересчёта ускорения исх. пинга (ms)")
            .setValue(3000)
            .range(0, 20000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings minOutgoingPingDecelerationTime2 = new SliderSettings("MinOutgoingPingDecelerationTimeSmooth", "Мин. время снижения исх. пинга (Smooth) (ms)")
            .setValue(250)
            .range(0, 5000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));
    final SliderSettings maxOutgoingPingDecelerationTime2 = new SliderSettings("MaxOutgoingPingDecelerationTimeSmooth", "Макс. время снижения исх. пинга (Smooth) (ms)")
            .setValue(750)
            .range(0, 10000)
            .visible(() -> outgoingPingAccelerationMode.isSelected("Smooth"));

    final BooleanSetting outgoingComboMode = new BooleanSetting("OutgoingComboMode", "Combo режим исходящего пинга")
            .setValue(false);
    final SliderSettings minOutgoingPingStartDelay = new SliderSettings("MinOutgoingPingStartDelay", "Мин. стартовая задержка пинга (ms)")
            .setValue(0)
            .range(0, 5000)
            .visible(() -> outgoingComboMode.isValue());
    final SliderSettings maxOutgoingPingStartDelay = new SliderSettings("MaxOutgoingPingStartDelay", "Макс. стартовая задержка пинга (ms)")
            .setValue(0)
            .range(0, 10000)
            .visible(() -> outgoingComboMode.isValue());
    final SliderSettings resetPingAtDistanceToTarget = new SliderSettings("ResetPingAtDistanceToTarget", "Сброс пинга по дистанции до цели")
            .setValue(0f)
            .range(0f, 20f)
            .visible(() -> outgoingComboMode.isValue());

    final Deque<QueuedPacket> queuedPackets = new ArrayDeque<>();
    final Deque<PositionSample> positionHistory = new ArrayDeque<>();

    double currentIncomingPingMs = 0.0;
    double targetIncomingPingMs = 0.0;
    long nextIncomingPingRecalcAt = 0L;

    double currentOutgoingPingMs = 0.0;
    double targetOutgoingPingMs = 0.0;
    long nextOutgoingPingRecalcAt = 0L;

    boolean pulsing = false;
    long pulseStartTime = 0L;
    long lastPulseEndTime = 0L;

    long lagResumeAtMs = 0L;

    Vec3d renderPos = null;
    float renderYaw = 0f;
    boolean hasRenderState = false;

    public FakeLag() {
        super("LagRange", ModuleCategory.MOVEMENT);
        setup(
                delayMs,
                fastMode,
                fastFlushAll,
                nextLagDelayMs,
                maxPacketsPerTick,
                flushOnDisable,
                autoBlinkOnStop,
                pulseMode,
                pulseDurationMs,
                pulseCooldownMs,
                maxPulsePackets,
                playerActivateRange,
                playerActivateRadius,
                blinkPlayerRadius,
                blinkActions,
                minIncomingPing,
                maxIncomingPing,
                minIncomingPingRecalcDelay,
                maxIncomingPingRecalcDelay,
                incomingPingAccelerationMode,
                minIncomingPingDecelerationTime,
                maxIncomingPingDecelerationTime,
                minIncomingPingAccelerationPerSecond,
                maxIncomingPingAccelerationPerSecond,
                minIncomingPingAccelerationApplyDelay,
                maxIncomingPingAccelerationApplyDelay,
                minIncomingPingAccelerationRecalcDelay,
                maxIncomingPingAccelerationRecalcDelay,
                minIncomingPingDecelerationTime2,
                maxIncomingPingDecelerationTime2,
                minOutgoingPing,
                maxOutgoingPing,
                minOutgoingPingRecalcDelay,
                maxOutgoingPingRecalcDelay,
                outgoingPingAccelerationMode,
                minOutgoingPingDecelerationTime,
                maxOutgoingPingDecelerationTime,
                minOutgoingPingAccelerationPerSecond,
                maxOutgoingPingAccelerationPerSecond,
                minOutgoingPingAccelerationApplyDelay,
                maxOutgoingPingAccelerationApplyDelay,
                minOutgoingPingAccelerationRecalcDelay,
                maxOutgoingPingAccelerationRecalcDelay,
                minOutgoingPingDecelerationTime2,
                maxOutgoingPingDecelerationTime2,
                outgoingComboMode,
                minOutgoingPingStartDelay,
                maxOutgoingPingStartDelay,
                resetPingAtDistanceToTarget,
                renderEsp,
                espSmoothMode,
                hideEspInFirstPerson,
                chamsColor,
                visualSpinMethod,
                spinSpeed,
                lowSpeedStart,
                lowSpeedFactor
        );
    }

    private boolean isDelayActive() {
        return delayMs.getValue() > 0;
    }

    private boolean isBlinkingInternal() {
        return System.currentTimeMillis() < lagResumeAtMs;
    }

    private void startBlink() {
        long now = System.currentTimeMillis();
        if (flushOnDisable.isValue()) {
            flushAll();
        } else {
            queuedPackets.clear();
        }
        pulsing = false;
        lagResumeAtMs = now + (long) nextLagDelayMs.getValue();

        positionHistory.clear();
        if (mc.player != null) {
            positionHistory.addLast(new PositionSample(
                    now,
                    mc.player.getPos(),
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));
        }

        renderPos = null;
        hasRenderState = false;
    }

    @Override
    public void activate() {
        queuedPackets.clear();
        positionHistory.clear();
        pulsing = false;
        pulseStartTime = 0L;
        lastPulseEndTime = 0L;
        lagResumeAtMs = 0L;
        renderPos = null;
        renderYaw = 0f;
        hasRenderState = false;

        if (mc.player != null) {
            long now = System.currentTimeMillis();
            positionHistory.addLast(new PositionSample(
                    now,
                    mc.player.getPos(),
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));
        }
    }

    @Override
    public void deactivate() {
        if (flushOnDisable.isValue()) {
            flushAll();
        } else {
            queuedPackets.clear();
        }
        positionHistory.clear();
        pulsing = false;
        lagResumeAtMs = 0L;
        renderPos = null;
        renderYaw = 0f;
        hasRenderState = false;
    }

    private void flushAll() {
        if (queuedPackets.isEmpty()) return;
        while (!queuedPackets.isEmpty()) {
            QueuedPacket qp = queuedPackets.pollFirst();
            if (qp != null && qp.packet() != null) {
                PlayerInteractionHelper.sendPacketWithOutEvent(qp.packet());
            }
        }
    }

    private void flushExpiredNormal() {
        if (!isDelayActive()) return;
        if (queuedPackets.isEmpty()) return;

        long now = System.currentTimeMillis();
        long delay = getCurrentDelayMs();

        int limit;
        if (fastMode.isValue() && fastFlushAll.isValue()) {
            limit = Integer.MAX_VALUE;
        } else {
            limit = (int) maxPacketsPerTick.getValue();
        }

        int sent = 0;
        while (!queuedPackets.isEmpty() && sent < limit) {
            QueuedPacket first = queuedPackets.peekFirst();
            if (first == null) break;
            if (now - first.timestamp() >= delay) {
                queuedPackets.pollFirst();
                PlayerInteractionHelper.sendPacketWithOutEvent(first.packet());
                sent++;
            } else {
                break;
            }
        }
    }

    private long getCurrentDelayMs() {
        if (fastMode.isValue()) {
            double v = delayMs.getValue();
            if (v < 0) v = 0;
            return (long) v;
        }

        double min = minOutgoingPing.getValue();
        double max = maxOutgoingPing.getValue();
        if (max < min) {
            double t = min;
            min = max;
            max = t;
        }
        double mid = (min + max) * 0.5;
        if (mid < 0) mid = 0;
        return (long) mid;
    }

    private void updateHistory() {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        positionHistory.addLast(new PositionSample(
                now,
                mc.player.getPos(),
                mc.player.getYaw(),
                mc.player.getPitch()
        ));

        long keepTime = Math.max(getCurrentDelayMs(), (long) pulseDurationMs.getValue()) + 2000L;

        while (!positionHistory.isEmpty()) {
            PositionSample first = positionHistory.peekFirst();
            if (first == null) break;
            if (now - first.timestamp() > keepTime) {
                positionHistory.pollFirst();
            } else {
                break;
            }
        }
    }

    private PositionSample getServerSample() {
        if (positionHistory.isEmpty()) return null;

        long now = System.currentTimeMillis();
        long targetTime = now - getCurrentDelayMs();
        PositionSample closest = null;
        long bestDiff = Long.MAX_VALUE;

        for (PositionSample sample : positionHistory) {
            long diff = Math.abs(sample.timestamp() - targetTime);
            if (diff < bestDiff) {
                bestDiff = diff;
                closest = sample;
            }
        }

        return closest;
    }

    private boolean isPlayerStopped() {
        if (mc.player == null) return false;
        Vec3d vel = mc.player.getVelocity();
        double speedSq = vel.x * vel.x + vel.z * vel.z;
        return speedSq < 1e-4;
    }

    private boolean canStartPulse(long now) {
        long cd = (long) pulseCooldownMs.getValue();
        return !pulsing && (now - lastPulseEndTime >= cd);
    }

    private boolean isPulseExpired(long now) {
        long duration = (long) pulseDurationMs.getValue();
        return pulsing && (now - pulseStartTime >= duration);
    }

    private boolean hasNearbyPlayer(double radius) {
        if (mc.world == null || mc.player == null) return false;
        if (radius <= 0) return true;
        double r2 = radius * radius;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (p.isDead() || p.isRemoved()) continue;
            double d2 = p.squaredDistanceTo(mc.player);
            if (d2 <= r2) return true;
        }
        return false;
    }

    private double nearestPlayerDistance() {
        if (mc.world == null || mc.player == null) return Double.MAX_VALUE;
        double best = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (p.isDead() || p.isRemoved()) continue;
            double d = p.distanceTo(mc.player);
            if (d < best) best = d;
        }
        return best;
    }

    private boolean isRidingVehicle() {
        return mc.player != null && mc.player.hasVehicle();
    }

    private boolean isUsingFood() {
        if (mc.player == null) return false;
        if (!mc.player.isUsingItem()) return false;
        ItemStack stack = mc.player.getActiveItem();
        if (stack == null || stack.isEmpty()) return false;
        UseAction action = stack.getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    private boolean isPvpUseAction() {
        if (mc.player == null) return false;
        if (!mc.player.isUsingItem()) return false;
        ItemStack stack = mc.player.getActiveItem();
        if (stack == null || stack.isEmpty()) return false;
        UseAction action = stack.getUseAction();
        return action == UseAction.BLOCK || action == UseAction.BOW || action == UseAction.CROSSBOW;
    }

    private boolean hasElytraEquipped() {
        if (mc.player == null) return false;
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return chest != null && chest.isOf(Items.ELYTRA);
    }

    private boolean isInInventory() {
        return mc.currentScreen instanceof HandledScreen<?>;
    }

    private boolean isInChat() {
        return mc.currentScreen instanceof ChatScreen;
    }

    private boolean isSneaking() {
        return mc.player != null && mc.player.isSneaking();
    }

    private boolean isInWater() {
        return mc.player != null && mc.player.isTouchingWater();
    }

    private boolean isUsingPotion() {
        if (mc.player == null) return false;
        if (!mc.player.isUsingItem()) return false;
        ItemStack stack = mc.player.getActiveItem();
        if (stack == null || stack.isEmpty()) return false;
        return stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION);
    }

    private void handleBlinkConditionsTick() {
        if (mc.player == null || mc.world == null) return;

        double radius = blinkPlayerRadius.getValue();
        if (radius > 0) {
            double dist = nearestPlayerDistance();
            if (dist <= radius && dist != Double.MAX_VALUE) {
                startBlink();
            }
        }

        if (blinkActions.isSelected("Vehicle") && isRidingVehicle()) {
            startBlink();
        }
        if (blinkActions.isSelected("Elytra") && hasElytraEquipped()) {
            startBlink();
        }
        if (blinkActions.isSelected("Inventory") && isInInventory()) {
            startBlink();
        }
        if (blinkActions.isSelected("Chat") && isInChat()) {
            startBlink();
        }
        if (blinkActions.isSelected("Eat") && isUsingFood()) {
            startBlink();
        }
        if (blinkActions.isSelected("PvpAction") && isPvpUseAction()) {
            startBlink();
        }
        if (blinkActions.isSelected("Sneak") && isSneaking()) {
            startBlink();
        }
        if (blinkActions.isSelected("Water") && isInWater()) {
            startBlink();
        }
        if (blinkActions.isSelected("StopMotion") && isPlayerStopped()) {
            startBlink();
        }

        if (blinkActions.isSelected("Potion") && isUsingPotion()) {
            startBlink();
        }

        if (blinkActions.isSelected("AnyAction")) {
            boolean any = isUsingFood() || isPvpUseAction() || isUsingPotion()
                    || isInInventory() || isInChat() || isSneaking()
                    || isInWater() || isRidingVehicle() || hasElytraEquipped();
            if (any) {
                startBlink();
            }
        }
    }

    private boolean isLaggingAllowed() {
        if (!isDelayActive() && !pulseMode.isValue()) return false;
        if (isBlinkingInternal()) return false;
        if (playerActivateRange.isValue() && !hasNearbyPlayer(playerActivateRadius.getValue())) return false;
        return true;
    }

    public boolean isLagActiveForAura() {
        return isLaggingAllowed() && !queuedPackets.isEmpty();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!isState()) return;
        if (PlayerInteractionHelper.nullCheck()) return;

        handleBlinkConditionsTick();

        if (!isLaggingAllowed()) {
            flushAll();
            updateHistory();
            return;
        }

        updateHistory();

        long now = System.currentTimeMillis();

        if (pulseMode.isValue()) {
            if (pulsing) {
                boolean tooLong = isPulseExpired(now);
                boolean tooManyPackets = queuedPackets.size() >= (int) maxPulsePackets.getValue();

                if (tooLong || tooManyPackets) {
                    flushAll();
                    pulsing = false;
                    lastPulseEndTime = now;
                }
            }
        } else {
            flushExpiredNormal();

            if (isDelayActive() && autoBlinkOnStop.isValue() && isPlayerStopped()) {
                startBlink();
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!isState()) return;
        if (PlayerInteractionHelper.nullCheck()) return;

        Packet<?> packet = e.getPacket();

        if (!e.isSend()) {
            if (packet instanceof PlayerRespawnS2CPacket
                    || packet instanceof GameJoinS2CPacket) {
                setState(false);
            }
            if (packet instanceof EntityVelocityUpdateS2CPacket vel
                    && blinkActions.isSelected("Velocity")
                    && mc.player != null
                    && vel.getEntityId() == mc.player.getId()) {
                startBlink();
            }
            if (packet instanceof EntityStatusS2CPacket s2c
                    && blinkActions.isSelected("Velocity")) {
                Entity ent = s2c.getEntity(mc.world);
                if (ent != null && ent.equals(mc.player)) {
                    byte st = s2c.getStatus();
                    if (st == 2 || st == 33) {
                        startBlink();
                    }
                }
            }
            return;
        }

        if (!isDelayActive() && !pulseMode.isValue()) return;

        if (packet instanceof ClientStatusC2SPacket status
                && status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (packet instanceof PlayerInteractEntityC2SPacket interact
                && blinkActions.isSelected("PvpAction")) {
            try {
                Object it = getInteractTypeCompat(interact);
                if (it != null && String.valueOf(it).toUpperCase().contains("ATTACK")) {
                    startBlink();
                }
            } catch (Throwable ignored) {
            }
        }

        if (!isLaggingAllowed()) {
            return;
        }

        if (pulseMode.isValue()) {
            if (!pulsing) {
                if (!canStartPulse(now)) {
                    return;
                }
                pulsing = true;
                pulseStartTime = now;
            }

            queuedPackets.addLast(new QueuedPacket(now, packet));
            e.cancel();
        } else {
            if (!isDelayActive()) return;

            queuedPackets.addLast(new QueuedPacket(now, packet));
            e.cancel();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!isState()) return;
        if (mc.world == null || mc.player == null) return;
        if (renderEsp.isSelected("Off")) return;

        if (hideEspInFirstPerson.isValue() && mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        boolean lagActive = !queuedPackets.isEmpty() && isLaggingAllowed();
        if (!lagActive) return;

        if (!isDelayActive()) {
            if (renderEsp.isSelected("Soul")) {
                float yaw = mc.player.getYaw();
                if (visualSpinMethod.isValue()) {
                    float speed = (float) spinSpeed.getValue();
                    if (speed > 0f) {
                        long now = System.currentTimeMillis();
                        float spin = (now % 3600L) / 10.0f * speed;
                        yaw += spin;
                    }
                }
                Render3D.drawEntity(
                        mc.player,
                        mc.player.getPos(),
                        yaw,
                        200,
                        e.getStack(),
                        e.getPartialTicks()
                );
                return;
            }

            int color = chamsColor.getColor();

            if (renderEsp.isSelected("Chams")) {
                int rgba = color;
                float yaw = mc.player.getYaw();
                if (visualSpinMethod.isValue()) {
                    float speed = (float) spinSpeed.getValue();
                    if (speed > 0f) {
                        long now = System.currentTimeMillis();
                        float spin = (now % 3600L) / 10.0f * speed;
                        yaw += spin;
                    }
                }

                ItemStack head = mc.player.getEquippedStack(EquipmentSlot.HEAD);
                ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                ItemStack legs = mc.player.getEquippedStack(EquipmentSlot.LEGS);
                ItemStack feet = mc.player.getEquippedStack(EquipmentSlot.FEET);

                mc.player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                mc.player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
                mc.player.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
                mc.player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
                try {
                    Render3D.drawEntity(
                            mc.player,
                            mc.player.getPos(),
                            yaw,
                            rgba,
                            e.getStack(),
                            e.getPartialTicks()
                    );
                } finally {
                    mc.player.equipStack(EquipmentSlot.HEAD, head);
                    mc.player.equipStack(EquipmentSlot.CHEST, chest);
                    mc.player.equipStack(EquipmentSlot.LEGS, legs);
                    mc.player.equipStack(EquipmentSlot.FEET, feet);
                }
                return;
            }

            if (renderEsp.isSelected("Box")) {
                Render3D.drawBox(mc.player.getBoundingBox(), color, 1.5f);
                return;
            }

            return;
        }

        PositionSample sample = getServerSample();
        if (sample == null) return;

        Vec3d targetPos = sample.pos();
        float targetYaw = sample.yaw();

        Vec3d localPos = mc.player.getPos();
        if (targetPos.squaredDistanceTo(localPos) < 0.04) {
            renderPos = null;
            hasRenderState = false;
            return;
        }

        long now = System.currentTimeMillis();

        if (!hasRenderState || renderPos == null) {
            renderPos = targetPos;
            renderYaw = targetYaw;
            hasRenderState = true;
        } else {
            double posFactor;
            if (lowSpeedStart.isValue()) {
                posFactor = MathHelper.clamp(lowSpeedFactor.getValue(), 0f, 1f);
            } else {
                posFactor = 1.0;
            }

            renderPos = renderPos.add(targetPos.subtract(renderPos).multiply(posFactor));

            float yawDiff = wrapDegrees(targetYaw - renderYaw);
            renderYaw += yawDiff * (float) posFactor;
        }

        if (renderEsp.isSelected("Soul")) {
            float finalYaw = renderYaw;
            if (visualSpinMethod.isValue()) {
                float speed = (float) spinSpeed.getValue();
                if (speed > 0f) {
                    float spin = (now % 3600L) / 10.0f * speed;
                    finalYaw += spin;
                }
            }
            Render3D.drawEntity(
                    mc.player,
                    renderPos,
                    finalYaw,
                    200,
                    e.getStack(),
                    e.getPartialTicks()
            );
            return;
        }

        int color = chamsColor.getColor();

        if (renderEsp.isSelected("Chams")) {
            int rgba = color;
            float finalYaw = renderYaw;
            if (visualSpinMethod.isValue()) {
                float speed = (float) spinSpeed.getValue();
                if (speed > 0f) {
                    float spin = (now % 3600L) / 10.0f * speed;
                    finalYaw += spin;
                }
            }

            ItemStack head = mc.player.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legs = mc.player.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feet = mc.player.getEquippedStack(EquipmentSlot.FEET);

            mc.player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            mc.player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            mc.player.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            mc.player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
            try {
                Render3D.drawEntity(
                        mc.player,
                        renderPos,
                        finalYaw,
                        rgba,
                        e.getStack(),
                        e.getPartialTicks()
                );
            } finally {
                mc.player.equipStack(EquipmentSlot.HEAD, head);
                mc.player.equipStack(EquipmentSlot.CHEST, chest);
                mc.player.equipStack(EquipmentSlot.LEGS, legs);
                mc.player.equipStack(EquipmentSlot.FEET, feet);
            }
            return;
        }

        if (renderEsp.isSelected("Box")) {
            Vec3d currentPos = mc.player.getPos();
            Vec3d offset = renderPos.subtract(currentPos);
            Box bb = mc.player.getBoundingBox().offset(offset);
            Render3D.drawBox(bb, color, 1.5f);
        }
    }

    private float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private Object getInteractTypeCompat(PlayerInteractEntityC2SPacket pkt) {
        try {
            return PlayerInteractEntityC2SPacket.class.getMethod("getType").invoke(pkt);
        } catch (Throwable ignored) {
        }
        try {
            return PlayerInteractEntityC2SPacket.class.getMethod("getInteractionType").invoke(pkt);
        } catch (Throwable ignored) {
        }
        try {
            return PlayerInteractEntityC2SPacket.class.getMethod("getAction").invoke(pkt);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private record QueuedPacket(long timestamp, Packet<?> packet) {}

    private record PositionSample(long timestamp, Vec3d pos, float yaw, float pitch) {}
}
