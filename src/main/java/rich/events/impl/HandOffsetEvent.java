package rich.events.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import rich.events.api.events.callables.EventCancellable;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class HandOffsetEvent extends EventCancellable {
    MatrixStack matrices;
    ItemStack stack;
    Hand hand;
    float scale;

    public HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
        this.matrices = matrices;
        this.stack = stack;
        this.hand = hand;
        this.scale = 1.0F;
    }
}