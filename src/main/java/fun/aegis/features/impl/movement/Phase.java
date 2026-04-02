package fun.aegis.features.impl.movement;

import java.util.ArrayList;
import java.util.List;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render3D;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Phase extends Module {
    private final List<Packet<?>> list = new ArrayList<>();
    private Box box;
    private final SelectSetting mode;

    public Phase() {
        super("Phase", ModuleCategory.MOVEMENT);
        this.box = new Box(BlockPos.ORIGIN);
        this.mode = new SelectSetting("Mode", "Selects the type of mode").value("ReallyWorld");
        this.setup(new Setting[]{this.mode});
    }

    @Native
    @Override
    public void activate() {
        // Проверка версии Minecraft (1.17-1.20.6)
        int version = SharedConstants.getGameVersion().getProtocolVersion();
        if (version < 755 || version > 766) {
            // Уведомление о несовместимой версии
            setState(false);
        }
    }

    @Override
    public void deactivate() {
        // Отправляем пакет для сброса позиции при выключении
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(0.0D, -999.0D, 0.0D, false, false));
        // Отправляем все сохраненные пакеты
        this.list.forEach(packet -> mc.player.networkHandler.sendPacket(packet));
        this.list.clear();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(0.0D, -999.0D, 0.0D, false, false));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!this.list.isEmpty()) {
            // Рисуем хитбокс фазового состояния
            Render3D.drawBox(this.box, ColorAssist.getClientColor(), 1.0F);
        }
    }

    @EventHandler
    @Native
    public void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        
        // Определяем тип пакета и обрабатываем
        if (packet instanceof PlayerRespawnS2CPacket) {
            // При респавне отключаем модуль
            setState(false);
            return;
        } else if (packet instanceof EntityStatusS2CPacket) {
            // При получении статуса сущности отключаем модуль
            setState(false);
            return;
        } else if (packet instanceof GameJoinS2CPacket) {
            // При присоединении к игре отключаем модуль
            setState(false);
            return;
        } else if (packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket status = (PlayerPositionLookS2CPacket) packet;
            // Отключаем при обычном обновлении позиции
            setState(false);
            return;
        } else if (packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket move = (PlayerMoveC2SPacket) packet;
            // Сохраняем пакеты движения для фазового состояния
            if (!move.isOnGround() && this.list.isEmpty()) {
                return;
            }
            this.list.add(move);
            e.setCancelled(true);
            return;
        } else if (packet instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket setBack = (BlockUpdateS2CPacket) packet;
            // Обновляем хитбокс при изменении блоков
            this.box = mc.player.getBoundingBox().stretch(
                new Vec3d(setBack.getPos().getX(), setBack.getPos().getY(), setBack.getPos().getZ())
                    .subtract(new Vec3d(mc.player.getBlockPos().getX(), mc.player.getBlockPos().getY(), mc.player.getBlockPos().getZ()))
            );
            return;
        }
    }

    @EventHandler
    @Native
    public void onTick(TickEvent e) {
        if (this.list.isEmpty()) {
            // Используем обычный хитбокс игрока
            this.box = mc.player.getBoundingBox();
        } else {
            // При фазовом движении игнорируем столкновения
            mc.player.noClip = true;
            mc.player.getAbilities().flying = false;
        }
    }
}
