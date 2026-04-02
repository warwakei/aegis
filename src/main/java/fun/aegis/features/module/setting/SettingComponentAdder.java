package fun.aegis.features.module.setting;

import fun.aegis.features.module.setting.implement.*;
 import fun.aegis.display.screens.clickgui.components.implement.settings.*;
import fun.aegis.display.screens.clickgui.components.implement.settings.multiselect.MultiSelectComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.select.SelectComponent;

import java.util.List;

public class SettingComponentAdder {
    public void addSettingComponent(List<Setting> settings, List<AbstractSettingComponent> components) {
        settings.forEach(setting -> {
            if (setting instanceof BooleanSetting booleanSetting) {
                components.add(new CheckboxComponent(booleanSetting));
            }

            if (setting instanceof BindSetting bindSetting) {
                components.add(new BindComponent(bindSetting));
            }

            if (setting instanceof ColorSetting colorSetting) {
                components.add(new ColorComponent(colorSetting));
            }

            if (setting instanceof TextSetting textSetting) {
                components.add(new TextComponent(textSetting));
            }

            if (setting instanceof SliderSettings sliderSetting) {
                components.add(new SliderComponent(sliderSetting));
            }

            if (setting instanceof GroupSetting groupSetting) {
                components.add(new GroupComponent(groupSetting));
            }

            if (setting instanceof ButtonSetting buttonSetting) {
                components.add(new SButtonComponent(buttonSetting));
            }

            if (setting instanceof SelectSetting selectSetting) {
                components.add(new SelectComponent(selectSetting));
            }

            if (setting instanceof MultiSelectSetting multiSelectSetting) {
                components.add(new MultiSelectComponent(multiSelectSetting));
            }
        });
    }
}
