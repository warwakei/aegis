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
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.mixin.ClientWorldAccessor;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
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

    public JenroCasino() {
        super("Jenro Casino", "Jenro Casino AFK", ModuleCategory.MISC);
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

        // Прописываем /sit при включении
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand("sit");
            sat = true;
            ChatMessage.brandmessage("Jenro Casino: /sit отправлен");
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        sat = false;
        lastClickTime = 0;
        ChatMessage.brandmessage("Jenro Casino: остановлено");
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            setState(false);
            return;
        }

        long delay = (long) clickDelay.getValue();
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastClickTime < delay) {
            return;
        }

        // Ищем ближайший рычаг
        BlockPos leverPos = findNearestLever();

        if (leverPos == null) {
            // Если рычаг не найден - пробуем кликать в воздух (для некоторых казино)
            clickInAir();
            lastClickTime = currentTime;
            return;
        }

        // Поворот к рычагу если включено
        if (rotate.isValue()) {
            rotateToLever(leverPos);
        }

        // Кликаем по рычагу
        clickLever(leverPos);
        lastClickTime = currentTime;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getType() != PacketEvent.Type.RECEIVE) return;
        if (!(e.getPacket() instanceof ChatMessageS2CPacket packet)) return;

        try {
            String text = extractChatText(packet);
            if (text == null) return;
            String stripped = stripColorCodes(text);
            // Скрываем сообщения "Машина занята, пожалуйста подождите."
            if (stripped.contains("Машина занята")) {
                e.cancel();
            }
        } catch (Exception ignored) {}
    }

    private String extractChatText(ChatMessageS2CPacket packet) {
        try {
            // Search fields for Text type
            for (var f : packet.getClass().getDeclaredFields()) {
                if (net.minecraft.text.Text.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(packet);
                    if (val instanceof net.minecraft.text.Text t) {
                        return t.getString();
                    }
                }
            }
            // Also try methods
            for (var m : packet.getClass().getMethods()) {
                if (net.minecraft.text.Text.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    Object result = m.invoke(packet);
                    if (result instanceof net.minecraft.text.Text t) {
                        return t.getString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String stripColorCodes(String text) {
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private BlockPos findNearestLever() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        int searchRadius = 8; // Увеличил с 5 до 8 для лучшего поиска

        BlockPos nearestLever = null;
        double nearestDistance = Double.MAX_VALUE;

        // Ищем рычаги в радиусе
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 3; y++) { // Ограничил Y диапазон (рычаги обычно рядом)
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

        // Определяем направление клика
        Direction direction = Direction.getFacing(
                mc.player.getX() - leverPos.getX(),
                mc.player.getY() - leverPos.getY(),
                mc.player.getZ() - leverPos.getZ()
        );

        // Получаем противоположное направление для клика
        Direction clickDirection = direction.getOpposite();

        Vec3d hitVec = Vec3d.ofCenter(leverPos);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                clickDirection,
                leverPos,
                false
        );

        // Отправляем пакет клика
        sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                hitResult,
                sequence
        ));

        // Анимация качания руки
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void clickInAir() {
        if (mc.player == null || mc.world == null) return;

        // Кликаем перед собой (для казино где нет рычага или он в другом месте)
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
