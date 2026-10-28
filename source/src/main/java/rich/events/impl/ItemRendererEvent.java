package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import rich.events.api.events.Event;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemRendererEvent implements Event {
    AbstractClientPlayerEntity player;
    ItemStack stack;
    Hand hand;
}
