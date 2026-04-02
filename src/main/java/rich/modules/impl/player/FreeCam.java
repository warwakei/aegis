package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.*;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.Instance;
import rich.util.math.MathUtils;
import rich.util.move.MoveUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FreeCam extends ModuleStructure {
    public static FreeCam getInstance() {
        return Instance.get(FreeCam.class);
    }

    private final SliderSettings speedSetting = new SliderSettings("Скорость", "Выберите скорость камеры отладки").setValue(2.0F).range(0.5F, 5.0F);
    private final BooleanSetting freezeSetting = new BooleanSetting("Заморозка", "Вы замораживаетесь на месте").setValue(false);
    private final BooleanSetting reloadChunksSetting = new BooleanSetting("Reload Chunks", "Отключает cave culling").setValue(true);
    private final BooleanSetting toggleOnLogSetting = new BooleanSetting("Toggle On Log", "Выключает при дисконнекте").setValue(true);

    public final ColorSetting fakeplayer = new ColorSetting("Color 1", "First gradient color")
            .value(ColorUtil.getColor(255, 50, 100, 255));

    public Vec3d pos, prevPos;

    public FreeCam() {
        super("FreeCam", "Free Cam", ModuleCategory.PLAYER);
        settings(speedSetting, freezeSetting, reloadChunksSetting, toggleOnLogSetting);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        prevPos = pos = mc.getEntityRenderDispatcher().camera.getCameraPos();

        if (reloadChunksSetting.isValue()) {
            mc.worldRenderer.reload();
        }

        super.activate();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        if (reloadChunksSetting.isValue()) {
            mc.execute(mc.worldRenderer::reload);
        }

        super.deactivate();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case PlayerMoveC2SPacket move when freezeSetting.isValue() -> e.cancel();
            case PlayerRespawnS2CPacket respawn -> setState(false);
            case GameJoinS2CPacket join -> setState(false);
            default -> {}
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onMove(MoveEvent e) {
        if (freezeSetting.isValue()) {
            e.setMovement(Vec3d.ZERO);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onInput(InputEvent e) {
        float speed = speedSetting.getValue();
        double[] motion = MoveUtil.calculateDirection(e.forward(), e.sideways(), speed);

        prevPos = pos;
        pos = pos.add(motion[0], e.getInput().jump() ? speed : e.getInput().sneak() ? -speed : 0, motion[1]);

        e.inputNone();
    }

    @EventHandler
    public void onCameraPosition(CameraPositionEvent e) {
        e.setPos(MathUtils.interpolate(prevPos, pos));
        mc.options.setPerspective(Perspective.FIRST_PERSON);
    }

    @EventHandler
    public void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onGameLeft(GameLeftEvent event) {
        if (toggleOnLogSetting.isValue()) {
            setState(false);
        }
    }
}