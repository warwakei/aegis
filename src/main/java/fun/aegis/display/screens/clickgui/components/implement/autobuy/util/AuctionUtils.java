package fun.aegis.display.screens.clickgui.components.implement.autobuy.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuctionUtils {
    public static final Pattern funTimePricePattern = Pattern.compile("\\$(\\d+(?:[\\s,]\\d{3})*(?:\\.\\d{2})?)");

    public static int getPrice(ItemStack stack) {
        var tag = stack.getComponents();
        if (tag == null) return -1;

        String priceStr = null;
        String componentString = tag.toString();
        priceStr = StringUtils.substringBetween(componentString, "literal{ $", "}[style={color=green}]");

        if (priceStr == null || priceStr.isEmpty()) {
            String customName = stack.getName().getString();
            if (customName != null) {
                java.util.regex.Matcher matcher = funTimePricePattern.matcher(customName);
                if (matcher.find()) {
                    priceStr = matcher.group(1);
                }
            }
        }

        if (priceStr == null || priceStr.isEmpty()) return -1;

        try {
            priceStr = priceStr.replaceAll("[\\s,]", "");
            return Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String cleanString(String str) {
        if (str == null) return "";
        return str.toLowerCase().trim()
                .replaceAll("§.", "")
                .replaceAll("[^a-zа-яё0-9\\s\\[\\]★+]", "")
                .replaceAll("\\s+", " ");
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

    private static boolean isKillerOriginal(ItemStack stack) {
        if (stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.POTION) {
            return false;
        }

        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {

            return false;
        }

        List<StatusEffectInstance> effects = potionContents.customEffects();
        if (effects.isEmpty()) {
            return false;
        }

        boolean hasStrengthIV = false;

        for (StatusEffectInstance effect : effects) {
            int amplifier = effect.getAmplifier();

            if (effect.getEffectType().matchesKey(StatusEffects.STRENGTH.getKey().get())) {
                if (amplifier >= 3) {
                    hasStrengthIV = true;
                }
            }
        }

        if (hasStrengthIV) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean compareItem(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;

        if (isArmorItem(a) && hasThornsEnchantment(a)) {
            return false;
        }

        String aName = a.getName().getString();
        aName = funTimePricePattern.matcher(aName).replaceAll("").trim();
        String bName = b.getName().getString();

        String aNameClean = cleanString(aName);
        String bNameClean = cleanString(bName);

        var aLore = a.get(DataComponentTypes.LORE);
        var bLoreComp = b.get(DataComponentTypes.LORE);
        boolean hasLore = bLoreComp != null && !bLoreComp.lines().isEmpty();

        boolean isKillerPotionTemplate = false;
        if (hasLore) {
            for (Text expected : bLoreComp.lines()) {
                String expectedStr = expected.getString().toLowerCase();
                if (expectedStr.contains("киллер") || expectedStr.contains("killer")) {
                    isKillerPotionTemplate = true;
                    break;
                }
            }
        }

        if (isKillerPotionTemplate && (a.getItem() == Items.SPLASH_POTION || a.getItem() == Items.POTION)) {
            boolean result = isKillerOriginal(a);
            return result;
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

            boolean hasOriginalMarker = false;

            for (String line : auctionLoreStrings) {
                if (line.contains("оригинальный предмет") || line.contains("★")) {
                    hasOriginalMarker = true;
                }
            }

            int matchCount = 0;
            int requiredMatches = 0;

            for (Text expected : expectedLore) {
                String expectedStr = cleanString(expected.getString());
                if (expectedStr.isEmpty()) continue;

                boolean isOriginalMarker = expectedStr.contains("оригинальный предмет") || expectedStr.contains("★");

                if (isOriginalMarker) {
                    if (!hasOriginalMarker) {
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
            if (matchRatio < 0.5) {
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
}
