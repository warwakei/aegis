package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import rich.events.api.events.callables.EventCancellable;
import rich.modules.impl.combat.aura.Angle;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraEvent extends EventCancellable {
    boolean cameraClip;
    float distance;
    Angle angle;
}
