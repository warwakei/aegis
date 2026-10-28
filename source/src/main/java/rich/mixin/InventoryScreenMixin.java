package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void addDropAllButton(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        InventoryScreen screen = (InventoryScreen) (Object) this;

        int x = screen.width / 2 - 40;
        int y = screen.height / 2 - 120;

        ButtonWidget dropAllButton = ButtonWidget.builder(
                Text.of("Выкинуть всё"),
                button -> dropAllItems(mc)
        ).position(x, y).size(80, 20).build();

        screen.addDrawableChild(dropAllButton);
    }

    private void dropAllItems(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i + 36,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }
    }
}