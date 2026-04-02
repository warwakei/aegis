package rich.modules.module.setting.implement;

import rich.modules.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class SelectSetting extends Setting {
    @Setter
    private String selected;
    private List<String> list;

    public SelectSetting(String name, String description) {
        super(name, description);
    }

    public SelectSetting value(String... values) {
        this.list = Arrays.asList(values);
        this.selected = list.isEmpty() ? "" : list.get(0);
        return this;
    }

    public SelectSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public SelectSetting selected(String string) {
        if (list.contains(string)) {
            this.selected = string;
        }
        return this;
    }

    public boolean isSelected(String name) {
        return selected.equals(name);
    }
}