package fun.aegis.utils.client.managers.event.events.callables;

import fun.aegis.utils.client.managers.event.events.Event;
import fun.aegis.utils.client.managers.event.events.Typed;

public abstract class EventTyped implements Event, Typed {

    private final byte type;

    /**
     * Sets the type of the event when it's called.
     *
     * @param eventType
     *         The type ID of the event.
     */
    protected EventTyped(byte eventType) {
        type = eventType;
    }

    /**
     * @see com.darkmagician6.eventapi.events.Typed.getType
     */
    @Override
    public byte getType() {
        return type;
    }

}
