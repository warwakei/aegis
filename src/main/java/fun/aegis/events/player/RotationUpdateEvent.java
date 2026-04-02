package fun.aegis.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import fun.aegis.utils.client.managers.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
