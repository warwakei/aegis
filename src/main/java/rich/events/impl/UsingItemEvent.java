package rich.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import rich.events.api.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class UsingItemEvent extends EventCancellable {
    byte type;
}
