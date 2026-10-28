package rich.modules.impl.misc.autoparser.dev;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.string.chat.ChatMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

@Getter
public class ItemParser extends ModuleStructure {
    private static ItemParser instance;

    private final BooleanSetting showInChat = new BooleanSetting("Показывать в чате", "").setValue(true);
    private final BooleanSetting saveToFile = new BooleanSetting("Сохранять в файл", "").setValue(true);

    private int parseCounter = 0;

    private static final Set<String> IGNORED_ITEMS = Set.of(
            "minecraft:glass_pane",
            "minecraft:white_stained_glass_pane",
            "minecraft:orange_stained_glass_pane",
            "minecraft:magenta_stained_glass_pane",
            "minecraft:light_blue_stained_glass_pane",
            "minecraft:yellow_stained_glass_pane",
            "minecraft:lime_stained_glass_pane",
            "minecraft:pink_stained_glass_pane",
            "minecraft:gray_stained_glass_pane",
            "minecraft:light_gray_stained_glass_pane",
            "minecraft:cyan_stained_glass_pane",
            "minecraft:purple_stained_glass_pane",
            "minecraft:blue_stained_glass_pane",
            "minecraft:brown_stained_glass_pane",
            "minecraft:green_stained_glass_pane",
            "minecraft:red_stained_glass_pane",
            "minecraft:black_stained_glass_pane",
            "minecraft:glass",
            "minecraft:white_stained_glass",
            "minecraft:orange_stained_glass",
            "minecraft:magenta_stained_glass",
            "minecraft:light_blue_stained_glass",
            "minecraft:yellow_stained_glass",
            "minecraft:lime_stained_glass",
            "minecraft:pink_stained_glass",
            "minecraft:gray_stained_glass",
            "minecraft:light_gray_stained_glass",
            "minecraft:cyan_stained_glass",
            "minecraft:purple_stained_glass",
            "minecraft:blue_stained_glass",
            "minecraft:brown_stained_glass",
            "minecraft:green_stained_glass",
            "minecraft:red_stained_glass",
            "minecraft:black_stained_glass",
            "minecraft:air",
            "minecraft:barrier"
    );

    private static final Map<String, String> EFFECT_TO_STATUSEFFECTS = Map.ofEntries(
            Map.entry("speed", "StatusEffects.SPEED"),
            Map.entry("slowness", "StatusEffects.SLOWNESS"),
            Map.entry("haste", "StatusEffects.HASTE"),
            Map.entry("mining_fatigue", "StatusEffects.MINING_FATIGUE"),
            Map.entry("strength", "StatusEffects.STRENGTH"),
            Map.entry("instant_health", "StatusEffects.INSTANT_HEALTH"),
            Map.entry("instant_damage", "StatusEffects.INSTANT_DAMAGE"),
            Map.entry("jump_boost", "StatusEffects.JUMP_BOOST"),
            Map.entry("nausea", "StatusEffects.NAUSEA"),
            Map.entry("regeneration", "StatusEffects.REGENERATION"),
            Map.entry("resistance", "StatusEffects.RESISTANCE"),
            Map.entry("fire_resistance", "StatusEffects.FIRE_RESISTANCE"),
            Map.entry("water_breathing", "StatusEffects.WATER_BREATHING"),
            Map.entry("invisibility", "StatusEffects.INVISIBILITY"),
            Map.entry("blindness", "StatusEffects.BLINDNESS"),
            Map.entry("night_vision", "StatusEffects.NIGHT_VISION"),
            Map.entry("hunger", "StatusEffects.HUNGER"),
            Map.entry("weakness", "StatusEffects.WEAKNESS"),
            Map.entry("poison", "StatusEffects.POISON"),
            Map.entry("wither", "StatusEffects.WITHER"),
            Map.entry("health_boost", "StatusEffects.HEALTH_BOOST"),
            Map.entry("absorption", "StatusEffects.ABSORPTION"),
            Map.entry("saturation", "StatusEffects.SATURATION"),
            Map.entry("glowing", "StatusEffects.GLOWING"),
            Map.entry("levitation", "StatusEffects.LEVITATION"),
            Map.entry("luck", "StatusEffects.LUCK"),
            Map.entry("unluck", "StatusEffects.UNLUCK"),
            Map.entry("slow_falling", "StatusEffects.SLOW_FALLING"),
            Map.entry("conduit_power", "StatusEffects.CONDUIT_POWER"),
            Map.entry("dolphins_grace", "StatusEffects.DOLPHINS_GRACE"),
            Map.entry("bad_omen", "StatusEffects.BAD_OMEN"),
            Map.entry("hero_of_the_village", "StatusEffects.HERO_OF_THE_VILLAGE"),
            Map.entry("darkness", "StatusEffects.DARKNESS")
    );

