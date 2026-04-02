package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import rich.events.api.EventHandler;
import rich.events.impl.CameraEvent;
import rich.events.impl.FovEvent;
import rich.events.impl.KeyEvent;
import rich.events.impl.MouseRotationEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.util.string.PlayerInteractionHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FreeLook extends ModuleStructure {

    Perspective perspective;
    Angle angle;
    public static BindSetting freeLookSetting = new BindSetting("Свободный обзор", "Клавиша для свободного обзора");

    public FreeLook() {
        super("FreeLook", "Free Look", ModuleCategory.RENDER);
        settings(freeLookSetting);
        angle = null;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(freeLookSetting.getKey())) {
            perspective = mc.options.getPerspective();
            if (angle == null) {
                angle = MathAngle.cameraAngle();
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onFov(FovEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting)) {
            handleFreeLookActivation();
        } else if (perspective != null) {
            handleFreeLookDeactivation();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleFreeLookActivation() {
        if (mc.options.getPerspective().isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        if (angle == null) {
            angle = MathAngle.cameraAngle();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleFreeLookDeactivation() {
        mc.options.setPerspective(perspective);
        perspective = null;
        angle = null;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onMouseRotation(MouseRotationEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting)) {
            if (angle == null) {
                angle = MathAngle.cameraAngle();
            }
            angle.setYaw(angle.getYaw() + e.getCursorDeltaX() * 0.15F);
            angle.setPitch(MathHelper.clamp(angle.getPitch() + e.getCursorDeltaY() * 0.15F, -90F, 90F));
            e.cancel();
        } else {
            angle = null;
        }
    }

    @EventHandler
    public void onCamera(CameraEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting) && angle != null) {
            e.setAngle(angle);
            e.cancel();
        }
    }
}