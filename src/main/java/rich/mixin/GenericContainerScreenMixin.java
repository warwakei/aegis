package rich.mixin;

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
import rich.modules.impl.misc.AutoBuy;
import rich.modules.impl.misc.autoparser.AutoParser;
import rich.modules.impl.misc.autoparser.dev.ItemParser;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.util.modules.autoparser.DiscountSliderWidget;
import rich.util.string.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> {

    @Unique
    private static long lastScreenOpenTime = 0;
    @Unique
    private static String lastScreenTitle = "";
    @Unique
    private static final long SCREEN_REOPEN_THRESHOLD = 500;

    @Unique
    private ButtonWidget takeAllButton;
    @Unique
    private ButtonWidget dropAllButton;
    @Unique
    private ButtonWidget storeAllButton;
    @Unique
    private ButtonWidget autoBuyButton;
    @Unique
    private ButtonWidget autoParserButton;
    @Unique
    private DiscountSliderWidget discountSlider;
    @Unique
    private ButtonWidget parseButton;
    @Unique
    private boolean buttonsAdded = false;
    @Unique
    private boolean isQuickReopen = false;
    @Unique
    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    public GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        String currentTitle = title.getString();

        if (currentTitle.equals(lastScreenTitle) && (now - lastScreenOpenTime) < SCREEN_REOPEN_THRESHOLD) {
            isQuickReopen = true;
        } else {
            isQuickReopen = false;
        }

        lastScreenOpenTime = now;
        lastScreenTitle = currentTitle;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String title = this.getTitle().getString();

        if (!buttonsAdded) {
            addButtons(mc, title);
            buttonsAdded = true;
        }

        updateAutoBuyButton();
        updateAutoParserButton();
        renderNetworkInfo(context, mc, title);
    }

    @Unique
    private void updateAutoBuyButton() {
        if (autoBuyButton == null) return;

        AutoBuy autoBuyModule = AutoBuy.getInstance();
        boolean moduleActive = autoBuyModule != null && autoBuyModule.isState();
        boolean buttonEnabled = autoBuyManager.isEnabled();

        String status;
        if (!moduleActive) {
            status = "§cOFF";
            autoBuyButton.active = false;
        } else if (buttonEnabled) {
            status = "§aON";
            autoBuyButton.active = true;
        } else {
            status = "§ePAUSE";
            autoBuyButton.active = true;
        }

        autoBuyButton.setMessage(Text.literal("AutoBuy: " + status));
    }

    @Unique
    private void updateAutoParserButton() {
        AutoParser parser = AutoParser.getInstance();
        if (autoParserButton == null || parser == null) return;

        boolean isRunning = parser.isRunning();
        autoParserButton.setMessage(Text.literal("AutoParser: " + (isRunning ? "§aON" : "§cOFF")));

        if (discountSlider != null) {
            discountSlider.active = !isRunning;
        }
    }

    @Unique
    private void renderNetworkInfo(DrawContext context, MinecraftClient mc, String title) {
        if (autoBuyButton == null || !title.contains("Аукцион")) return;

        AutoBuy autoBuyModule = AutoBuy.getInstance();
        if (autoBuyModule == null || !autoBuyModule.isState()) return;

        int clients = autoBuyModule.getNetworkManager().getConnectedClientCount();
        int inAuction = autoBuyModule.getNetworkManager().getClientsInAuctionCount();
        boolean connected = autoBuyModule.getNetworkManager().isConnectedToServer();
        boolean isServer = autoBuyModule.getNetworkManager().isServerRunning();

        String info;
        if (isServer) {
            info = "§7Клиентов: §b" + clients + " §7В аукционе: §b" + inAuction;
        } else if (connected) {
            info = "§aПодключён к серверу";
        } else {
            info = "§cНет подключения";
        }

        int infoX = (this.width - this.backgroundWidth) / 2 + this.backgroundWidth / 2;
        int infoY = (this.height - this.backgroundHeight) / 2 - 10;
        context.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(info), infoX, infoY, 0xFFFFFF);
    }

    @Unique
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

        int autoBuyX = (this.width - this.backgroundWidth) / 2 + this.backgroundWidth / 2 - 55;
        int autoBuyY = (this.height - this.backgroundHeight) / 2 - 25;

        AutoBuy autoBuyModule = AutoBuy.getInstance();
        boolean moduleActive = autoBuyModule != null && autoBuyModule.isState();
        boolean buttonEnabled = autoBuyManager.isEnabled();

        ItemParser parser2 = ItemParser.getInstance();
        boolean parserActive = parser2 != null && parser2.isState();

