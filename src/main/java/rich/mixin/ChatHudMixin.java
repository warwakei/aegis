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
 * Миксин для скрытия сообщений "Машина занята..." когда JenroCasino включён
 * и для добавления префиксов к никам игроков в чате
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

        // Проверяем включён ли JenroCasino для скрытия сообщений "Машина занята"
        try {
            var jenroCasino = rich.util.Instance.get(rich.modules.impl.misc.JenroCasino.class);
            if (jenroCasino != null && jenroCasino.isState()) {
                String clean = text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
                if (clean.contains("Машина занята")) {
                    return null; // Возвращаем null чтобы отменить добавление
                }
            }
        } catch (Exception ignored) {}

        // Формат сообщения в чате: <ник> сообщение или ник: сообщение
        // Ищем ник в начале сообщения
        String playerName = extractPlayerName(text);
        if (playerName != null && PlayerPrefixUtils.hasPrefix(playerName)) {
            // Создаём новое сообщение с префиксом
            MutableText newText = Text.empty();
            
            // Добавляем ник
            newText.append(Text.literal(playerName).formatted(Formatting.WHITE));
            
            // Добавляем префикс
            MutableText prefix = PlayerPrefixUtils.getPrefix(playerName);
            if (prefix != null) {
                newText.append(prefix);
            }
            
            // Добавляем остаток сообщения после ника
            String restOfMessage = text.substring(playerName.length());
            newText.append(Text.literal(restOfMessage));
            
            return newText;
        }

        return message;
    }

    @Unique
    private String extractPlayerName(String text) {
        // Формат: <ник> сообщение
        if (text.startsWith("<")) {
            int closeBracket = text.indexOf(">");
            if (closeBracket > 2) {
                // Извлекаем ник между < и >
                String potentialName = text.substring(1, closeBracket);
                // Убираем возможные форматирования
                potentialName = potentialName.replaceAll("§[0-9a-fk-or]", "").trim();
                
                // Проверяем, есть ли такой игрок в списке
                if (PlayerPrefixUtils.hasPrefix(potentialName)) {
                    return potentialName;
                }
            }
        }
        
        // Формат: ник: сообщение или ник -> сообщение
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
