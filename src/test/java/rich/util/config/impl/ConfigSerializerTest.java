package rich.util.config.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.implement.SliderSettings;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ConfigSerializerTest {
    private final ConfigSerializer serializer = new ConfigSerializer();

    private static class ProbeModule extends ModuleStructure {
        final SliderSettings slider = new SliderSettings("value", "").range(1, 10).setValue(2);
        float valueOnActivation;
        ProbeModule() { super("Probe", ModuleCategory.MISC); settings(slider); }
        @Override public void setState(boolean enabled) { if (enabled) valueOnActivation = slider.getValue(); }
    }

    @Test public void activationSeesLoadedSettings() throws Exception {
        ProbeModule module = new ProbeModule();
        Method method = ConfigSerializer.class.getDeclaredMethod("deserializeModule", ModuleStructure.class, JsonObject.class);
        method.setAccessible(true);
        method.invoke(serializer, module, JsonParser.parseString("{\"enabled\":true,\"settings\":{\"value\":8}}").getAsJsonObject());
        assertEquals(8, module.valueOnActivation, 0);
    }

    private void loadSlider(SliderSettings slider, String json) throws Exception {
        Method method = ConfigSerializer.class.getDeclaredMethod("deserializeSetting", Setting.class, com.google.gson.JsonElement.class);
        method.setAccessible(true);
        method.invoke(serializer, slider, JsonParser.parseString(json));
    }

    @Test public void outOfRangeSlidersAreClamped() throws Exception {
        SliderSettings slider = new SliderSettings("test", "").range(1, 10).setValue(5);
        loadSlider(slider, "99999");
        assertEquals(10, slider.getValue(), 0);
        loadSlider(slider, "-99999");
        assertEquals(1, slider.getValue(), 0);
    }

    @Test public void nonFiniteSliderKeepsPreviousValue() throws Exception {
        SliderSettings slider = new SliderSettings("test", "").range(1, 10).setValue(5);
        loadSlider(slider, "\"NaN\"");
        assertEquals(5, slider.getValue(), 0);
        loadSlider(slider, "1e999");
        assertEquals(5, slider.getValue(), 0);
    }

    @Test public void invalidJsonDoesNotReportSuccessfulLoad() {
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize("{broken"));
    }
}
