package rich.events.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import rich.events.api.events.callables.EventCancellable;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FovEvent extends EventCancellable {
    int fov;
}
