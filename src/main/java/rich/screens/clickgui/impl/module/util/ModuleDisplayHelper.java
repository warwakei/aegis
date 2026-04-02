package rich.screens.clickgui.impl.module.util;

import rich.modules.module.ModuleStructure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleDisplayHelper {

    private final Set<ModuleStructure> modulesWithSettings = new HashSet<>();

    public void updateModulesWithSettings(List<ModuleStructure> displayModules) {
        modulesWithSettings.clear();
        for (ModuleStructure mod : displayModules) {
            if (hasModuleSettings(mod)) {
                modulesWithSettings.add(mod);
            }
        }
    }

    public boolean hasModuleSettings(ModuleStructure module) {
        if (module == null) return false;
        var settings = module.settings();
        return settings != null && !settings.isEmpty();
    }

    public boolean hasSettings(ModuleStructure module) {
        return modulesWithSettings.contains(module);
    }
}