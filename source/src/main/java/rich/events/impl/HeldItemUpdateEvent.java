package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import rich.events.api.events.Event;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HeldItemUpdateEvent implements Event {
    ItemStack mainHand;
    ItemStack offHand;
}