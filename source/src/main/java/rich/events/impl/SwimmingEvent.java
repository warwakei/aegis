package rich.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import rich.events.api.events.callables.EventCancellable;

@Setter
@Getter
@AllArgsConstructor
public class SwimmingEvent extends EventCancellable {
    Vec3d vector;
}
