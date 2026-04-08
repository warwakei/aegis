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

        // Новые префиксы
        // 123BDB123 - dolboeb (зелёный, жирный, курсив)
        PREFIX_MAP.put("123bdb123", new PlayerPrefix(
            createAegisPrefix("dolboeb", Formatting.GREEN, true, false, true)
        ));

        // software - podsos (золотой, жирный)
        PREFIX_MAP.put("software", new PlayerPrefix(
            createAegisPrefix("podsos", Formatting.GOLD, true, false)
        ));

        // AsTRe1d - Босс жалоб (тёмно-фиолетовый, жирный, подчеркнутый)
        PREFIX_MAP.put("astre1d", new PlayerPrefix(
            createAegisPrefix("Босс жалоб", Formatting.DARK_PURPLE, true, true)
        ));

        // _Razzyy_ - Pidorazzy (фиолетовый, жирный, курсив)
        PREFIX_MAP.put("_razzyy_", new PlayerPrefix(
            createAegisPrefix("Pidorazzy", Formatting.DARK_PURPLE, true, false, true)
        ));

        // _FOXi4k_ - lisenok (тёмно-зелёный, жирный)
        PREFIX_MAP.put("_foxi4k_", new PlayerPrefix(
            createAegisPrefix("lisenok", Formatting.DARK_GREEN, true, false)
        ));

        // Ridap - pidaR (уважаемая персона - белый, жирный, подчеркнутый)
        PREFIX_MAP.put("ridap", new PlayerPrefix(
            createAegisPrefix("pidaR", Formatting.WHITE, true, true)
        ));

        // II_BlackBitch_II - Greatest (крутейшая женщина - розовый, жирный, курсив)
        PREFIX_MAP.put("ii_blackbitch_ii", new PlayerPrefix(
            createAegisPrefix("Greatest", Formatting.LIGHT_PURPLE, true, false, true)
        ));

        // DeMpDeez - pidaR (твинк Ridap - белый, жирный, подчеркнутый)
        PREFIX_MAP.put("dempdeez", new PlayerPrefix(
            createAegisPrefix("pidaR", Formatting.WHITE, true, true)
        ));

        // woo - gavnoo (тёмно-серый, жирный)
        PREFIX_MAP.put("woo", new PlayerPrefix(
            createAegisPrefix("gavnoo", Formatting.DARK_GRAY, true, false)
        ));

        // Amurka777 - govnurka666 (серый, жирный, курсив)
        PREFIX_MAP.put("amurka777", new PlayerPrefix(
            createAegisPrefix("govnurka666", Formatting.GRAY, true, false, true)
        ));

        // _kacherga - onmyface! (жёлтый, жирный)
        PREFIX_MAP.put("_kacherga", new PlayerPrefix(
            createAegisPrefix("onmyface!", Formatting.YELLOW, true, false)
        ));

        // ____ - Autism (голубой, жирный, курсив, подчеркнутый)
        PREFIX_MAP.put("____", new PlayerPrefix(
            createAegisPrefix("Autism", Formatting.AQUA, true, true, true)
        ));

        // xyanya - Autism (голубой, жирный, курсив, подчеркнутый)
        PREFIX_MAP.put("xyanya", new PlayerPrefix(
            createAegisPrefix("Autism", Formatting.AQUA, true, true, true)
        ));

        // vaizu - important (зелёный, жирный, подчеркнутый)
        PREFIX_MAP.put("vaizu", new PlayerPrefix(
            createAegisPrefix("important", Formatting.GREEN, true, true)
        ));

        // JeNro0 - god (золотой, жирный, курсив, подчеркнутый)
        PREFIX_MAP.put("jenro0", new PlayerPrefix(
            createAegisPrefix("god", Formatting.GOLD, true, true, true)
        ));

        // syka_oldi - eblan (тёмно-серый, жирный)
        PREFIX_MAP.put("syka_oldi", new PlayerPrefix(
            createAegisPrefix("eblan", Formatting.DARK_GRAY, true, false)
        ));

        // miss - rizz (розовый, жирный, курсив)
        PREFIX_MAP.put("miss", new PlayerPrefix(
            createAegisPrefix("rizz", Formatting.LIGHT_PURPLE, true, false, true)
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
