package rich.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилита для управления префиксами игроков в Tab-листе и чате
 */
public class PlayerPrefixUtils {

    // Маппинг ников на префиксы (нижний регистр для case-insensitive сравнения)
    private static final Map<String, PlayerPrefix> PREFIX_MAP = new HashMap<>();

    static {
        // boodlone и mester_dan55 - " | Aegis : Bud" (фиолетовый, жирный, подчеркивание на Aegis)
        PlayerPrefix budPrefix = new PlayerPrefix(
            createAegisPrefix("Bud", Formatting.LIGHT_PURPLE, true, true)
        );
        PREFIX_MAP.put("boodlone", budPrefix);
        PREFIX_MAP.put("mester_dan55", budPrefix);

        // danil113 - тоже "Bud"
        PREFIX_MAP.put("danil113", new PlayerPrefix(
            createAegisPrefix("Bud", Formatting.LIGHT_PURPLE, true, true)
        ));

        // warwakei, confession, vesee200, papotcha, kurysheva, dletontet - " | Aegis : Owner" (Owner красный, жирный, курсив, подчеркивание)
        PlayerPrefix ownerPrefix = new PlayerPrefix(
            createAegisPrefix("Owner", Formatting.RED, true, true, true)
        );
        PREFIX_MAP.put("warwakei", ownerPrefix);
        PREFIX_MAP.put("confession", ownerPrefix);
        PREFIX_MAP.put("vesee200", ownerPrefix);
        PREFIX_MAP.put("papotcha", ownerPrefix);
        PREFIX_MAP.put("kurysheva", ownerPrefix);
        PREFIX_MAP.put("dletontet", ownerPrefix);

        // catarsis - " | Aegis : FAME" (FAME жёлтый, жирный)
        PREFIX_MAP.put("catarsis", new PlayerPrefix(
            createAegisPrefix("FAME", Formatting.YELLOW, true, false)
        ));

        // Sty1es - "Kakashka"
        PREFIX_MAP.put("sty1es", new PlayerPrefix(
            createSimplePrefix("Kakashka", Formatting.WHITE)
        ));

        // lentauhc - "Dumb" (case-insensitive)
        PREFIX_MAP.put("lentauhc", new PlayerPrefix(
            createSimplePrefix("Dumb", Formatting.WHITE)
        ));
    }

    /**
     * Создает префикс в формате " | Aegis : <suffix>"
     */
    private static MutableText createAegisPrefix(String suffix, Formatting color, boolean bold, boolean underline) {
        return createAegisPrefix(suffix, color, bold, underline, false);
    }

    /**
     * Создает префикс в формате " | Aegis : <suffix>"
     */
    private static MutableText createAegisPrefix(String suffix, Formatting color, boolean bold, boolean underline, boolean italic) {
        // Разделитель |
        MutableText separator = Text.literal(" | ").formatted(Formatting.GRAY);
        
        // Aegis - фиолетовый, подчеркнутый
        MutableText aegis = Text.literal("Aegis")
            .formatted(Formatting.LIGHT_PURPLE)
            .styled(style -> style.withUnderline(true));
            
        // Разделитель :
        MutableText colon = Text.literal(" : ").formatted(Formatting.GRAY);

        // Суффикс с заданным форматированием
        MutableText suffixText = Text.literal(suffix).formatted(color);
        if (bold) {
            suffixText = suffixText.formatted(Formatting.BOLD);
        }
        if (underline) {
            suffixText = suffixText.styled(style -> style.withUnderline(true));
        }
        if (italic) {
            suffixText = suffixText.formatted(Formatting.ITALIC);
        }

        return Text.empty()
            .append(separator)
            .append(aegis)
            .append(colon)
            .append(suffixText);
    }

    /**
     * Создает простой префикс в формате " | <text>"
     */
    private static MutableText createSimplePrefix(String text, Formatting color) {
        MutableText separator = Text.literal(" | ").formatted(Formatting.GRAY);
        MutableText textPart = Text.literal(text).formatted(color, Formatting.BOLD);
        return Text.empty().append(separator).append(textPart);
    }

    /**
     * Проверяет, есть ли у игрока префикс
     */
    public static boolean hasPrefix(String playerName) {
        return PREFIX_MAP.containsKey(playerName.toLowerCase());
    }

    /**
     * Получает префикс для игрока (только суффикс без ника)
     */
    public static MutableText getPrefix(String playerName) {
        PlayerPrefix prefix = PREFIX_MAP.get(playerName.toLowerCase());
        if (prefix != null) {
            return prefix.prefixText.copy();
        }
        return null;
    }

    /**
     * Получает полное имя игрока с префиксом: "ник | Aegis : Bud"
     */
    public static MutableText getFullNameWithPrefix(String playerName) {
        MutableText name = Text.literal(playerName).formatted(Formatting.WHITE);
        MutableText prefix = getPrefix(playerName);
        if (prefix != null) {
            return Text.empty().append(name).append(prefix);
        }
        return name;
    }

    /**
     * Внутренний класс для хранения префикса
     */
    private static class PlayerPrefix {
        private final MutableText prefixText;

        public PlayerPrefix(MutableText prefixText) {
            this.prefixText = prefixText;
        }
    }
}
