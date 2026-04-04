package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.mixin.ClientWorldAccessor;
import rich.modules.impl.misc.JenroChatGame;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.string.chat.ChatMessage;

/**
 * Автоматический фарм денег на FunnyGame через NPC каждые 30 минут
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FunnyGameMoneyFarm extends ModuleStructure {

    // Координаты NPC
    private static final String TP_NPC1 = "/tp -271 52 -1030";
    private static final String TP_NPC2 = "/tp -149 69 -1022";
    private static final String TP_HOME = "/home";
    private static final String CMD_BALANCE = "/balance";

    // Задержки
    private static final int TP_DELAY_MS = 320;
    private static final int FARM_INTERVAL_MS = 30 * 60 * 1000; // 30 минут

    final SliderSettings farmInterval = new SliderSettings("Интервал (мин)", "Интервал между фармами")
            .range(5, 60)
            .setValue(30);

    final BooleanSetting autoEnableChatGame = new BooleanSetting("Авто-ChatGame", "Автоматически включать Jenro ChatGame")
            .setValue(true);

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
        WAIT_TP_HOME,
        CHECK_BALANCE,
        WAITING
    }

    FarmPhase phase = FarmPhase.IDLE;
    long phaseStartTime = 0;
    int countdownValue = 10;
    boolean chatGameWasEnabled = false;

    public FunnyGameMoneyFarm() {
        super("FunnyGame MoneyFarm", "Автоматический фарм денег через NPC", ModuleCategory.MISC);
        settings(farmInterval, autoEnableChatGame);
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
            case WAIT_TP_HOME -> handleWaitTpHome();
            case CHECK_BALANCE -> handleCheckBalance();
            case WAITING -> handleWaiting();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleCountdown() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;

        // Обновляем сообщение каждую секунду
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
                            chatGameWasEnabled = true; // уже был включён
                        }
                    } catch (Exception ignored) {}
                }

                // Начинаем фарм
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
                "Чтобы использовать данный модуль вам нужно встать в АФК и не двигатся. " +
                "При получении ПВП релога модуль не будет работать. " +
                "Вам так же нужен донат на сервере, ибо нужны права на телепорт по координатам. " +
                "Включение через %d...",
                countdownValue
        );
        mc.player.networkHandler.sendChatMessage(msg);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void startFarmCycle() {
        phase = FarmPhase.TELEPORT_TO_NPC1;
        phaseStartTime = System.currentTimeMillis();

        // Телепорт к первому NPC
        mc.player.networkHandler.sendChatCommand("tp -271 52 -1030");
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
            // ПКМ по NPC
            interactWithNpc();
            phase = FarmPhase.INTERACT_NPC1;
            phaseStartTime = System.currentTimeMillis();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleInteractNpc1() {
        // Сразу переходим ко второму NPC
        phase = FarmPhase.TELEPORT_TO_NPC2;
        phaseStartTime = System.currentTimeMillis();
        mc.player.networkHandler.sendChatCommand("tp -149 69 -1022");
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
            // ПКМ по NPC
            interactWithNpc();
            phase = FarmPhase.INTERACT_NPC2;
            phaseStartTime = System.currentTimeMillis();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleInteractNpc2() {
        // Телепорт домой
        phase = FarmPhase.TELEPORT_HOME;
        phaseStartTime = System.currentTimeMillis();
        mc.player.networkHandler.sendChatCommand("home");
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleTeleportHome() {
        phase = FarmPhase.WAIT_TP_HOME;
        phaseStartTime = System.currentTimeMillis();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaitTpHome() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed >= TP_DELAY_MS) {
            // Проверяем баланс
            phase = FarmPhase.CHECK_BALANCE;
            phaseStartTime = System.currentTimeMillis();
            mc.player.networkHandler.sendChatCommand("balance");
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleCheckBalance() {
        // Ждём и начинаем цикл заново через 30 минут
        phase = FarmPhase.WAITING;
        phaseStartTime = System.currentTimeMillis();
        ChatMessage.brandmessage("FunnyGame MoneyFarm: ожидание " + (int) farmInterval.getValue() + " мин...");
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleWaiting() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        long intervalMs = (long) farmInterval.getValue() * 60 * 1000;

        if (elapsed >= intervalMs) {
            // Начинаем цикл заново
            startFarmCycle();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void interactWithNpc() {
        if (mc.player == null || mc.world == null) return;

        // Ищем ближайший интерактивный блок/сущность для ПКМ
        BlockPos interactPos = mc.player.getBlockPos();

        // Пробуем взаимодействовать с блоком перед собой
        for (Direction dir : Direction.values()) {
            BlockPos pos = interactPos.offset(dir);
            if (!mc.world.getBlockState(pos).isAir()) {
                interactWithBlock(pos, dir.getOpposite());
                return;
            }
        }

        // Если нет блоков — кликаем в воздух перед собой
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Vec3d hitVec = mc.player.getEyePos().add(lookVec.multiply(2.0));

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

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendChatCommand(String command) {
        if (mc.player != null && mc.player.networkHandler != null) {
            if (command.startsWith("/")) {
                mc.player.networkHandler.sendChatCommand(command.substring(1));
            } else {
                mc.player.networkHandler.sendChatCommand(command);
            }
        }
    }
}
