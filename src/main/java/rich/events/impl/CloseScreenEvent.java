package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.Screen;
import rich.events.api.events.callables.EventCancellable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloseScreenEvent extends EventCancellable {
    Screen screen;

}
