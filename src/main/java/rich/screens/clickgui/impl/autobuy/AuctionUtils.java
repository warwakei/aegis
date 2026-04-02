package rich.screens.clickgui.impl.autobuy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuctionUtils {
    public static final Pattern funTimePricePattern = Pattern.compile("\\$([\\d]+(?:[\\s,][\\d]{3})*(?:\\.[\\d]{2})?)");
    private static final Pattern digitPattern = Pattern.compile("([\\d][\\d\\s,.]*)");

    public static int getPrice(ItemStack stack) {
        String priceStr = null;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            for (Text line : lore.lines()) {
                String lineStr = line.getString();
                if (lineStr.contains("$") || lineStr.toLowerCase().contains("цена")) {
                    Matcher matcher = funTimePricePattern.matcher(lineStr);
                    if (matcher.find()) {
                        priceStr = matcher.group(1);
                        break;
                    }
                    matcher = digitPattern.matcher(lineStr);
                    if (matcher.find()) {
                        priceStr = matcher.group(1);
                        break;
                    }
                }
            }
        }
        if (priceStr == null || priceStr.isEmpty()) {
            String itemName = stack.getName().getString();
            if (itemName != null) {
                Matcher matcher = funTimePricePattern.matcher(itemName);
                if (matcher.find()) {
                    priceStr = matcher.group(1);
                }
            }
        }
        if (priceStr == null || priceStr.isEmpty()) {
            var components = stack.getComponents();
            if (components != null) {
                String componentString = components.toString();
                if (componentString.contains("$")) {
                    Matcher matcher = funTimePricePattern.matcher(componentString);
                    if (matcher.find()) {
                        priceStr = matcher.group(1);
                    }
                }
            }
        }
        if (priceStr == null || priceStr.isEmpty()) return -1;
        try {
            priceStr = priceStr.replaceAll("[\\s,.$]", "").trim();
            if (priceStr.isEmpty()) return -1;
            return Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String cleanString(String str) {
        if (str == null) return "";
        return str.toLowerCase().trim()
                .replaceAll("§.", "")
                .replaceAll("[^a-zа-яё0-9\\s\\[\\]★⚒+()]", "")
                .replaceAll("\\s+", " ");
    }

    private static List<String> getLoreStrings(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return List.of();
        }
        return lore.lines().stream()
                .map(text -> text.getString().toLowerCase())
                .collect(Collectors.toList());
    }

    private static boolean loreContains(ItemStack stack, String phrase) {
        List<String> loreLines = getLoreStrings(stack);
        String phraseLower = phrase.toLowerCase();
        for (String line : loreLines) {
            if (line.contains(phraseLower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean loreContainsAny(ItemStack stack, String... phrases) {
        List<String> loreLines = getLoreStrings(stack);
        for (String phrase : phrases) {
            String phraseLower = phrase.toLowerCase();
            for (String line : loreLines) {
                if (line.contains(phraseLower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String extractChunkLoaderSize(ItemStack stack) {
        List<String> loreLines = getLoreStrings(stack);
        for (String line : loreLines) {
            if (line.contains("(1x1)") || line.contains("области (1x1)")) return "1x1";
            if (line.contains("(3x3)") || line.contains("области (3x3)")) return "3x3";
            if (line.contains("(5x5)") || line.contains("области (5x5)")) return "5x5";
        }
        return null;
    }

    private static boolean isTntBlackType(ItemStack stack) {
        return loreContains(stack, "обсидиан") || loreContains(stack, "способен взорвать обсидиан");
    }

    private static String getLockpickType(ItemStack stack) {
        List<String> loreLines = getLoreStrings(stack);
        for (String line : loreLines) {
            if (line.contains("с сферами") || line.contains("сферами")) return "spheres";
            if (line.contains("с ключами") || line.contains("ключами")) return "keys";
            if (line.contains("с монетами") || line.contains("монетами")) return "coins";
        }
        return "unknown";
    }

    private static boolean isDragonSkin(ItemStack stack) {
        return loreContains(stack, "драконий скин");
    }

    private static String getSkinType(ItemStack stack) {
        List<String> loreLines = getLoreStrings(stack);
        for (String line : loreLines) {
            if (line.contains("драконий скин")) return "dragon";
            if (line.contains("ледяной скин")) return "ice";
            if (line.contains("огненный скин")) return "fire";
        }
        return "unknown";
    }

    private static boolean isValidTrap(ItemStack stack) {
        return loreContains(stack, "нерушимая клетка");
    }

    private static String getTrapSkinType(ItemStack stack) {
        if (!isValidTrap(stack)) {
            return "invalid";
        }
        List<String> loreLines = getLoreStrings(stack);
        for (String line : loreLines) {
            if (line.contains("драконий") || line.contains("dragon")) return "dragon";
            if (line.contains("ледяной") || line.contains("ледян") || line.contains("ice")) return "ice";
            if (line.contains("огненный") || line.contains("fire")) return "fire";
        }
        return "standard";
    }

    private static String getSignalFireLootLevel(ItemStack stack) {
        List<String> loreLines = getLoreStrings(stack);
        for (String line : loreLines) {
            if (line.contains("уровень лута:") || line.contains("уровень лута")) {
                if (line.contains("легендарный")) return "legendary";
                if (line.contains("богатый")) return "rich";
                if (line.contains("обычный")) return "ordinary";
                if (line.contains("случайный")) return "random";
            }
        }
        return "unknown";
    }

    private static boolean isSignalFire(ItemStack stack) {
        return stack.getItem() == Items.CAMPFIRE || stack.getItem() == Items.SOUL_CAMPFIRE;
    }

    private static boolean isValidSignalFire(ItemStack stack) {
        return isSignalFire(stack) && loreContains(stack, "мистический сундук");
    }

    private static boolean isValidLockpick(ItemStack stack) {
        return loreContainsAny(stack, "открыть хранилище", "этой отмычкой можно");
    }

    private static boolean isValidExperienceBottle(ItemStack stack) {
        return loreContainsAny(stack, "содержит", "ур опыта", "ур. опыта");
    }

    private static boolean isValidTnt(ItemStack stack) {
        return loreContains(stack, "динамит взрывается");
    }

    private static boolean isValidDragonSkin(ItemStack stack) {
        return loreContains(stack, "драконий скин");
    }

    private static boolean isValidChunkLoader(ItemStack stack) {
        return loreContains(stack, "прогружает чанк");
    }

    public static boolean isArmorItem(ItemStack stack) {
        return stack.getItem() == Items.NETHERITE_HELMET ||
                stack.getItem() == Items.NETHERITE_CHESTPLATE ||
                stack.getItem() == Items.NETHERITE_LEGGINGS ||
                stack.getItem() == Items.NETHERITE_BOOTS ||
                stack.getItem() == Items.DIAMOND_HELMET ||
                stack.getItem() == Items.DIAMOND_CHESTPLATE ||
                stack.getItem() == Items.DIAMOND_LEGGINGS ||
                stack.getItem() == Items.DIAMOND_BOOTS ||
                stack.getItem() == Items.IRON_HELMET ||
                stack.getItem() == Items.IRON_CHESTPLATE ||
                stack.getItem() == Items.IRON_LEGGINGS ||
                stack.getItem() == Items.IRON_BOOTS ||
                stack.getItem() == Items.GOLDEN_HELMET ||
                stack.getItem() == Items.GOLDEN_CHESTPLATE ||
                stack.getItem() == Items.GOLDEN_LEGGINGS ||
                stack.getItem() == Items.GOLDEN_BOOTS ||
                stack.getItem() == Items.CHAINMAIL_HELMET ||
                stack.getItem() == Items.CHAINMAIL_CHESTPLATE ||
                stack.getItem() == Items.CHAINMAIL_LEGGINGS ||
                stack.getItem() == Items.CHAINMAIL_BOOTS ||
                stack.getItem() == Items.LEATHER_HELMET ||
                stack.getItem() == Items.LEATHER_CHESTPLATE ||
                stack.getItem() == Items.LEATHER_LEGGINGS ||
                stack.getItem() == Items.LEATHER_BOOTS ||
                stack.getItem() == Items.TURTLE_HELMET;
    }

    public static boolean hasThornsEnchantment(ItemStack stack) {
        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            return false;
        }
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            String enchantId = entry.getIdAsString();
            if (enchantId != null) {
                String lowerEnchantId = enchantId.toLowerCase();
                if (lowerEnchantId.contains("thorns") || lowerEnchantId.contains("шип")) {
                    return true;
                }
            }
        }
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String loreStr = line.getString().toLowerCase();
                if (loreStr.contains("thorns") || loreStr.contains("шип")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            return false;
        }
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            String enchantId = entry.getIdAsString();
            if (enchantId != null && enchantId.toLowerCase().contains("vanishing")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnbreakableItem(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.getBoolean("Unbreakable", false)) {
                return true;
            }
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("нерушим") || name.contains("[⚒]");
    }

    public static boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION;
    }

    public static Map<RegistryEntry<StatusEffect>, EffectData> getPotionEffects(ItemStack stack) {
        Map<RegistryEntry<StatusEffect>, EffectData> effects = new HashMap<>();
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {
            return effects;
        }
        for (StatusEffectInstance effect : potionContents.customEffects()) {
            effects.put(effect.getEffectType(), new EffectData(effect.getAmplifier(), effect.getDuration()));
        }
        return effects;
    }

    public static boolean hasEffect(ItemStack stack, RegistryEntry<StatusEffect> effectType, int minAmplifier) {
        Map<RegistryEntry<StatusEffect>, EffectData> effects = getPotionEffects(stack);
        EffectData data = effects.get(effectType);
        return data != null && data.amplifier >= minAmplifier;
    }

    public static boolean matchesPotionEffects(ItemStack auctionItem, List<PotionEffectRequirement> requirements) {
        if (!isSplashPotion(auctionItem)) {
            return false;
        }
        Map<RegistryEntry<StatusEffect>, EffectData> auctionEffects = getPotionEffects(auctionItem);
        if (auctionEffects.isEmpty()) {
            return false;
        }
        for (PotionEffectRequirement req : requirements) {
            EffectData data = auctionEffects.get(req.effect);
            if (data == null) {
                return false;
            }
            if (data.amplifier < req.minAmplifier) {
                return false;
            }
        }
        return true;
    }

    public static class EffectData {
        public final int amplifier;
        public final int duration;

        public EffectData(int amplifier, int duration) {
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }

    public static class PotionEffectRequirement {
        public final RegistryEntry<StatusEffect> effect;
        public final int minAmplifier;

        public PotionEffectRequirement(RegistryEntry<StatusEffect> effect, int minAmplifier) {
            this.effect = effect;
            this.minAmplifier = minAmplifier;
        }
    }

    public static boolean compareItem(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) {
            if (isSignalFire(a) && isSignalFire(b)) {
            } else {
                return false;
            }
        }

        if (isArmorItem(a) && hasThornsEnchantment(a)) {
            return false;
        }

        if (a.getItem() == Items.NETHERITE_SCRAP) {
            if (!isValidTrap(a)) {
                return false;
            }
            if (!isValidTrap(b)) {
                return true;
            }
            String aType = getTrapSkinType(a);
            String bType = getTrapSkinType(b);
            if (bType.equals("standard")) {
                return aType.equals("standard") || aType.equals("dragon") || aType.equals("ice") || aType.equals("fire");
            }
            return aType.equals(bType);
        }

        if (isSignalFire(a) && isSignalFire(b)) {
            if (!isValidSignalFire(a)) {
                return false;
            }
            String aLevel = getSignalFireLootLevel(a);
            String bLevel = getSignalFireLootLevel(b);
            if (!bLevel.equals("unknown")) {
                if (!aLevel.equals(bLevel)) {
                    return false;
                }
            }
            return true;
        }

        if (a.getItem() == Items.STRUCTURE_BLOCK) {
            if (!isValidChunkLoader(a)) {
                return false;
            }
            String aSize = extractChunkLoaderSize(a);
            String bSize = extractChunkLoaderSize(b);
            if (bSize != null) {
                if (aSize == null || !aSize.equals(bSize)) {
                    return false;
                }
            }
            return true;
        }

        if (a.getItem() == Items.TNT) {
            if (!isValidTnt(a)) {
                return false;
            }
            boolean aIsBlack = isTntBlackType(a);
            boolean bIsBlack = isTntBlackType(b);
            if (aIsBlack != bIsBlack) {
                return false;
            }
            return true;
        }

        if (a.getItem() == Items.TRIPWIRE_HOOK) {
            if (!isValidLockpick(a)) {
                return false;
            }
            String aType = getLockpickType(a);
            String bType = getLockpickType(b);
            if (!bType.equals("unknown")) {
                if (!aType.equals(bType)) {
                    return false;
                }
            }
            return true;
        }

        if (a.getItem() == Items.PAPER) {
            boolean bIsDragon = isDragonSkin(b);
            if (bIsDragon) {
                if (!isValidDragonSkin(a)) {
                    return false;
                }
            }
            String aType = getSkinType(a);
            String bType = getSkinType(b);
            if (!bType.equals("unknown")) {
                if (!aType.equals(bType)) {
                    return false;
                }
            }
            return true;
        }

        if (a.getItem() == Items.EXPERIENCE_BOTTLE) {
            var bLoreComp = b.get(DataComponentTypes.LORE);
            if (bLoreComp != null && !bLoreComp.lines().isEmpty()) {
                if (!isValidExperienceBottle(a)) {
                    return false;
                }
                String aExpLevel = getExperienceLevel(a);
                String bExpLevel = getExperienceLevel(b);
                if (!bExpLevel.equals("unknown") && !aExpLevel.equals(bExpLevel)) {
                    return false;
                }
            }
            return true;
        }

        String aName = a.getName().getString();
        aName = funTimePricePattern.matcher(aName).replaceAll("").trim();
        String bName = b.getName().getString();
        String aNameClean = cleanString(aName);
        String bNameClean = cleanString(bName);

        if (bNameClean.contains("⚒") || bNameClean.contains("нерушим")) {
            if (!isUnbreakableItem(a) && !hasVanishingCurse(a)) {
                return false;
            }
            if (aNameClean.contains("нерушим") && bNameClean.contains("нерушим")) {
                return aNameClean.contains("элитр") && bNameClean.contains("элитр");
            }
        }

        var aLore = a.get(DataComponentTypes.LORE);
        var bLoreComp = b.get(DataComponentTypes.LORE);
        boolean hasLore = bLoreComp != null && !bLoreComp.lines().isEmpty();

        if (isSplashPotion(a) && isSplashPotion(b)) {
            return comparePotionsByEffects(a, b);
        }

        if (hasLore) {
            List<Text> expectedLore = bLoreComp.lines();
            if (aLore == null || aLore.lines().isEmpty()) {
                return false;
            }

            List<String> auctionLoreStrings = aLore.lines().stream()
                    .map(text -> cleanString(text.getString()))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            String auctionLoreJoined = String.join(" ", auctionLoreStrings);

            List<String> criticalPhrases = List.of(
                    "с сферами",
                    "сферами",
                    "драконий скин",
                    "обсидиан",
                    "способен взорвать обсидиан",
                    "области (1x1)",
                    "области (3x3)",
                    "области (5x5)",
                    "нерушимая клетка",
                    "tier black",
                    "tier white",
                    "уровень лута легендарный",
                    "уровень лута богатый",
                    "уровень лута обычный",
                    "уровень лута случайный",
                    "мистический сундук",
                    "прогружает чанк",
                    "динамит взрывается",
                    "открыть хранилище"
            );

            for (Text expected : expectedLore) {
                String expectedStr = cleanString(expected.getString());
                if (expectedStr.isEmpty()) continue;

                for (String critical : criticalPhrases) {
                    if (expectedStr.contains(critical)) {
                        boolean foundInAuction = false;
                        for (String auctionLine : auctionLoreStrings) {
                            if (auctionLine.contains(critical)) {
                                foundInAuction = true;
                                break;
                            }
                        }
                        if (!foundInAuction && !auctionLoreJoined.contains(critical)) {
                            return false;
                        }
                    }
                }
            }

            boolean hasOriginalMarker = false;
            boolean hasUnbreakableMarker = false;
            for (String line : auctionLoreStrings) {
                if (line.contains("оригинальный предмет") || line.contains("★")) {
                    hasOriginalMarker = true;
                }
                if (line.contains("нерушим") || line.contains("⚒")) {
                    hasUnbreakableMarker = true;
                }
            }

            int matchCount = 0;
            int requiredMatches = 0;

            for (Text expected : expectedLore) {
                String expectedStr = cleanString(expected.getString());
                if (expectedStr.isEmpty()) continue;

                boolean isOriginalMarker = expectedStr.contains("оригинальный предмет") || expectedStr.contains("★");
                boolean isUnbreakableMarker = expectedStr.contains("нерушим") || expectedStr.contains("⚒");

                if (isOriginalMarker) {
                    if (!hasOriginalMarker) {
                        return false;
                    }
                    matchCount++;
                    requiredMatches++;
                    continue;
                }

                if (isUnbreakableMarker) {
                    if (!hasUnbreakableMarker && !isUnbreakableItem(a) && !hasVanishingCurse(a)) {
                        return false;
                    }
                    matchCount++;
                    requiredMatches++;
                    continue;
                }

                requiredMatches++;
                boolean found = false;

                for (String auctionLine : auctionLoreStrings) {
                    if (auctionLine.contains(expectedStr) || expectedStr.contains(auctionLine)) {
                        found = true;
                        break;
                    }
                }

                if (!found && auctionLoreJoined.contains(expectedStr)) {
                    found = true;
                }

                if (found) {
                    matchCount++;
                }
            }

            double matchRatio = requiredMatches > 0 ? (double) matchCount / requiredMatches : 1.0;
            if (matchRatio < 0.7) {
                return false;
            }

            if (hasOriginalMarker) {
                var aEnchants = a.get(DataComponentTypes.ENCHANTMENTS);
                var bEnchants = b.get(DataComponentTypes.ENCHANTMENTS);

                if (bEnchants != null && !bEnchants.isEmpty()) {
                    if (aEnchants == null || aEnchants.isEmpty()) {
                        return false;
                    }

                    Map<String, Integer> aEnchantMap = new HashMap<>();
                    for (RegistryEntry<Enchantment> entry : aEnchants.getEnchantments()) {
                        String enchantId = entry.getIdAsString();
                        if (enchantId != null) {
                            String enchantName = enchantId.replace("minecraft:", "").toLowerCase();
                            int level = aEnchants.getLevel(entry);
                            aEnchantMap.put(enchantName, level);
                        }
                    }

                    Map<String, Integer> bEnchantMap = new HashMap<>();
                    for (RegistryEntry<Enchantment> entry : bEnchants.getEnchantments()) {
                        String enchantId = entry.getIdAsString();
                        if (enchantId != null) {
                            String enchantName = enchantId.replace("minecraft:", "").toLowerCase();
                            int level = bEnchants.getLevel(entry);
                            bEnchantMap.put(enchantName, level);
                        }
                    }

                    if (bEnchantMap.isEmpty()) {
                        return true;
                    }

                    int enchantMatchCount = 0;
                    for (Map.Entry<String, Integer> bEntry : bEnchantMap.entrySet()) {
                        String bEnchantName = bEntry.getKey();
                        Integer aLevel = aEnchantMap.get(bEnchantName);
                        if (aLevel != null && aLevel >= 1) {
                            enchantMatchCount++;
                        }
                    }

                    double enchantMatchRatio = (double) enchantMatchCount / bEnchantMap.size();
                    if (enchantMatchRatio < 1) {
                        return false;
                    }
                }
            }
        } else {
            if (!aNameClean.contains(bNameClean) && !bNameClean.contains(aNameClean)) {
                return false;
            }
        }

        return true;
    }

    private static String getExperienceLevel(ItemStack stack) {
        List<String> loreLines = getLoreStrings(stack);
        for (String line : loreLines) {
            if (line.contains("15")) return "15";
            if (line.contains("30")) return "30";
            if (line.contains("50")) return "50";
        }
        return "unknown";
    }

    private static boolean comparePotionsByEffects(ItemStack auctionPotion, ItemStack templatePotion) {
        Map<RegistryEntry<StatusEffect>, EffectData> auctionEffects = getPotionEffects(auctionPotion);
        Map<RegistryEntry<StatusEffect>, EffectData> templateEffects = getPotionEffects(templatePotion);

        if (templateEffects.isEmpty()) {
            return false;
        }
        if (auctionEffects.isEmpty()) {
            return false;
        }

        for (Map.Entry<RegistryEntry<StatusEffect>, EffectData> entry : templateEffects.entrySet()) {
            RegistryEntry<StatusEffect> requiredEffect = entry.getKey();
            int requiredAmplifier = entry.getValue().amplifier;
            EffectData auctionData = auctionEffects.get(requiredEffect);
            if (auctionData == null) {
                return false;
            }
            if (auctionData.amplifier < requiredAmplifier) {
                return false;
            }
        }
        return true;
    }
}