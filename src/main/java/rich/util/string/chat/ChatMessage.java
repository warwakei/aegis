package rich.util.string.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.util.Version;
import rich.util.string.chat.helper.TextHelper;

public class ChatMessage {
    public static MutableText brandmessage() {
        return (MutableText) TextHelper.applyPredefinedGradient(Version.NAME, "black_light_purple", true);
    }

    public static MutableText blockesp() {
        return (MutableText) TextHelper.applyPredefinedGradient("Block Esp", "black_light_purple", true);
    }

    public static MutableText autobuy() {
        return (MutableText) TextHelper.applyPredefinedGradient("Auto Buy", "black_light_purple", true);
    }

    public static MutableText autobuiparcer() {
        return (MutableText) TextHelper.applyPredefinedGradient("Parce price", "black_light_purple", true);
    }

    public static void brandmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient(Version.NAME + " -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void autobuymessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("AutoBuy -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void autobuymessageSuccess(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("AutoBuy -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void autobuymessageError(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("AutoBuy -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.RED)));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void autobuymessageWarning(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("AutoBuy -> ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
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

    public static void ircmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("[IRC] ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void ircmessageWithGreen(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("[IRC] ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void ircmessageWithRed(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefix = TextHelper.applyPredefinedGradient("[IRC] ", "black_light_purple", true);
            Text formattedMessage = prefix.copy().append(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.RED)));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static Text ircprefixDeveloper(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Developer ", "dark_red_bright_red", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixCurator(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Куратор ", "dark_red", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixYouTube(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("YouTube ", "red_white", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixPikmi(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Пикми ", "purple_bright_pink", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixLabuba(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Лабуба ", "pink_dark_pink", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixZapen(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Запен ", "bright_red", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixBoost(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Буст ", "dark_green_bright_green", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixRich(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Рич ", "red_orange", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixPanda(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Панда ", "white_black", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixSmiley(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("(●'◡'●) ", "turquoise_blue", true);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixBibi(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Биби...! ", "cyan_orange_fade", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixBenena(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Бэнена ", "yellow_cyan", false);
        return prefix.copy().append(Text.literal(message));
    }

    public static Text ircprefixBlyabuba(String message) {
        Text prefix = TextHelper.applyPredefinedGradient("Блябуба ", "purple_red_fade", false);
        return prefix.copy().append(Text.literal(message));
    }
}