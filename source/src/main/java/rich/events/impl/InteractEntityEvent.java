package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import rich.events.api.events.callables.EventCancellable;

@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InteractEntityEvent extends EventCancellable {
    Entity entity;
}
