package rich.modules.module;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleBuilder {
    ModuleRepository repository;

    public ModuleBuilder add(ModuleStructure module) {
        repository.registerModule(module, false);
        return this;
    }

    public ModuleBuilder hidden(ModuleStructure module) {
        repository.registerModule(module, true);
        return this;
    }
}