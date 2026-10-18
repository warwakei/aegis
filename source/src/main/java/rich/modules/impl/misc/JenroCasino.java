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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

    final SliderSettings interactReach = new SliderSettings("Дальность интеракта (блоки)", "Через сколько блоков можно слать Interact рычагу")
            .range(1, 35)
            .setValue(8);

    long lastClickTime = 0;
    boolean sat = false;

    // задержка перед отправкой /sit
    private static final long SIT_DELAY_MS = 800;
    private static final long POST_JUMP_DELAY_1 = 350;
    private static final long POST_JUMP_DELAY_2 = 440;

    // ждали завершения цикла FunnyGame MoneyFarm (чтобы ре-/sit отправить один раз)
    boolean waitedForFarmCycle = false;

    // /sit отправляется не сразу, а через seqвенцию: 800мс → прыжок → 350 → прыжок → 440 → /sit
    // sitStage: 0 = нет ожидания, 1 = ждём 800, 2 = ждём 350 после 1-го прыжка, 3 = ждём 440 после 2-го прыжка
    long sitScheduleTime = 0;
    int sitStage = 0;

    public JenroCasino() {
        super("Jenro Casino", "Jenro Casino AFK", ModuleCategory.JENRO);
        settings(clickDelay, rotate, interactReach);
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

        // пишем sit при вкл, но через секвенцию с прыжками
        if (mc.player != null) {
            sitScheduleTime = System.currentTimeMillis() + SIT_DELAY_MS;
            ChatMessage.brandmessage("Jenro Casino: /sit через 800 мс (с прыжками)...");
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        sat = false;
        lastClickTime = 0;
        waitedForFarmCycle = false;
        sitScheduleTime = 0;
        sitStage = 0;
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

        // секвенция: 800мс → прыжок → 350мс → прыжок → 440мс → /sit
        if (sitStage > 0) {
            if (currentTime >= sitScheduleTime) {
                switch (sitStage) {
                    case 1 -> {
                        sitStage = 2;
                        sitScheduleTime = currentTime + POST_JUMP_DELAY_1;
                        jump();
                    }
                    case 2 -> {
                        sitStage = 3;
                        sitScheduleTime = currentTime + POST_JUMP_DELAY_2;
                        jump();
                    }
                    default -> {
                        sitStage = 0;
                        sitScheduleTime = 0;
                        mc.player.networkHandler.sendChatCommand("sit");
                        sat = true;
                        ChatMessage.brandmessage("Jenro Casino: /sit отправлен");
                    }
                }
            }
            return; // пока секвенция не доиграла — рычаг не тыкаем
        }

        // FunnyGame MoneyFarm запустил цикл (tp → NPC → back → pay):
        // прячем тык рычага, ждём пока цикл уйдёт на кулдаун,
        // и как только ушёл (WAITING) — один раз секвенцию sit-а запускаем опять
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
                sitStage = 1;
                sitScheduleTime = currentTime + SIT_DELAY_MS;
                ChatMessage.brandmessage("Jenro Casino: фарм завершён, /sit через 800 мс...");
            }
        }

        long delay = (long) clickDelay.getValue();

        if (currentTime - lastClickTime < delay) {
            return;
        }

        // ищем рычаг рядом (в пределах дальности интеракта с сервера)
        BlockPos leverPos = findNearestLever();

        if (leverPos == null) {
            // рычага в дальности нет — никаких интеракт-пакетов,
            // античит с урезанным reach-конфигом такое флагает
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

    /**
     * Рычаг реально виден от глаз игрока (не закрыт стеной).
     */
    private boolean isLeverVisible(BlockPos leverPos) {
        if (mc.player == null || mc.world == null) return false;
        try {
            BlockHitResult rayHit = mc.world.raycast(new RaycastContext(
                    mc.player.getEyePos(),
                    Vec3d.ofCenter(leverPos),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));
            // луч должен упереться ровно в этот рычаг (или в воздух в его зоне)
            if (rayHit == null) return false;
            if (rayHit.getType() == HitResult.Type.MISS) return true;
            return mc.world.getBlockState(rayHit.getBlockPos()).getBlock() == Blocks.LEVER;
        } catch (Exception e) {
            // не смогли протрейсить — считаем видимым, поведение как раньше
            return true;
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private BlockPos findNearestLever() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();

        // дальность интеракта с сервера — за пределами не шлём Interact вообще
        int searchRadius = Math.min(35, (int) interactReach.getValue());
        double maxReach = interactReach.getValue();

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
                        if (distance <= maxReach && distance < nearestDistance) {
                            // рычаг должен быть виден от глаз: если перед ним стена — не наш клиент
                            if (isLeverVisible(pos)) {
                                nearestDistance = distance;
                                nearestLever = pos;
                            }
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

        // точка клика — на грани блока (той, что валидится сервером), а не в центре куба
        org.joml.Vector3f faceVec = clickDirection.getUnitVector();
        Vec3d faceNormal = new Vec3d(faceVec.x, faceVec.y, faceVec.z);
        Vec3d hitVec = Vec3d.ofCenter(leverPos).add(faceNormal.multiply(0.5));

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

    /** прыжок для секвенции /sit (подняться с места перед повторной посадкой) */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void jump() {
        if (mc.player == null) return;
        mc.player.jump();
    }
}
