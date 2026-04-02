package fun.aegis.mixins.player.inventory;

// import fun.aegis.display.screens.clickgui.components.implement.autobuy.manager.AutoBuyManager;
import fun.aegis.features.impl.misc.SelfDestruct;
import fun.aegis.utils.display.widget.ContainerBackgroundRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> {
    private ButtonWidget takeAllButton;
    private ButtonWidget dropAllButton;
    private ButtonWidget storeAllButton;
    private boolean buttonsAdded = false;
    // private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    @Unique
    private static final ContainerBackgroundRender BACKGROUND_RENDER = new ContainerBackgroundRender();

    public GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        String title = this.getTitle().getString();

        if (!buttonsAdded) {
            addButtons(mc, title);
            buttonsAdded = true;
        }

    }

    private void addButtons(MinecraftClient mc, String titleText) {
        int baseX = (this.width + this.backgroundWidth) / 2;
        int baseY = (this.height - this.backgroundHeight) / 2;

        this.dropAllButton = ButtonWidget.builder(
                Text.literal("Выбросить"),
                button -> dropAll(mc)
        ).dimensions(baseX, baseY, 80, 20).build();

        this.takeAllButton = ButtonWidget.builder(
                Text.literal("Взять всё"),
                button -> takeAll(mc)
        ).dimensions(baseX, baseY + 22, 80, 20).build();

        this.storeAllButton = ButtonWidget.builder(
                Text.literal("Сложить всё"),
                button -> storeAll(mc)
        ).dimensions(baseX, baseY + 44, 80, 20).build();

        this.addDrawableChild(dropAllButton);
        this.addDrawableChild(takeAllButton);
        this.addDrawableChild(storeAllButton);

    }

    private void takeAll(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;
        for (Slot slot : player.currentScreenHandler.slots) {
            if (slot.inventory != player.getInventory() && slot.hasStack()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        slot.id,
                        0,
                        SlotActionType.QUICK_MOVE,
                        player
                );
            }
        }
    }

    private void dropAll(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;
        for (Slot slot : player.currentScreenHandler.slots) {
            if (slot.inventory != player.getInventory() && slot.hasStack()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        slot.id,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }
    }

    private void storeAll(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;
        for (Slot slot : player.currentScreenHandler.slots) {
            if (slot.inventory == player.getInventory() && slot.hasStack()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        slot.id,
                        0,
                        SlotActionType.QUICK_MOVE,
                        player
                );
            }
        }
    }
}