    public ItemParser() {
        super("Item Parser", "Парсинг информации о предметах", ModuleCategory.MISC);
        instance = this;
        settings(showInChat, saveToFile);
    }

    public static ItemParser getInstance() {
        return instance;
    }

    public void parseAllSlots(List<Slot> slots, int containerSize, String containerTitle) {
        parseCounter++;
        StringBuilder info = new StringBuilder();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        info.append("// ПАРСИНГ #").append(parseCounter).append(" | ").append(timestamp).append("\n");
        info.append("// Контейнер: ").append(containerTitle).append("\n\n");

        int itemCount = 0;

        for (int i = 0; i < containerSize && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (IGNORED_ITEMS.contains(itemId)) continue;

            itemCount++;
            info.append("// --- СЛОТ ").append(i).append(" ---\n");
            parseItemCompact(stack, info);
            info.append("\n");
        }

        info.append("// ИТОГО: ").append(itemCount).append(" предметов\n");

        String result = info.toString();

        if (showInChat.isValue()) {
            ChatMessage.autobuymessage("§6Парсинг #" + parseCounter + " | §bПредметов: §f" + itemCount);
        }

        if (saveToFile.isValue()) {
            saveToFile(result, parseCounter);
            ChatMessage.autobuymessageSuccess("Файл: parse_" + parseCounter + ".txt");
        }
    }

    private void parseItemCompact(ItemStack stack, StringBuilder info) {
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        String displayName = customName != null ? customName.getString() : stack.getName().getString();

        info.append("// ").append(displayName).append(" (").append(itemId).append(")\n");

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            info.append("List<Text> lore = List.of(\n");
            for (int i = 0; i < lore.lines().size(); i++) {
                String line = lore.lines().get(i).getString();
                if (line.trim().isEmpty()) continue;
                info.append("    Text.literal(\"").append(escapeString(line)).append("\")");
                if (i < lore.lines().size() - 1) info.append(",");
                info.append("\n");
            }
            info.append(");\n");
        }

