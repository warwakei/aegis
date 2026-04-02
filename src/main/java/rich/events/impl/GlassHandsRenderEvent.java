package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import rich.events.api.events.callables.EventCancellable;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@Setter
public class GlassHandsRenderEvent extends EventCancellable {

    public enum Phase {
        PRE,
        POST
    }

    Phase phase;
    MatrixStack matrices;
    float tickDelta;
}