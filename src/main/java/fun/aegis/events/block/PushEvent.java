package fun.aegis.events.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import fun.aegis.utils.client.managers.event.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}
