package rich.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoInteract extends ModuleStructure {
    public static NoInteract getInstance() {
        return Instance.get(NoInteract.class);
    }

    public NoInteract() {
        super("NoInteract", "No Interact", ModuleCategory.COMBAT);
    }
}
