package rich.screens.clickgui.impl.autobuy.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KrushItems {

    public static ItemStack getHelmet() {
        ItemStack stack = new ItemStack(Items.NETHERITE_HELMET);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.FIRE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.RESPIRATION, 3));
        enchants.add(new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.BLAST_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.AQUA_AFFINITY, 1));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Шлем Крушителя"),
                List.of(
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getChestplate() {
        ItemStack stack = new ItemStack(Items.NETHERITE_CHESTPLATE);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.BLAST_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.FIRE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Нагрудник Крушителя"),
                List.of(
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getLeggings() {
        ItemStack stack = new ItemStack(Items.NETHERITE_LEGGINGS);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.FIRE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.BLAST_PROTECTION, 5));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Поножи Крушителя"),
                List.of(
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getBoots() {
        ItemStack stack = new ItemStack(Items.NETHERITE_BOOTS);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.FIRE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.SOUL_SPEED, 3));
        enchants.add(new EnchantmentData(Enchantments.FEATHER_FALLING, 4));
        enchants.add(new EnchantmentData(Enchantments.DEPTH_STRIDER, 3));
        enchants.add(new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.BLAST_PROTECTION, 5));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Ботинки Крушителя"),
                List.of(
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getSword() {
        ItemStack stack = new ItemStack(Items.NETHERITE_SWORD);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.SHARPNESS, 7));
        enchants.add(new EnchantmentData(Enchantments.BANE_OF_ARTHROPODS, 7));
        enchants.add(new EnchantmentData(Enchantments.FIRE_ASPECT, 2));
        enchants.add(new EnchantmentData(Enchantments.SWEEPING_EDGE, 3));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.LOOTING, 5));
        enchants.add(new EnchantmentData(Enchantments.SMITE, 7));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Меч Крушителя"),
                List.of(
                        Text.literal("Опытный III").formatted(Formatting.GRAY),
                        Text.literal("Вампиризм II").formatted(Formatting.GRAY),
                        Text.literal("Окисление II").formatted(Formatting.GRAY),
                        Text.literal("Яд III").formatted(Formatting.GRAY),
                        Text.literal("Детекция III").formatted(Formatting.GRAY),
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getPickaxe() {
        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.EFFICIENCY, 10));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.FORTUNE, 5));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Кирка Крушителя"),
                List.of(
                        Text.literal("Бульдозер II").formatted(Formatting.GRAY),
                        Text.literal("Опытный III").formatted(Formatting.GRAY),
                        Text.literal("Магнит").formatted(Formatting.GRAY),
                        Text.literal("Авто-Плавка").formatted(Formatting.GRAY),
                        Text.literal("Паутина").formatted(Formatting.GRAY),
                        Text.literal("Пингер").formatted(Formatting.GRAY),
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getCrossbow() {
        ItemStack stack = new ItemStack(Items.CROSSBOW);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.MULTISHOT, 1));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.PIERCING, 5));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 3));
        enchants.add(new EnchantmentData(Enchantments.QUICK_CHARGE, 3));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Арбалет Крушителя"),
                List.of(
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getBow() {
        ItemStack stack = new ItemStack(Items.BOW);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.POWER, 7));
        enchants.add(new EnchantmentData(Enchantments.PUNCH, 3));
        enchants.add(new EnchantmentData(Enchantments.FLAME, 1));
        enchants.add(new EnchantmentData(Enchantments.INFINITY, 1));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Лук Крушителя"),
                List.of(
                        Text.literal("Снайпер II").formatted(Formatting.GRAY),
                        Text.literal("Подрывник").formatted(Formatting.GRAY),
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getTrident() {
        ItemStack stack = new ItemStack(Items.TRIDENT);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.CHANNELING, 1));
        enchants.add(new EnchantmentData(Enchantments.SHARPNESS, 7));
        enchants.add(new EnchantmentData(Enchantments.FIRE_ASPECT, 2));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.LOYALTY, 3));
        enchants.add(new EnchantmentData(Enchantments.IMPALING, 5));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Трезубец Крушителя"),
                List.of(
                        Text.literal("Скаут III").formatted(Formatting.GRAY),
                        Text.literal("Опытный III").formatted(Formatting.GRAY),
                        Text.literal("Вампиризм II").formatted(Formatting.GRAY),
                        Text.literal("Ступор III").formatted(Formatting.GRAY),
                        Text.literal("Притяжение II").formatted(Formatting.GRAY),
                        Text.literal("Окисление II").formatted(Formatting.GRAY),
                        Text.literal("Возвращение").formatted(Formatting.GRAY),
                        Text.literal("Подрывник").formatted(Formatting.GRAY),
                        Text.literal("Яд III").formatted(Formatting.GRAY),
                        Text.literal("Детекция III").formatted(Formatting.GRAY),
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getMace() {
        ItemStack stack = new ItemStack(Items.MACE);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.SHARPNESS, 7));
        enchants.add(new EnchantmentData(Enchantments.SMITE, 7));
        enchants.add(new EnchantmentData(Enchantments.BANE_OF_ARTHROPODS, 7));
        enchants.add(new EnchantmentData(Enchantments.DENSITY, 5));
        enchants.add(new EnchantmentData(Enchantments.BREACH, 3));
        enchants.add(new EnchantmentData(Enchantments.SWEEPING_EDGE, 3));
        enchants.add(new EnchantmentData(Enchantments.FIRE_ASPECT, 2));
        enchants.add(new EnchantmentData(Enchantments.LOOTING, 5));
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Булава Крушителя"),
                List.of(
                        Text.literal("Опытный III").formatted(Formatting.GRAY),
                        Text.literal("Вампиризм II").formatted(Formatting.GRAY),
                        Text.literal("Окисление II").formatted(Formatting.GRAY),
                        Text.literal("Яд III").formatted(Formatting.GRAY),
                        Text.literal("Детекция III").formatted(Formatting.GRAY),
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    public static ItemStack getElytra() {
        ItemStack stack = new ItemStack(Items.ELYTRA);
        List<EnchantmentData> enchants = new ArrayList<>();
        enchants.add(new EnchantmentData(Enchantments.UNBREAKING, 5));
        enchants.add(new EnchantmentData(Enchantments.MENDING, 1));
        addEnchantments(stack, enchants);
        setupItem(stack, createStyledName("Элитры Крушителя"),
                List.of(
                        Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY)
                ));
        return stack;
    }

    private static void addEnchantments(ItemStack stack, List<EnchantmentData> enchantments) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        try {
            RegistryWrapper.WrapperLookup registryLookup = client.world.getRegistryManager();
            RegistryWrapper<Enchantment> enchantmentRegistry = registryLookup.getOrThrow(RegistryKeys.ENCHANTMENT);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
                    stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));

            for (EnchantmentData data : enchantments) {
                try {
                    Optional<RegistryEntry.Reference<Enchantment>> enchantmentOpt =
                            enchantmentRegistry.getOptional(data.key);

                    if (enchantmentOpt.isPresent()) {
                        builder.add(enchantmentOpt.get(), data.level);
                    }
                } catch (Exception ignored) {
                }
            }

            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupItem(ItemStack stack, Text name, List<Text> lore) {
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        if (!lore.isEmpty()) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        }
    }

    private static Text createStyledName(String baseName) {
        return Text.literal(baseName).formatted(Formatting.BOLD, Formatting.DARK_RED);
    }

    private static class EnchantmentData {
        final RegistryKey<Enchantment> key;
        final int level;

        EnchantmentData(RegistryKey<Enchantment> key, int level) {
            this.key = key;
            this.level = level;
        }
    }
}