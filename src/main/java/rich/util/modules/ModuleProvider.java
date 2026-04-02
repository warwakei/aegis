package rich.util.modules;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import rich.modules.module.ModuleStructure;

import java.util.List;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleProvider {
    List<ModuleStructure> moduleStructures;

    @SuppressWarnings("unchecked")
    public <T extends ModuleStructure> T get(final String name) {
        return moduleStructures.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .map(module -> (T) module)
                .findFirst()
                .orElse(null);
    }

    public <T extends ModuleStructure> T get(final Class<T> clazz) {
        return moduleStructures.stream()
                .filter(module -> clazz.isAssignableFrom(module.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}
