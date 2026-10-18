package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.mixin.ClientWorldAccessor;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.string.chat.ChatMessage;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JenroCasino extends ModuleStructure {

    final SliderSettings clickDelay = new SliderSettings("Задержка (мс)", "Задержка между кликами")
            .range(10, 100)
            .setValue(20);

    final BooleanSetting rotate = new BooleanSetting("Ротация", "Поворачиваться к рычагу")
            .setValue(true);

    long lastClickTime = 0;
    boolean sat = false;

    // задержка перед отправкой /sit
    private static final long SIT_DELAY_MS = 800;

    // ждали завершения цикла FunnyGame MoneyFarm (чтобы ре-/sit отправить один раз)
    boolean waitedForFarmCycle = false;

    // /sit отправляется не сразу, а с задержкой SIT_DELAY_MS
    long sitScheduleTime = 0;

    public JenroCasino() {
        super("Jenro Casino", "Jenro Casino AFK", ModuleCategory.JENRO);
        settings(clickDelay, rotate);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        if (mc.player == null || mc.world == null) {
            setState(false);
            return;
        }

        sat = false;
        lastClickTime = 0;
        waitedForFarmCycle = false;

        // пишем sit при вкл, но с задержкой 800 мс
        if (mc.player != null) {
            sitScheduleTime = System.currentTimeMillis() + SIT_DELAY_MS;
            ChatMessage.brandmessage("Jenro Casino: /sit через 800 мс...");
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        sat = false;
        lastClickTime = 0;
        waitedForFarmCycle = false;
        sitScheduleTime = 0;
        ChatMessage.brandmessage("Jenro Casino: остановлено");
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            setState(false);
            return;
        }

        long currentTime = System.currentTimeMillis();

        // ожидание отложенного /sit (800 мс)
        if (sitScheduleTime != 0) {
            if (currentTime >= sitScheduleTime) {
                sitScheduleTime = 0;
                mc.player.networkHandler.sendChatCommand("sit");
                sat = true;
                ChatMessage.brandmessage("Jenro Casino: /sit отправлен");
            }
            return; // пока /sit не улетел — рычаг не тыкаем
        }

        // FunnyGame MoneyFarm запустил цикл (tp → NPC → back → pay):
        // прячем тык рычага, ждём пока цикл уйдёт на кулдаун,
        // и как только ушёл (WAITING) — один раз пишем /sit опять (тоже с задержкой 800 мс)
        FunnyGameMoneyFarm moneyFarm = Instance.get(FunnyGameMoneyFarm.class);
        if (moneyFarm != null && moneyFarm.isState()) {
            if (moneyFarm.isFarmingCycle()) {
                if (!waitedForFarmCycle) {
                    waitedForFarmCycle = true;
                    ChatMessage.brandmessage("Jenro Casino: фарм-цикл активен, ждём кулдауна...");
                }
                return;
            }
            if (waitedForFarmCycle) {
                waitedForFarmCycle = false;
                sitScheduleTime = currentTime + SIT_DELAY_MS;
                ChatMessage.brandmessage("Jenro Casino: фарм завершён, /sit через 800 мс...");
            }
        }

        long delay = (long) clickDelay.getValue();

        if (currentTime - lastClickTime < delay) {
            return;
        }

        // ищем рычаг рядом
        BlockPos leverPos = findNearestLever();

        if (leverPos == null) {
            // рычага нет тыкаем воздух
            clickInAir();
            lastClickTime = currentTime;
            return;
        }

        // крутимся к рычагу
        if (rotate.isValue()) {
            rotateToLever(leverPos);
        }

        // жмем рычаг
        clickLever(leverPos);
        lastClickTime = currentTime;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getType() != PacketEvent.Type.RECEIVE) return;

        try {
            // казино спамит через system_chat, игровой чат тут не при чём
            if (e.getPacket() instanceof GameMessageS2CPacket gameMessage) {
                if (isCasinoSpam(gameMessage.content())) {
                    e.cancel();
                }
                return;
            }

            if (e.getPacket() instanceof ChatMessageS2CPacket chatMessage) {
                if (chatMessage.body() != null && isCasinoSpam(chatMessage.body().content())) {
                    e.cancel();
                }
            }
        } catch (Exception ignored) {}
    }

    // прячем спам машина занята
    private boolean isCasinoSpam(Text text) {
        if (text == null) return false;
        try {
            return isCasinoSpam(text.getString());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isCasinoSpam(String text) {
        if (text == null || text.isEmpty()) return false;
        return stripColorCodes(text).contains("Машина занята");
    }

    private String stripColorCodes(String text) {
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private BlockPos findNearestLever() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        int searchRadius = 8; // радик поболе для поиска

        BlockPos nearestLever = null;
        double nearestDistance = Double.MAX_VALUE;

        // рычаги рядом ищем
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 3; y++) { // игрек режем рычаги рядом
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    var state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.LEVER) {
                        double distance = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestLever = pos;
                        }
                    }
                }
            }
        }

        return nearestLever;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void rotateToLever(BlockPos leverPos) {
        if (mc.player == null || leverPos == null) return;

        Vec3d leverVec = Vec3d.ofCenter(leverPos);
        Vec3d playerVec = mc.player.getEyePos();

        Vec3d diff = leverVec.subtract(playerVec);
        double horizontalDistance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontalDistance));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void clickLever(BlockPos leverPos) {
        if (mc.player == null || mc.world == null || leverPos == null) return;

        // куда тыкать решаем
        Direction direction = Direction.getFacing(
                mc.player.getX() - leverPos.getX(),
                mc.player.getY() - leverPos.getY(),
                mc.player.getZ() - leverPos.getZ()
        );

        // наоборот для клика
        Direction clickDirection = direction.getOpposite();

        Vec3d hitVec = Vec3d.ofCenter(leverPos);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                clickDirection,
                leverPos,
                false
        );

        // шлем пакет клика
        sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                hitResult,
                sequence
        ));

        // машем рукой
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void clickInAir() {
        if (mc.player == null || mc.world == null) return;

        // тыкаем перед собой
        Vec3d lookVec = mc.player.getRotationVecClient();
        Vec3d hitVec = mc.player.getEyePos().add(lookVec.multiply(3));

        Direction direction = Direction.getFacing(lookVec.x, lookVec.y, lookVec.z);

        BlockPos pos = mc.player.getBlockPos().offset(direction);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                direction.getOpposite(),
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
