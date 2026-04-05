package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.mixin.ClientWorldAccessor;
import rich.modules.impl.combat.AntiBot;
import rich.modules.impl.misc.JenroChatGame;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.modules.module.setting.implement.TextSetting;
import rich.util.Instance;
import rich.util.string.chat.ChatMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Автоматический фарм денег на FunnyGame через NPC + авто-перевод на основной аккаунт
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FunnyGameMoneyFarm extends ModuleStructure {

    // Координаты NPC
    private static final String TP_NPC1 = "tp -271 52 -1030";
    private static final String TP_NPC2 = "tp -149 69 -1022";

    // Задержки
    private static final int TP_DELAY_MS = 890;
    private static final int PAY_CONFIRM_DELAY_MS = 200;

    // Паттерн для парсинга баланса: [$] Ваш баланс: 1,186,805,078$
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
            "Ваш баланс:\\s*([\\d,]+)\\s*\\$",
            Pattern.CASE_INSENSITIVE
    );

    final SliderSettings farmInterval = new SliderSettings("Интервал (мин)", "Интервал между фармами")
            .range(5, 60)
            .setValue(30);

    final BooleanSetting autoEnableChatGame = new BooleanSetting("Авто-ChatGame", "Автоматически включать Jenro ChatGame")
            .setValue(true);

    final TextSetting transferTo = new TextSetting("Переводить баланс:", "Никнейм основного аккаунта для перевода")
            .setText("");

    enum FarmPhase {
        IDLE,
        COUNTDOWN,
        TELEPORT_TO_NPC1,
        WAIT_TP1,
        INTERACT_NPC1,
        TELEPORT_TO_NPC2,
        WAIT_TP2,
        INTERACT_NPC2,
        TELEPORT_HOME,
        WAIT_HOME,
        WAITING_BALANCE_RESPONSE,
        SENDING_PAY_1,
        WAITING_PAY_CONFIRM,
        SENDING_PAY_2,
        WAITING
    }

    FarmPhase phase = FarmPhase.IDLE;
    long phaseStartTime = 0;
    int countdownValue = 10;
    boolean chatGameWasEnabled = false;
    long parsedBalance = 0;
    String payTarget = "";

    public FunnyGameMoneyFarm() {
        super("FunnyGame MoneyFarm", "Автоматический фарм денег через NPC", ModuleCategory.MISC);
        settings(farmInterval, autoEnableChatGame, transferTo);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        if (mc.player == null || mc.world == null) {
            setState(false);
            return;
        }

        phase = FarmPhase.COUNTDOWN;
        countdownValue = 10;
        phaseStartTime = System.currentTimeMillis();
        chatGameWasEnabled = false;
        parsedBalance = 0;

        sendCountdownMessage();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        phase = FarmPhase.IDLE;
        ChatMessage.brandmessage("FunnyGame MoneyFarm: остановлен");
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            phase = FarmPhase.IDLE;
            return;
        }

        if (phase == FarmPhase.IDLE) return;

        switch (phase) {
            case COUNTDOWN -> handleCountdown();
            case TELEPORT_TO_NPC1 -> handleTeleportToNpc1();
            case WAIT_TP1 -> handleWaitTp1();
            case INTERACT_NPC1 -> handleInteractNpc1();
            case TELEPORT_TO_NPC2 -> handleTeleportToNpc2();
            case WAIT_TP2 -> handleWaitTp2();
            case INTERACT_NPC2 -> handleInteractNpc2();
            case TELEPORT_HOME -> handleTeleportHome();
            case WAIT_HOME -> handleWaitHome();
            case WAITING_BALANCE_RESPONSE -> handleWaitingBalanceResponse();
            case SENDING_PAY_1 -> handleSendingPay1();
            case WAITING_PAY_CONFIRM -> handleWaitingPayConfirm();
            case SENDING_PAY_2 -> handleSendingPay2();
            case WAITING -> handleWaiting();
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String text = packet.content().getString();
        if (text == null) return;

        try {
            // Парсим баланс
            Matcher matcher = BALANCE_PATTERN.matcher(text);
            if (matcher.find()) {
                String balanceStr = matcher.group(1).replace(",", "");
                try {
                    parsedBalance = Long.parseLong(balanceStr);
                    ChatMessage.brandmessage("Баланс: " + parsedBalance + "$");

                    if (phase == FarmPhase.WAITING_BALANCE_RESPONSE) {
                        processBalanceAndPay();
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleCountdown() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;

        if (elapsed >= 1000) {
            countdownValue--;
            phaseStartTime = System.currentTimeMillis();

            if (countdownValue <= 0) {
                // Включаем JenroChatGame если нужно
                if (autoEnableChatGame.isValue()) {
                    try {
                        JenroChatGame chatGame = Instance.get(JenroChatGame.class);
                        if (chatGame != null && !chatGame.isState()) {
                            chatGame.setState(true);
                            chatGameWasEnabled = true;
                        } else if (chatGame != null && chatGame.isState()) {
                            chatGameWasEnabled = true;
                        }
                    } catch (Exception ignored) {}
                }

                ChatMessage.brandmessage("FunnyGame MoneyFarm: запуск!");
                startFarmCycle();
                return;
            }

            sendCountdownMessage();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendCountdownMessage() {
        String msg = String.format(
                "Гайд: встаньте в АФК и не двигайтесь. При ПВП релоге модуль не работает. " +
                "Нужен донат на права телепорта. Запуск через %d...",
                countdownValue
        );
        ChatMessage.brandmessage(msg);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void startFarmCycle() {
        phase = FarmPhase.TELEPORT_TO_NPC1;
        phaseStartTime = System.currentTimeMillis();
        parsedBalance = 0;

        mc.player.networkHandler.sendChatCommand(TP_NPC1);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleTeleportToNpc1() {
        phase = FarmPhase.WAIT_TP1;
        phaseStartTime = System.currentTimeMillis();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaitTp1() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed >= TP_DELAY_MS) {
            interactWithNpc();
            phase = FarmPhase.INTERACT_NPC1;
            phaseStartTime = System.currentTimeMillis();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleInteractNpc1() {
        phase = FarmPhase.TELEPORT_TO_NPC2;
        phaseStartTime = System.currentTimeMillis();
        mc.player.networkHandler.sendChatCommand(TP_NPC2);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleTeleportToNpc2() {
        phase = FarmPhase.WAIT_TP2;
        phaseStartTime = System.currentTimeMillis();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaitTp2() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed >= TP_DELAY_MS) {
            interactWithNpc();
            phase = FarmPhase.INTERACT_NPC2;
            phaseStartTime = System.currentTimeMillis();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleInteractNpc2() {
        // Телепорт домой
        mc.player.networkHandler.sendChatCommand("home");
        phase = FarmPhase.TELEPORT_HOME;
        phaseStartTime = System.currentTimeMillis();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleTeleportHome() {
        phase = FarmPhase.WAIT_HOME;
        phaseStartTime = System.currentTimeMillis();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaitHome() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed >= TP_DELAY_MS) {
            // Задержка прошла — проверяем баланс
            mc.player.networkHandler.sendChatCommand("balance");
            phase = FarmPhase.WAITING_BALANCE_RESPONSE;
            phaseStartTime = System.currentTimeMillis();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaitingBalanceResponse() {
        // Ждём пока PacketEvent не распарсит баланс
        // Таймаут 5 секунд
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed >= 5000) {
            // Таймаут — идём дальше без перевода
            ChatMessage.brandmessage("Таймаут получения баланса, пропускаем перевод");
            startWaitingPhase();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processBalanceAndPay() {
        String target = transferTo.getText().trim();
        if (target.isEmpty() || parsedBalance <= 0) {
            ChatMessage.brandmessage("Никнейм не указан или баланс 0, пропускаем перевод");
            startWaitingPhase();
            return;
        }

        payTarget = target;
        ChatMessage.brandmessage("Переводим " + parsedBalance + "$ на аккаунт " + payTarget);

        // Отправляем первый /pay
        phase = FarmPhase.SENDING_PAY_1;
        phaseStartTime = System.currentTimeMillis();
        mc.player.networkHandler.sendChatMessage("pay " + payTarget + " " + parsedBalance);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleSendingPay1() {
        // Ждём подтверждение от сервера
        phase = FarmPhase.WAITING_PAY_CONFIRM;
        phaseStartTime = System.currentTimeMillis();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaitingPayConfirm() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed >= PAY_CONFIRM_DELAY_MS) {
            // Отправляем подтверждение — тот же pay повторно
            phase = FarmPhase.SENDING_PAY_2;
            phaseStartTime = System.currentTimeMillis();
            mc.player.networkHandler.sendChatMessage("pay " + payTarget + " " + parsedBalance);
            ChatMessage.brandmessage("Подтверждение перевода: pay " + payTarget + " " + parsedBalance);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleSendingPay2() {
        // Второй pay отправлен, ждём немного и идём в ожидание
        ChatMessage.brandmessage("Перевод выполнен: " + parsedBalance + "$ → " + payTarget);
        startWaitingPhase();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void startWaitingPhase() {
        phase = FarmPhase.WAITING;
        phaseStartTime = System.currentTimeMillis();
        ChatMessage.brandmessage("FunnyGame MoneyFarm: ожидание " + (int) farmInterval.getValue() + " мин...");
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaiting() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        long intervalMs = (long) farmInterval.getValue() * 60 * 1000;

        if (elapsed >= intervalMs) {
            startFarmCycle();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void interactWithNpc() {
        if (mc.player == null || mc.world == null) return;

        // Ищем ближайшего NPC (AntiBot распознаёт NPC как бота)
        PlayerEntity nearestNpc = null;
        double nearestDistance = Double.MAX_VALUE;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) continue;

            // Проверяем через AntiBot — NPC распознаются как боты
            AntiBot antiBot = AntiBot.getInstance();
            if (antiBot != null && antiBot.isBot(entity)) {
                double distance = mc.player.distanceTo(entity);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestNpc = entity;
                }
            }
        }

        if (nearestNpc != null) {
            // Взаимодействуем с NPC через пакет
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(nearestNpc, mc.player.isSneaking(), Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);
            ChatMessage.brandmessage("Взаимодействие с NPC: " + nearestNpc.getName().getString());
        } else {
            // Fallback — если NPC не найден, пробуем кликнуть вперёд
            Vec3d lookVec = mc.player.getRotationVec(1.0f);
            Vec3d hitVec = mc.player.getEyePos().add(lookVec.multiply(2.0));
            BlockPos interactPos = mc.player.getBlockPos();

            BlockHitResult hitResult = new BlockHitResult(
                    hitVec,
                    Direction.DOWN,
                    interactPos,
                    false
            );

            sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(
                    Hand.MAIN_HAND,
                    hitResult,
                    sequence
            ));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void interactWithBlock(BlockPos pos, Direction face) {
        if (mc.player == null || mc.world == null) return;

        Vec3d hitVec = Vec3d.ofCenter(pos);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                face,
                pos,
                false
        );

        sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                hitResult,
                sequence
        ));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void sendSequencedPacket(java.util.function.IntFunction<net.minecraft.network.packet.Packet<?>> packetCreator) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.world == null) return;

        try {
            ClientWorldAccessor worldAccessor = (ClientWorldAccessor) mc.world;
            PendingUpdateManager pendingUpdateManager = worldAccessor.getPendingUpdateManager().incrementSequence();

            int sequence = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.apply(sequence));

            pendingUpdateManager.close();
        } catch (Exception e) {
            mc.getNetworkHandler().sendPacket(packetCreator.apply(0));
        }
    }
}
