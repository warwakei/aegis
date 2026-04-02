package rich.events.api.events.callables;

import rich.events.api.events.Event;
import rich.events.api.events.Typed;

public abstract class EventTyped implements Event, Typed {

    private final byte type;

    protected EventTyped(byte eventType) {
        type = eventType;
    }

    @Override
    public byte getType() {
        return type;
    }

}