//        this.parseButton = ButtonWidget.builder(
//                Text.literal(parserActive ? "§aПарсить всё" : "§7Парсить всё"),
//                button -> handleParseButtonClick()
//        ).dimensions(autoBuyX, autoBuyY - 22, 80, 20).build();
//        this.parseButton.active = parserActive;
//        this.addDrawableChild(parseButton);

        String initialStatus;
        if (!moduleActive) {
            initialStatus = "§cOFF";
        } else if (buttonEnabled) {
            initialStatus = "§aON";
        } else {
            initialStatus = "§ePAUSE";
        }

        this.autoBuyButton = ButtonWidget.builder(
                Text.literal("AutoBuy: " + initialStatus),
                button -> handleAutoBuyButtonClick(button)
        ).dimensions(autoBuyX, autoBuyY, 110, 20).build();

        this.autoBuyButton.active = moduleActive;
        this.addDrawableChild(autoBuyButton);

        int leftX = (this.width - this.backgroundWidth) / 2 - 100;
        int leftY = (this.height - this.backgroundHeight) / 2;

        AutoParser parser = AutoParser.getInstance();
        int initialDiscount = parser != null ? parser.getDiscountPercent() : 60;

        this.autoParserButton = ButtonWidget.builder(
                Text.literal("AutoParser: §cOFF"),
                button -> handleAutoParserButtonClick()
        ).dimensions(leftX, leftY, 95, 20).build();
        this.addDrawableChild(autoParserButton);

        this.discountSlider = new DiscountSliderWidget(leftX, leftY + 24, 95, 20, initialDiscount);
        this.addDrawableChild(discountSlider);
    }

    @Unique
    private void handleAutoParserButtonClick() {
        AutoParser parser = AutoParser.getInstance();
        if (parser == null) {
            ChatMessage.autobuymessageError("AutoParser не инициализирован!");
            return;
        }

        if (parser.isRunning()) {
            parser.stopParsing();
        } else {
            parser.startParsing();
        }
    }

    @Unique
    private void handleParseButtonClick() {
        ItemParser parser = ItemParser.getInstance();
        if (parser == null || !parser.isState()) {
            ChatMessage.autobuymessageError("Сначала включите модуль Item Parser!");
            return;
        }

        int containerSize = this.handler.getRows() * 9;

        List<Slot> containerSlots = new ArrayList<>();
        for (int i = 0; i < containerSize && i < this.handler.slots.size(); i++) {
            containerSlots.add(this.handler.slots.get(i));
        }

        String containerTitle = this.getTitle().getString();

        parser.parseAllSlots(containerSlots, containerSize, containerTitle);
    }

    @Unique
    private void handleAutoBuyButtonClick(ButtonWidget button) {
        AutoBuy autoBuyModule = AutoBuy.getInstance();

        if (autoBuyModule == null || !autoBuyModule.isState()) {
            ChatMessage.autobuymessageError("Сначала включите модуль Auto Buy!");
            button.setMessage(Text.literal("AutoBuy: §cOFF"));
            button.active = false;
            return;
        }

        boolean currentState = autoBuyManager.isEnabled();
        boolean newState = !currentState;
        autoBuyManager.setEnabled(newState);

        String status;
        if (newState) {
            status = "§aON";
            ChatMessage.autobuymessageSuccess("AutoBuy включён!");
        } else {
            status = "§ePAUSE";
            ChatMessage.autobuymessageWarning("AutoBuy на паузе");
        }

        button.setMessage(Text.literal("AutoBuy: " + status));
    }

    @Unique
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

    @Unique
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

    @Unique
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