        if (stack.getItem() == Items.PLAYER_HEAD) {
            generateHeadCode(stack, displayName, info);
        } else if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            generateTalismanCode(displayName, info);
        } else if (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
            generatePotionCode(stack, displayName, info);
        } else {
            generateGenericCode(stack, displayName, info);
        }
    }

    private void generateHeadCode(ItemStack stack, String displayName, StringBuilder info) {
        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) {
            info.append("// НЕТ ПРОФИЛЯ\n");
            return;
        }

        var gameProfile = profile.getGameProfile();
        String uuid = gameProfile.id() != null ? gameProfile.id().toString() : "unknown";
        String texture = "";

        var textures = gameProfile.properties().get("textures");
        if (textures != null && !textures.isEmpty()) {
            for (var property : textures) {
                texture = property.value();
                break;
            }
        }

        String cleanName = displayName.replace("[★] ", "");
        info.append("spheres.add(createSphere(\"").append(displayName).append("\", \"")
                .append(uuid).append("\", \"").append(texture).append("\", ")
                .append("Defaultpricec.getPrice(\"").append(cleanName).append("\"), lore));\n");
    }

    private void generateTalismanCode(String displayName, StringBuilder info) {
        String cleanName = displayName.replace("[★] ", "");
        info.append("talismans.add(new CustomItem(\"").append(displayName)
                .append("\", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice(\"")
                .append(cleanName).append("\"), null, lore));\n");
    }

    private void generatePotionCode(ItemStack stack, String displayName, StringBuilder info) {
        String itemType;
        if (stack.getItem() == Items.SPLASH_POTION) {
            itemType = "SPLASH_POTION";
        } else if (stack.getItem() == Items.LINGERING_POTION) {
            itemType = "LINGERING_POTION";
        } else {
            itemType = "POTION";
        }

        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);

        if (potionContents != null) {
            List<StatusEffectInstance> effects = new ArrayList<>();
            for (StatusEffectInstance effect : potionContents.getEffects()) {
                effects.add(effect);
            }

            int color = potionContents.getColor();
            String colorHex = String.format("0x%06X", color & 0xFFFFFF);

            if (!effects.isEmpty()) {
                info.append("List<StatusEffectInstance> ").append(toVariableName(displayName)).append("Effects = List.of(\n");
                for (int i = 0; i < effects.size(); i++) {
                    StatusEffectInstance effect = effects.get(i);
                    String effectId = effect.getEffectType().getIdAsString();
                    if (effectId != null) {
                        effectId = effectId.replace("minecraft:", "");
                    }
                    String statusEffect = EFFECT_TO_STATUSEFFECTS.getOrDefault(effectId, "StatusEffects." + effectId.toUpperCase());
                    int duration = effect.getDuration();
                    int amplifier = effect.getAmplifier();

                    info.append("        new StatusEffectInstance(").append(statusEffect)
                            .append(", ").append(duration)
                            .append(", ").append(amplifier).append(")");
                    if (i < effects.size() - 1) info.append(",");
                    info.append(" // ").append(effectId).append(" lvl:").append(amplifier + 1)
                            .append(" dur:").append(formatDuration(duration)).append("\n");
                }
                info.append(");\n");

                String cleanName = displayName.replace("[★] ", "").replace("[🍹] ", "").replace("[$] ", "");
                info.append("potions.add(new CustomItem(\"").append(displayName)
                        .append("\", null, Items.").append(itemType)
                        .append(", Defaultpricec.getPrice(\"").append(cleanName).append("\"),\n");
                info.append("        new PotionContentsComponent(Optional.empty(), Optional.of(")
                        .append(colorHex).append("), ").append(toVariableName(displayName))
                        .append("Effects, Optional.empty()), lore));\n");
            } else {
                info.append("// Зелье без эффектов, цвет: ").append(colorHex).append("\n");
                String cleanName = displayName.replace("[★] ", "").replace("[🍹] ", "").replace("[$] ", "");
                info.append("potions.add(new CustomItem(\"").append(displayName)
                        .append("\", null, Items.").append(itemType).append(", Defaultpricec.getPrice(\"")
                        .append(cleanName).append("\"), null, lore));\n");
            }
        } else {
            info.append("// Нет PotionContentsComponent\n");
            String cleanName = displayName.replace("[★] ", "").replace("[🍹] ", "").replace("[$] ", "");
            info.append("potions.add(new CustomItem(\"").append(displayName)
                    .append("\", null, Items.").append(itemType).append(", Defaultpricec.getPrice(\"")
                    .append(cleanName).append("\"), null, lore));\n");
        }
    }

    private void generateGenericCode(ItemStack stack, String displayName, StringBuilder info) {
        String itemConst = Registries.ITEM.getId(stack.getItem()).getPath().toUpperCase();
        info.append("items.add(new CustomItem(\"").append(displayName)
                .append("\", null, Items.").append(itemConst).append(", price, null, lore));\n");
    }

    private String toVariableName(String displayName) {
        String clean = displayName
                .replace("[★] ", "")
                .replace("[🍹] ", "")
                .replace("[$] ", "")
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "");

        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (i == 0) {
                    sb.append(Character.toLowerCase(c));
                } else if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(c);
                }
            } else {
                nextUpper = true;
            }
        }

        if (sb.length() == 0) return "potion";
        return sb.toString();
    }

    private String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void saveToFile(String content, int number) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            File dir = new File(mc.runDirectory, "item_parser");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "parse_" + number + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.print(content);
            }
        } catch (IOException e) {
            ChatMessage.autobuymessageError("Ошибка: " + e.getMessage());
        }
    }
}