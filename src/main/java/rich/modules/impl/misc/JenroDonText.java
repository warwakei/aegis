package rich.modules.impl.misc;

import rich.events.api.EventHandler;
import rich.events.impl.ChatEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class JenroDonText extends ModuleStructure {

    private static final Map<Pattern, String> REPLACEMENTS = new HashMap<>();
    
    static {
        // Инициализация замен (регистронезависимые кроме указанных)
        addReplacement("бог", "&5&l[&e&lБОГ&5&l]&r", false);
        addReplacement("цезарь", "&2&l[&4&lЦезарь&2&l]&r", false);
        addReplacement("вип", "&2&l[&a&lВип&2&l]&r", false);
        addReplacement("премиум", "&3&l[&b&lПремиум&3&l]&r", false);
        addReplacement("Креатив", "&a&l[&2&lКреатив&a&l]&r", true); // Только с большой буквы
        addReplacement("Админ", "&d&l[&5&lАдмин&d&l]&r", true); // Только с большой буквы
        addReplacement("лорд", "&6&l[&e&lЛорд&6&l]&r", false);
        addReplacement("гл\\.админ", "&b&l[&3&lГл.Админ&b&l]&r", false);
        
        // Множественные варианты через |
        addReplacement("(?:Создатель|созд)", "&e&l[&6&lСоздатель&e&l]&r", false);
        addReplacement("(?:основател[ья])", "&5&l[&d&lОснователь&5&l]&r", false);
        addReplacement("(?:Владелец|владельца)", "&4&l[&c&lВладелец&4&l]&r", false);
        addReplacement("президента", "&6&l[&b&lПРЕЗИДЕНТ&6&l]&r", false);
        addReplacement("(?:властелин|власт|власта|властелина)", "&e&l[&a&lВЛАСТЕЛИН&e&l]&r", false);
        addReplacement("(?:правитель|правик|правика|правителя)", "&b&l[&6&lПРАВИТЕЛЬ&b&l]&r", false);
        addReplacement("(?:барон|барик|барона|барика)", "&e&l⟨&4&lБАРОН&e&l⟩&r", false);
        addReplacement("(?:владыка|владыки)", "&4&l⟨&2&lВЛАДЫКА&4&l⟩&r", false);
        addReplacement("(?:султан|султ|султана)", "&3&l⟨&e&lСУЛТАН&3&l⟩&r", false);
        addReplacement("(?:мажор|мажора)", "&6&l⟨&5&lМАЖОР&6&l⟩&r", false);
        addReplacement("(?:хозяин|хоз|хозяина)", "&c&l⟨&e&lХОЗЯИН&c&l⟩&r", false);
        addReplacement("(?:господь|госп|госпа|господя)", "&a&l⟨&e&l$&b&lГОСПОДЬ&e&l$&a&l⟩&r", false);
        addReplacement("(?:сп|спонсор|спонсора|спонсорки)", "&b&l⟨&e&l‚&d&lСПОНСОР&e&l¸&b&l⟩&r", false);
    }
    
    private static void addReplacement(String pattern, String replacement, boolean caseSensitive) {
        Pattern regex;
        if (caseSensitive) {
            regex = Pattern.compile("\\b" + pattern + "\\b");
        } else {
            regex = Pattern.compile("\\b" + pattern + "\\b", Pattern.CASE_INSENSITIVE);
        }
        REPLACEMENTS.put(regex, replacement);
    }

    public JenroDonText() {
        super("Jenro DonText", "Автоматически заменяет донатские слова на цветные", ModuleCategory.MISC);
    }

    @EventHandler
    public void onChatMessage(ChatEvent event) {
        if (!isState()) return;
        
        String originalMessage = event.getMessage();
        String modifiedMessage = originalMessage;
        
        // Отладка - выводим исходное сообщение
        System.out.println("[JenroDonText] Исходное сообщение: '" + originalMessage + "'");
        
        // Убираем все цветовые коды для поиска слов
        String cleanMessage = originalMessage.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
        System.out.println("[JenroDonText] Очищенное сообщение: '" + cleanMessage + "'");
        
        // Применяем все замены к очищенному тексту
        String replacedClean = cleanMessage;
        for (Map.Entry<Pattern, String> entry : REPLACEMENTS.entrySet()) {
            String before = replacedClean;
            replacedClean = entry.getKey().matcher(replacedClean).replaceAll(entry.getValue());
            if (!before.equals(replacedClean)) {
                System.out.println("[JenroDonText] Замена: '" + before + "' -> '" + replacedClean + "'");
            }
        }
        
        // Если произошли замены, используем новый текст
        if (!cleanMessage.equals(replacedClean)) {
            modifiedMessage = replacedClean;
            System.out.println("[JenroDonText] Финальное сообщение: '" + modifiedMessage + "'");
            event.setMessage(modifiedMessage);
        } else {
            System.out.println("[JenroDonText] Замен не произошло");
        }
    }
}