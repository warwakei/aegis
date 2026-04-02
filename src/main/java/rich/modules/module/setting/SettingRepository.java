package rich.modules.module.setting;

import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingRepository implements Setupable {
    List<Setting> settings = Lists.newArrayList();

    @Override
    public final void settings(Setting... setting) {
        settings.addAll(Arrays.asList(setting));
    }

    public Setting get(String name) {
        return settings.stream()
                .filter(setting -> setting.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Setting> settings() {
        return settings;
    }
}
