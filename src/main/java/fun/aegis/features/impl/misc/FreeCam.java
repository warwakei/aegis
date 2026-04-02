package fun.aegis.features.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.interactions.simulate.Simulations;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.*;
import fun.aegis.events.render.CameraPositionEvent;
import fun.aegis.events.render.WorldRenderEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FreeCam extends Module {
    public static FreeCam getInstance() {
        return Instance.get(FreeCam.class);
    }

    private final SliderSettings speedSetting = new SliderSettings("Скорость", "Выберите скорость камеры отладки").setValue(2.0F).range(0.5F, 5.0F);
    private final BooleanSetting freezeSetting = new BooleanSetting("Заморозка", "Вы замораживаетесь на месте").setValue(false);
    public Vec3d pos, prevPos;

    public FreeCam() {
        super("FreeCam", "Free Cam", ModuleCategory.MISC);
        setup(speedSetting, freezeSetting);
    }

    
    @Override
    public void activate() {
        prevPos = pos = new Vec3d(mc.getEntityRenderDispatcher().camera.getPos().toVector3f());
        super.activate();
    }

    
    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case PlayerMoveC2SPacket move when freezeSetting.isValue() -> e.cancel();
            case PlayerRespawnS2CPacket respawn -> setState(false);
            case GameJoinS2CPacket join -> setState(false);
            default -> {}
        }
    }

    
    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        Render3D.drawBox(mc.player.getBoundingBox().offset(Calculate.interpolate(mc.player).subtract(mc.player.getPos())), -1, 1);
    }

    
    @EventHandler
    public void onMove(MoveEvent e) {
        if (freezeSetting.isValue()) {
            e.setMovement(Vec3d.ZERO);
        }
    }

    
    @EventHandler
    public void onInput(InputEvent e) {
        float speed = speedSetting.getValue();
        double[] motion = Simulations.calculateDirection(e.forward(), e.sideways(), speed);

        prevPos = pos;
        pos = pos.add(motion[0], e.getInput().jump() ? speed : e.getInput().sneak() ? -speed : 0, motion[1]);

        e.inputNone();
    }

    
    @EventHandler
    public void onCameraPosition(CameraPositionEvent e) {
        e.setPos(Calculate.interpolate(prevPos, pos));
        mc.options.setPerspective(Perspective.FIRST_PERSON);
    }
}
