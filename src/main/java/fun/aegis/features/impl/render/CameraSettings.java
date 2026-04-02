package fun.aegis.features.impl.render;

import fun.aegis.features.impl.player.FreeLook;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.*;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.Instance;
import fun.aegis.events.keyboard.HotBarScrollEvent;
import fun.aegis.events.keyboard.KeyEvent;
import fun.aegis.events.render.CameraEvent;
import fun.aegis.events.render.FovEvent;
import fun.aegis.utils.features.aura.utils.MathAngle;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraSettings extends Module {

    float fov = 110;
    float smoothFov = 30;
    float lastChangedFov = 30;

    // Плавный переход F5
    Perspective lastPerspective = Perspective.FIRST_PERSON;
    float smoothDistance = 0f;
    float targetDistance = 0f;

    BooleanSetting clipSetting = new BooleanSetting("Проход камеры", "Камера проходит сквозь блоки").setValue(true);
    SliderSettings distanceSetting = new SliderSettings("Дистанция камеры", "Настройка расстояния камеры")
            .setValue(3.0F).range(2.0F, 5.0F);
    BooleanSetting smoothTransitionSetting = new BooleanSetting("Плавный переход F5", "Плавная анимация при переключении режима камеры").setValue(true);
    SliderSettings transitionSpeedSetting = new SliderSettings("Скорость перехода", "Скорость плавного перехода камеры")
            .setValue(3.0F).range(1.0F, 10.0F).visible(smoothTransitionSetting::isValue);
    BindSetting zoomSetting = new BindSetting("Зум", "Клавиша для увеличения камеры");

    public CameraSettings() {
        super("CameraSettings", "Camera Settings", ModuleCategory.RENDER);
        setup(clipSetting, distanceSetting, smoothTransitionSetting, transitionSpeedSetting, zoomSetting);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(zoomSetting.getKey())) {
            fov = Math.min(lastChangedFov, mc.options.getFov().getValue() - 20);
        }
        if (e.isKeyReleased(zoomSetting.getKey(), true)) {
            lastChangedFov = fov;
            fov = mc.options.getFov().getValue();
        }
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (PlayerInteractionHelper.isKey(zoomSetting)) {
            fov = (int) MathHelper.clamp(fov - e.getVertical() * 10, 10, mc.options.getFov().getValue());
            e.cancel();
        }
    }

    @EventHandler
    public void onFov(FovEvent e) {
        e.setFov((int) MathHelper.clamp((smoothFov = Calculate.interpolateSmooth(1.6, smoothFov, fov)) + 1, 10, mc.options.getFov().getValue()));
        e.cancel();
    }

    @EventHandler
    public void onCamera(CameraEvent e) {
        e.setCameraClip(clipSetting.isValue());
        
        Perspective currentPerspective = mc.options.getPerspective();
        float maxDistance = distanceSetting.getValue();
        
        // Плавный переход F5
        if (smoothTransitionSetting.isValue()) {
            // Определяем целевую дистанцию
            if (currentPerspective.isFirstPerson()) {
                targetDistance = 0f;
            } else {
                targetDistance = maxDistance;
            }
            
            // Детектим переключение перспективы
            if (currentPerspective != lastPerspective) {
                // При переходе из первого лица - начинаем с 0
                if (lastPerspective.isFirstPerson() && !currentPerspective.isFirstPerson()) {
                    smoothDistance = 0f;
                }
                // При переходе в первое лицо - начинаем с текущей дистанции
                else if (!lastPerspective.isFirstPerson() && currentPerspective.isFirstPerson()) {
                    smoothDistance = maxDistance;
                }
                lastPerspective = currentPerspective;
            }
            
            // Плавная интерполяция
            smoothDistance = Calculate.interpolateSmooth(transitionSpeedSetting.getValue(), smoothDistance, targetDistance);
            
            // Устанавливаем дистанцию
            e.setDistance(Math.max(0.1f, smoothDistance));
        } else {
            e.setDistance(maxDistance);
        }
        
        FreeLook freeLook = Instance.get(FreeLook.class);
        if (!freeLook.isState() || !PlayerInteractionHelper.isKey(freeLook.freeLookSetting)) {
            e.setAngle(MathAngle.cameraAngle());
        }
        e.cancel();
    }
}
