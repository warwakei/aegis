package rich.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rich.events.api.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}
