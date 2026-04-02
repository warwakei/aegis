package fun.aegis.utils.client;

import lombok.experimental.UtilityClass;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.features.module.Module;
import fun.aegis.Aegis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

    public <T extends Module> T get(Class<T> clazz) {
        return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> Aegis.getInstance().getModuleProvider().get(instance)));
    }

    public <T extends Module> T get(String module) {
        return Aegis.getInstance().getModuleProvider().get(module);
    }

    public <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
        return clazz.cast(instanceDraggables.computeIfAbsent(clazz, instance -> Aegis.getInstance().getDraggableRepository().get(instance)));
    }

    public <T extends AbstractDraggable> T getDraggable(String draggable) {
        return Aegis.getInstance().getDraggableRepository().get(draggable);
    }
}
