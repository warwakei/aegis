package rich.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * префиксы для табла и чата
 */
public class PlayerPrefixUtils {

    // ники и префиксы тут
    private static final Map<String, PlayerPrefix> PREFIX_MAP = new HashMap<>();

    static {
        // буд для буда и местера
        PlayerPrefix budPrefix = new PlayerPrefix(
            createAegisPrefix("Bud", Formatting.LIGHT_PURPLE, true, true)
        );
        PREFIX_MAP.put("boodlone", budPrefix);
        PREFIX_MAP.put("mester_dan55", budPrefix);

        // буд для данилы
        PREFIX_MAP.put("danil113", new PlayerPrefix(
            createAegisPrefix("Bud", Formatting.LIGHT_PURPLE, true, true)
        ));

        // овнер для своих
        PlayerPrefix ownerPrefix = new PlayerPrefix(
            createAegisPrefix("Owner", Formatting.RED, true, true, true)
        );
        PREFIX_MAP.put("warwakei", ownerPrefix);
        PREFIX_MAP.put("confession", ownerPrefix);
        PREFIX_MAP.put("vesee200", ownerPrefix);
        PREFIX_MAP.put("papotcha", ownerPrefix);
        PREFIX_MAP.put("kurysheva", ownerPrefix);
        PREFIX_MAP.put("dletontet", ownerPrefix);

        // фейма для катарсиса
        PREFIX_MAP.put("catarsis", new PlayerPrefix(
            createAegisPrefix("FAME", Formatting.YELLOW, true, false)
        ));

        // какашка для стайлса
        PREFIX_MAP.put("sty1es", new PlayerPrefix(
            createSimplePrefix("Kakashka", Formatting.WHITE)
        ));

        // дамб для лентауха
        PREFIX_MAP.put("lentauhc", new PlayerPrefix(
            createSimplePrefix("Dumb", Formatting.WHITE)
        ));

        // новые префиксы
        // долбоёб для бдб
        PREFIX_MAP.put("123bdb123", new PlayerPrefix(
            createAegisPrefix("dolboeb", Formatting.GREEN, true, false, true)
        ));

        // подсос для софта
        PREFIX_MAP.put("software", new PlayerPrefix(
            createAegisPrefix("podsos", Formatting.GOLD, true, false)
        ));

        // босс жалоб для астреда
        PREFIX_MAP.put("astre1d", new PlayerPrefix(
            createAegisPrefix("Босс жалоб", Formatting.DARK_PURPLE, true, true)
        ));

        // пидорази для раззи
        PREFIX_MAP.put("_razzyy_", new PlayerPrefix(
            createAegisPrefix("Pidorazzy", Formatting.DARK_PURPLE, true, false, true)
        ));

        // лисёнок для фокси
        PREFIX_MAP.put("_foxi4k_", new PlayerPrefix(
            createAegisPrefix("lisenok", Formatting.DARK_GREEN, true, false)
        ));

        // пидар для ридапа
        PREFIX_MAP.put("ridap", new PlayerPrefix(
            createAegisPrefix("pidaR", Formatting.WHITE, true, true)
        ));

        // грейтест для бич
        PREFIX_MAP.put("ii_blackbitch_ii", new PlayerPrefix(
            createAegisPrefix("Greatest", Formatting.LIGHT_PURPLE, true, false, true)
        ));

        // пидар для демпдиза
        PREFIX_MAP.put("dempdeez", new PlayerPrefix(
            createAegisPrefix("pidaR", Formatting.WHITE, true, true)
        ));

        // говно для ву
        PREFIX_MAP.put("woo", new PlayerPrefix(
            createAegisPrefix("gavnoo", Formatting.DARK_GRAY, true, false)
        ));

        // говнурка для амурки
        PREFIX_MAP.put("amurka777", new PlayerPrefix(
            createAegisPrefix("govnurka666", Formatting.GRAY, true, false, true)
        ));

        // онмуфейс для кочерги
        PREFIX_MAP.put("_kacherga", new PlayerPrefix(
            createAegisPrefix("onmyface!", Formatting.YELLOW, true, false)
        ));

        // аутизм для палок
        PREFIX_MAP.put("____", new PlayerPrefix(
            createAegisPrefix("Autism", Formatting.AQUA, true, true, true)
        ));

        // аутизм для хуани
        PREFIX_MAP.put("xyanya", new PlayerPrefix(
            createAegisPrefix("Autism", Formatting.AQUA, true, true, true)
        ));

        // важным для вайзу
        PREFIX_MAP.put("vaizu", new PlayerPrefix(
            createAegisPrefix("important", Formatting.GREEN, true, true)
        ));

        // год для женро
        PREFIX_MAP.put("jenro0", new PlayerPrefix(
            createAegisPrefix("god", Formatting.GOLD, true, true, true)
        ));

        // еблан для олди
        PREFIX_MAP.put("syka_oldi", new PlayerPrefix(
            createAegisPrefix("eblan", Formatting.DARK_GRAY, true, false)
        ));

        // ризз для мисс
        PREFIX_MAP.put("miss", new PlayerPrefix(
            createAegisPrefix("rizz", Formatting.LIGHT_PURPLE, true, false, true)
        ));

        // ещё префиксы

        // льйоша для клуреса
        PREFIX_MAP.put("cloures", new PlayerPrefix(
            createAegisPrefix("Льйоша", Formatting.AQUA, true, false, true)
        ));

        // говноед для слипа
        PREFIX_MAP.put("slipe", new PlayerPrefix(
            createAegisPrefix("Govnoed", Formatting.DARK_GREEN, true, false)
        ));

        // аутизм для джавы
        PREFIX_MAP.put("java45", new PlayerPrefix(
            createAegisPrefix("Autism", Formatting.AQUA, true, true, true)
        ));

        // гризюля для гриза
        PREFIX_MAP.put("mr_grize", new PlayerPrefix(
            createAegisPrefix("Гризюля", Formatting.YELLOW, true, true)
        ));

        // гей для воронда
        PREFIX_MAP.put("vorond123", new PlayerPrefix(
            createAegisPrefix("Gay", Formatting.LIGHT_PURPLE, true, false, true)
        ));
    }

