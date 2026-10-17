package rich.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rich.util.PlayerPrefixUtils;

/**
 * миксин чтоб чат чистить
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text onAddMessage(Text value) {
        return processChatMessage(value);
    }

    @Unique
    private Text processChatMessage(Text message) {
        if (message == null) return message;

        String text = message.getString();
        if (text == null || text.isEmpty()) return message;

        // формат чата ник плюс текст
        // ищем ник в начале
        String playerName = extractPlayerName(text);
        if (playerName != null && PlayerPrefixUtils.hasPrefix(playerName)) {
            // лепим новое сообщение с префиксом
            MutableText newText = Text.empty();

            // пихаем ник сюда
            newText.append(Text.literal(playerName).formatted(Formatting.WHITE));

            // пихаем префикс сюда
            MutableText prefix = PlayerPrefixUtils.getPrefix(playerName);
            if (prefix != null) {
                newText.append(prefix);
            }

            // дописываем остаток сообщения
            String restOfMessage = text.substring(playerName.length());
            newText.append(Text.literal(restOfMessage));

            return newText;
        }

        return message;
    }

    @Unique
    private String extractPlayerName(String text) {
        // формат скобки ник текст
        if (text.startsWith("<")) {
            int closeBracket = text.indexOf(">");
            if (closeBracket > 2) {
                // дёргаем ник из скобок
                String potentialName = text.substring(1, closeBracket);
                // чистим цвета форматирование
                potentialName = potentialName.replaceAll("§[0-9a-fk-or]", "").trim();

                // чекаем есть ли такой игрок
                if (PlayerPrefixUtils.hasPrefix(potentialName)) {
                    return potentialName;
                }
            }
        }

        // формат ник двоеточие текст
        int colonIndex = text.indexOf(":");
        int arrowIndex = text.indexOf("->");
        int separatorIndex = -1;

        if (colonIndex > 0 && (arrowIndex < 0 || colonIndex < arrowIndex)) {
            separatorIndex = colonIndex;
        } else if (arrowIndex > 0) {
            separatorIndex = arrowIndex;
        }

        if (separatorIndex > 0) {
            String potentialName = text.substring(0, separatorIndex).trim();
            potentialName = potentialName.replaceAll("§[0-9a-fk-or]", "").trim();

            if (PlayerPrefixUtils.hasPrefix(potentialName)) {
                return potentialName;
            }
        }

        return null;
    }
}
