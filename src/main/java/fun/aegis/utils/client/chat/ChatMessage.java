package fun.aegis.utils.client.chat;

import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import fun.aegis.utils.client.text.TextHelper;

public class ChatMessage {
    public static MutableText brandmessage() {
        return (MutableText) TextHelper.applyPredefinedGradient("Aegis Client", "black_light_purple", true);
    }

    public static MutableText blockesp() {
        return (MutableText) TextHelper.applyPredefinedGradient("Block Esp", "black_light_purple", true);
    }

    public static void brandmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("Aegis Client -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void ancientmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("Ancient Xray -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void helpmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("Help -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void swapmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("AutoSwap -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

}
