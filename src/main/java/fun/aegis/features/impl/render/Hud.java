package fun.aegis.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.Instance;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends Module {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса").value("Вотермарка", "Кейбинды", "Эффекты", "Список стафа", "Таргет худ", "Бинды", "Кулдауны", "Инвентарь", "Инфо игрока", "Уведомления", "Dynamic Island", "Броня", "Хотбар", "Скорборд")
            .selected("Кейбинды", "Эффекты", "Список стафа", "Таргет худ", "Бинды", "Кулдауны", "Инвентарь", "Инфо игрока", "Уведомления", "Dynamic Island");

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Уведомления", "Выберите, когда будут появляться уведомления")
            .value("Module Switch", "Staff Join", "Staff Leave", "Item Pick Up", "Auto Armor", "Break Shield", "Player Kill").selected("Item Pick Up", "Auto Armor", "Break Shield").visible(()-> interfaceSettings.isSelected("Уведомления"));

    public MultiSelectSetting dynamicIslandSettings = new MultiSelectSetting("DI Уведомления", "Уведомления в Dynamic Island")
            .value( "Авто свап", "FreeCam", "Авто тотем", "Элитра свап", "Новый игрок", "Amoled")
            .selected( "Авто свап", "FreeCam", "Авто тотем", "Элитра свап", "Новый игрок")
            .visible(() -> interfaceSettings.isSelected("Dynamic Island"));

    public ColorSetting colorSetting = new ColorSetting("Изменяет цвет некоторых модулей", "Выберите цвет клиента")
            .setColor(new Color(255, 101, 57, 255).getRGB()).presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B);

    public SliderSettings guiScale = new SliderSettings("GUI Scale", "Размер интерфейса Click GUI")
            .range(0.7f, 1.0f)
            .setValue(1.0f);
    
    public SliderSettings soundVolumeSetting = new SliderSettings("Sound Volume", "Volume for module switch sounds")
            .range(0.0f, 1.0f)
            .setValue(1.0f)
            .visible(() -> interfaceSettings.isSelected("Уведомления"));

    public float getModuleVolume() {
        return soundVolumeSetting.getValue();
    }

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup(colorSetting, interfaceSettings, notificationSettings, dynamicIslandSettings, guiScale, soundVolumeSetting);
    }
}
