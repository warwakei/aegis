package rich.util;

import lombok.experimental.UtilityClass;
import rich.Initialization;
import rich.modules.module.ModuleStructure;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  © 2025 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends ModuleStructure>, ModuleStructure> instanceModules = new ConcurrentHashMap<>();

    public <T extends ModuleStructure> T get(Class<T> clazz) {
        return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> Initialization.getInstance().getManager().getModuleProvider().get(instance)));
    }

    public <T extends ModuleStructure> T get(String module) {
        return Initialization.getInstance().getManager().getModuleProvider().get(module);
    }

}