    /**
     * клепаем префикс эгиса
     */
    private static MutableText createAegisPrefix(String suffix, Formatting color, boolean bold, boolean underline) {
        return createAegisPrefix(suffix, color, bold, underline, false);
    }

    /**
     * клепаем префикс эгиса
     */
    private static MutableText createAegisPrefix(String suffix, Formatting color, boolean bold, boolean underline, boolean italic) {
        // палка разделитель
        MutableText separator = Text.literal(" | ").formatted(Formatting.GRAY);
        
        // эгис фиолетовый
        MutableText aegis = Text.literal("Aegis")
            .formatted(Formatting.LIGHT_PURPLE)
            .styled(style -> style.withUnderline(true));
            
        // двоеточие лепим
        MutableText colon = Text.literal(" : ").formatted(Formatting.GRAY);

        // суффикс как сказали
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
     * простой префикс лепим
     */
    private static MutableText createSimplePrefix(String text, Formatting color) {
        MutableText separator = Text.literal(" | ").formatted(Formatting.GRAY);
        MutableText textPart = Text.literal(text).formatted(color, Formatting.BOLD);
        return Text.empty().append(separator).append(textPart);
    }

    /**
     * есть ли префикс
     */
    public static boolean hasPrefix(String playerName) {
        return PREFIX_MAP.containsKey(playerName.toLowerCase());
    }

    /**
     * даём префикс игроку
     */
    public static MutableText getPrefix(String playerName) {
        PlayerPrefix prefix = PREFIX_MAP.get(playerName.toLowerCase());
        if (prefix != null) {
            return prefix.prefixText.copy();
        }
        return null;
    }

    /**
     * ник вместе с префиксом
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
     * коробка под префикс
     */
    private static class PlayerPrefix {
        private final MutableText prefixText;

        public PlayerPrefix(MutableText prefixText) {
            this.prefixText = prefixText;
        }
    }
}
