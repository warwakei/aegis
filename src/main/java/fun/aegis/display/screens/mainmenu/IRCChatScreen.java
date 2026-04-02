package fun.aegis.display.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import fun.aegis.common.chat.ChatManager;
import fun.aegis.Aegis;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.interfaces.QuickImports;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class IRCChatScreen extends Screen implements QuickImports {
    private final Screen parent;
    private ChatManager chatManager;
    private String inputText = "";
    private List<String> chatHistory = new ArrayList<>();
    private float scrollOffset = 0;
    private static final int MESSAGE_HEIGHT = 15;
    private static final int CHAT_AREA_HEIGHT = 300;

    public IRCChatScreen(Screen parent) {
        super(Text.literal("IRC Chat"));
        this.parent = parent;
        
        if (Aegis.getInstance().getAuthenticationManager().isAuthenticated()) {
            this.chatManager = new ChatManager(
                Aegis.getInstance().getAuthenticationManager().getCurrentIdToken(),
                Aegis.getInstance().getAuthenticationManager().getCurrentUsername()
            );
            this.chatManager.startListening();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int chatWidth = 400;
        int chatHeight = CHAT_AREA_HEIGHT;
        int chatX = centerX - chatWidth / 2;
        int chatY = centerY - chatHeight / 2 - 40;

        rectangle.render(ShapeProperties.create(context.getMatrices(), chatX, chatY, chatWidth, chatHeight)
                .thickness(2).round(4).outlineColor(new Color(100, 100, 100, 200).getRGB())
                .color(new Color(30, 30, 30, 200).getRGB()).build());

        Fonts.getSize(14, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "IRC Chat", chatX + 10, chatY + 5, 0xFFFFFFFF);

        int messageStartY = chatY + 25;
        int visibleMessages = chatHeight / MESSAGE_HEIGHT - 2;
        
        if (chatManager != null) {
            List<ChatManager.ChatMessage> messages = chatManager.getMessages();
            int startIndex = Math.max(0, messages.size() - visibleMessages);
            
            for (int i = startIndex; i < messages.size(); i++) {
                ChatManager.ChatMessage msg = messages.get(i);
                int messageY = messageStartY + (i - startIndex) * MESSAGE_HEIGHT;
                
                String displayText = msg.username + ": " + msg.message;
                if (displayText.length() > 50) {
                    displayText = displayText.substring(0, 47) + "...";
                }
                
                Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(context.getMatrices(), displayText, chatX + 5, messageY, 0xFFCCCCCC);
            }
        }

        int inputY = chatY + chatHeight + 10;
        rectangle.render(ShapeProperties.create(context.getMatrices(), chatX, inputY, chatWidth, 20)
                .thickness(2).round(4).outlineColor(new Color(100, 100, 100, 200).getRGB())
                .color(new Color(40, 40, 40, 200).getRGB()).build());
        
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(), inputText, chatX + 5, inputY + 4, 0xFFFFFFFF);

        Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "ESC - Назад", this.width / 2, this.height - 20, 0xFF888888);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (chatManager != null) {
                chatManager.stopListening();
            }
            this.client.setScreen(parent);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (!inputText.isEmpty() && chatManager != null) {
                chatManager.sendMessage(inputText);
                inputText = "";
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!inputText.isEmpty()) {
                inputText = inputText.substring(0, inputText.length() - 1);
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr <= 126 && inputText.length() < 100) {
            inputText += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (chatManager != null) {
            chatManager.stopListening();
        }
        super.close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
