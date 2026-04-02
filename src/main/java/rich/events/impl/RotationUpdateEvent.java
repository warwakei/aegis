package rich.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rich.events.api.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
