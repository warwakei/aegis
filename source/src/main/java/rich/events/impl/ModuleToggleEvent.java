package rich.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rich.events.api.events.Event;
import rich.modules.module.ModuleStructure;

@Getter
@AllArgsConstructor
public class ModuleToggleEvent implements Event {
    private final ModuleStructure module;
    private final boolean enabled;
}