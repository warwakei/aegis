package fun.aegis.events.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import fun.aegis.utils.client.managers.event.events.callables.EventCancellable;


@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BoundingBoxControlEvent extends EventCancellable {
    public Box box;
    public Entity entity;
}
