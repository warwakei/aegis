package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.modules.impl.combat.macetarget.ChatTabButtonMixinAccessor;
import rich.util.config.impl.chattab.ChatTab;
import rich.util.config.impl.chattab.ChatTabContextMenu;
import rich.util.config.impl.chattab.ChatTabManager;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public abstract class ChatTabButtonMixin implements ChatTabButtonMixinAccessor {

    @Shadow
    protected TextFieldWidget chatField;

    @Shadow
    protected abstract void init();

    @Unique
    private final List<ButtonWidget> chatTabButtons = new ArrayList<>();

    @Unique
    private final List<ChatTab> chatTabData = new ArrayList<>();

    @Unique
    private int chatTabActiveIndex = 0;

    @Unique
    private static final int TAB_BUTTON_WIDTH = 70;

    @Unique
    private static final int TAB_BUTTON_HEIGHT = 10;

    @Unique
    private ChatTabContextMenu contextMenu;

    @Unique
    private Integer draggingTabIndex = null;

    @Unique
    private int dragOffsetX;

    @Inject(method = "init", at = @At("RETURN"))
    private void initChatTabs(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        contextMenu = ChatTabContextMenu.getInstance();
        contextMenu.hide();

        ChatTabManager tabManager = ChatTabManager.getInstance();
        tabManager.init();

        chatTabButtons.clear();
        chatTabData.clear();

        int buttonWidth = TAB_BUTTON_WIDTH;
        int buttonHeight = TAB_BUTTON_HEIGHT;
        int chatY = this.chatField.getY() - buttonHeight - 2;
        int startX = this.chatField.getX() + 2;

        int x = startX;

        // Кнопка "Полный чат"
        ButtonWidget allButton = createTabButton(x, chatY, buttonWidth, buttonHeight, "Полный чат", 0);
        chatTabButtons.add(allButton);
        chatTabData.add(null);
        x += buttonWidth + 1;

        // Кнопка "Друзья"
        ButtonWidget friendsButton = createTabButton(x, chatY, buttonWidth, buttonHeight, "Друзья", 1);
        chatTabButtons.add(friendsButton);
        chatTabData.add(null);
        x += buttonWidth + 1;

        // Кнопка "Игнор"
        ButtonWidget ignoreButton = createTabButton(x, chatY, buttonWidth, buttonHeight, "Игнор", 2);
        chatTabButtons.add(ignoreButton);
        chatTabData.add(null);
        x += buttonWidth + 1;

        // Кастомные вкладки
        for (ChatTab tab : tabManager.getTabs()) {
            final int tabIndex = chatTabButtons.size();
            String displayName = tab.getName().length() > 10 ? tab.getName().substring(0, 9) + "..." : tab.getName();

            ButtonWidget tabButton = createTabButton(x, chatY, buttonWidth, buttonHeight, displayName, tabIndex);
            chatTabButtons.add(tabButton);
            chatTabData.add(tab);
            x += buttonWidth + 1;
        }

        for (ButtonWidget button : chatTabButtons) {
            // Кнопки рендерятся вручную в onRender
        }

        updateButtonVisibility();
    }

    @Unique
    private ButtonWidget createTabButton(int x, int y, int width, int height, String text, int index) {
        return ButtonWidget.builder(
                Text.literal(text),
                btn -> {
                    if (index == 0) {
                        ChatTabManager.getInstance().setActiveTab(null);
                        chatTabActiveIndex = 0;
                    } else if (index == 1) {
                        ChatTabManager.getInstance().setActiveTab("Друзья");
                        chatTabActiveIndex = 1;
                    } else if (index == 2) {
                        ChatTabManager.getInstance().setActiveTab("Игнор");
                        chatTabActiveIndex = 2;
                    } else {
                        ChatTab tab = chatTabData.get(index);
                        if (tab != null) {
                            ChatTabManager.getInstance().setActiveTab(tab.getName());
                            chatTabActiveIndex = index;
                        }
                    }
                }
        )
        .dimensions(x, y, width, height)
        .build();
    }

    @Inject(method = "resize", at = @At("RETURN"))
    private void onResize(int width, int height, CallbackInfo ci) {
        updateButtonVisibility();
    }

    @Unique
    private void updateButtonVisibility() {
        int chatWidth = this.chatField.getWidth();
        int maxButtons = chatWidth / (TAB_BUTTON_WIDTH + 1);

        for (int i = 0; i < chatTabButtons.size(); i++) {
            ButtonWidget button = chatTabButtons.get(i);
            button.visible = i < maxButtons;
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (contextMenu == null) return;

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int button = click.button();

        // Обработка клика по контекстному меню
        if (contextMenu.isVisible()) {
            if (button == 1) {
                contextMenu.hide();
                cir.setReturnValue(true);
                return;
            }

            // Клик ЛКМ по опции меню
            if (contextMenu.isMouseOverOption(mouseX, mouseY, 0)) {
                // Удалить вкладку
                if (contextMenu.getTargetTab() != null) {
                    ChatTabManager.getInstance().removeTab(contextMenu.getTargetTab().getName());
                    contextMenu.hide();
                    init();
                    cir.setReturnValue(true);
                    return;
                }
            } else if (contextMenu.isMouseOverOption(mouseX, mouseY, 1)) {
                // Переименовать вкладку - отправляем команду
                if (contextMenu.getTargetTab() != null) {
                    String cmd = ".chatpages rename " + contextMenu.getTargetTab().getName() + " ";
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(cmd);
                    }
                    contextMenu.hide();
                    cir.setReturnValue(true);
                    return;
                }
            }
            return;
        }

        // Проверка клика по вкладкам
        for (int i = 0; i < chatTabButtons.size(); i++) {
            ButtonWidget buttonWidget = chatTabButtons.get(i);
            if (buttonWidget.visible && buttonWidget.isMouseOver(mouseX, mouseY)) {
                if (button == 1 && i >= 3) {
                    // ПКМ по кастомной вкладке - открываем контекстное меню
                    contextMenu.show(mouseX, mouseY - 70, chatTabData.get(i), i);
                    cir.setReturnValue(true);
                    return;
                } else if (button == 0 && i >= 3) {
                    // ЛКМ по кастомной вкладке - начинаем перетаскивание
                    draggingTabIndex = i;
                    dragOffsetX = mouseX - buttonWidget.getX();
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // Клик вне меню закрывает его
        if (contextMenu.isVisible()) {
            contextMenu.hide();
        }
    }

    @Unique
    public void onDragEnd(Click click) {
        if (draggingTabIndex != null) {
            int mouseX = (int) click.x();

            // Находим вкладку над которой отпустили
            for (int i = 3; i < chatTabButtons.size(); i++) {
                ButtonWidget button = chatTabButtons.get(i);
                if (button.visible && button.isMouseOver(mouseX, (int) click.y())) {
                    if (i != draggingTabIndex) {
                        // Перемещаем вкладку
                        ChatTabManager manager = ChatTabManager.getInstance();
                        manager.moveTab(draggingTabIndex - 3, i - 3);
                        init();
                        draggingTabIndex = null;
                        return;
                    }
                }
            }

            draggingTabIndex = null;
        }
    }

    @Unique
    public void onCursorMove(double mouseX, double mouseY) {
        if (contextMenu != null && contextMenu.isVisible()) {
            // Обновляем hovered опцию
            for (int i = 0; i < contextMenu.getOptionCount(); i++) {
                if (contextMenu.isMouseOverOption((int) mouseX, (int) mouseY, i)) {
                    contextMenu.setHoveredOption(i);
                    return;
                }
            }
            contextMenu.setHoveredOption(-1);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChatTabManager tabManager = ChatTabManager.getInstance();
        String activeTabName = tabManager.getActiveTabName();

        int buttonIndex = 0;
        if (!tabManager.hasActiveTab()) {
            buttonIndex = 0;
        } else {
            for (int i = 0; i < chatTabButtons.size(); i++) {
                ButtonWidget button = chatTabButtons.get(i);
                String buttonText = button.getMessage().getString();

                if ((activeTabName.equals("Все") && buttonText.equals("Полный чат")) ||
                    (activeTabName.equals("Друзья") && buttonText.equals("Друзья")) ||
                    (activeTabName.equals("Игнор") && buttonText.equals("Игнор")) ||
                    (activeTabName.equals(buttonText))) {
                    buttonIndex = i;
                    break;
                }
            }
        }

        // Рендер кнопок
        for (int i = 0; i < chatTabButtons.size(); i++) {
            ButtonWidget button = chatTabButtons.get(i);
            if (button.visible) {
                if (i == buttonIndex) {
                    button.setAlpha(1.0f);
                } else {
                    button.setAlpha(0.6f);
                }

                // Подсветка при перетаскивании
                if (draggingTabIndex != null && i != draggingTabIndex) {
                    button.setAlpha(0.3f);
                }
            }
        }

        // Рендер контекстного меню
        if (contextMenu != null && contextMenu.isVisible()) {
            renderContextMenu(context, mouseX, mouseY);
        }

        // Подсказка при перетаскивании
        if (draggingTabIndex != null) {
            Text dragText = Text.literal("Перетащите для перемещения").formatted(Formatting.YELLOW);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, dragText, mouseX + 10, mouseY - 20, 0xFFFFFF);
        }
    }

    @Unique
    private void renderContextMenu(DrawContext context, int mouseX, int mouseY) {
        int menuX = contextMenu.getX();
        int menuY = contextMenu.getY();
        int menuWidth = contextMenu.getWidth();
        int menuHeight = contextMenu.getHeight();

        // Фон меню
        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xC0000000);

        // Граница
        context.drawHorizontalLine(menuX, menuX + menuWidth, menuY, 0xFFFFFFFF);
        context.drawHorizontalLine(menuX, menuX + menuWidth, menuY + menuHeight, 0xFFFFFFFF);
        context.drawVerticalLine(menuX, menuY, menuY + menuHeight, 0xFFFFFFFF);
        context.drawVerticalLine(menuX + menuWidth, menuY, menuY + menuHeight, 0xFFFFFFFF);

        // Заголовок
        String tabName = contextMenu.getTargetTab() != null ? contextMenu.getTargetTab().getName() : "Вкладка";
        Text title = Text.literal(" " + tabName).formatted(Formatting.BOLD, Formatting.YELLOW);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, title, menuX + 2, menuY + 3, 0xFFFFFF);

        // Опции
        int optionY = menuY + 20;

        // Удалить
        boolean hoverDelete = contextMenu.isMouseOverOption(mouseX, mouseY, 0);
        Text deleteText = Text.literal(" ✕ Удалить").formatted(hoverDelete ? Formatting.RED : Formatting.WHITE);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, deleteText, menuX + 5, optionY, 0xFFFFFF);

        // Переименовать
        optionY += 20;
        boolean hoverRename = contextMenu.isMouseOverOption(mouseX, mouseY, 1);
        Text renameText = Text.literal(" ✎ Переименовать").formatted(hoverRename ? Formatting.GREEN : Formatting.WHITE);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, renameText, menuX + 5, optionY, 0xFFFFFF);
    }
}
