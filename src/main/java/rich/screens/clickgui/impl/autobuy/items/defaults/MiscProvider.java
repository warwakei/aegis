package rich.screens.clickgui.impl.autobuy.items.defaults;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.price.Defaultpricec;
import rich.screens.clickgui.impl.autobuy.util.KrushItem;

import java.util.ArrayList;
import java.util.List;

public class MiscProvider {
    public static List<AutoBuyableItem> getMisc() {
        List<AutoBuyableItem> misc = new ArrayList<>();
        misc.add(new KrushItem("Золотое яблоко", Items.GOLDEN_APPLE, new ItemStack(Items.GOLDEN_APPLE), Defaultpricec.getPrice("Золотое яблоко")));
        misc.add(new KrushItem("Яблоко", Items.APPLE, new ItemStack(Items.APPLE), Defaultpricec.getPrice("Яблоко")));
        misc.add(new KrushItem("Зачарованное золотое яблоко", Items.ENCHANTED_GOLDEN_APPLE, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), Defaultpricec.getPrice("Зачарованное золотое яблоко")));
        misc.add(new KrushItem("Порох", Items.GUNPOWDER, new ItemStack(Items.GUNPOWDER), Defaultpricec.getPrice("Порох")));
        misc.add(new KrushItem("Бирка", Items.NAME_TAG, new ItemStack(Items.NAME_TAG), Defaultpricec.getPrice("Бирка")));
        misc.add(new KrushItem("Трезубец", Items.TRIDENT, new ItemStack(Items.TRIDENT), Defaultpricec.getPrice("Трезубец")));
        misc.add(new KrushItem("Незеритовый слиток", Items.NETHERITE_INGOT, new ItemStack(Items.NETHERITE_INGOT), Defaultpricec.getPrice("Незеритовый слиток")));
        misc.add(new KrushItem("Незеритовое улучшение", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Defaultpricec.getPrice("Незеритовое улучшение")));
        misc.add(new KrushItem("Алмаз", Items.DIAMOND, new ItemStack(Items.DIAMOND), Defaultpricec.getPrice("Алмаз")));
        misc.add(new KrushItem("Алмазный блок", Items.DIAMOND_BLOCK, new ItemStack(Items.DIAMOND_BLOCK), Defaultpricec.getPrice("Алмазный блок")));
        misc.add(new KrushItem("Золотой слиток", Items.GOLD_INGOT, new ItemStack(Items.GOLD_INGOT), Defaultpricec.getPrice("Золотой слиток")));
        misc.add(new KrushItem("Блок золота", Items.GOLD_BLOCK, new ItemStack(Items.GOLD_BLOCK), Defaultpricec.getPrice("Блок золота")));
        misc.add(new KrushItem("Маяк", Items.BEACON, new ItemStack(Items.BEACON), Defaultpricec.getPrice("Маяк")));
        misc.add(new KrushItem("Пузырёк опыта", Items.EXPERIENCE_BOTTLE, new ItemStack(Items.EXPERIENCE_BOTTLE), Defaultpricec.getPrice("Пузырёк опыта")));
        misc.add(new KrushItem("Звезда Незера", Items.NETHER_STAR, new ItemStack(Items.NETHER_STAR), Defaultpricec.getPrice("Звезда Незера")));
        misc.add(new KrushItem("Шалкеровый ящик", Items.SHULKER_BOX, new ItemStack(Items.SHULKER_BOX), Defaultpricec.getPrice("Шалкеровый ящик")));
        misc.add(new KrushItem("Железный слиток", Items.IRON_INGOT, new ItemStack(Items.IRON_INGOT), Defaultpricec.getPrice("Железный слиток")));
        misc.add(new KrushItem("Железный блок", Items.IRON_BLOCK, new ItemStack(Items.IRON_BLOCK), Defaultpricec.getPrice("Железный блок")));
        misc.add(new KrushItem("Незеритовый блок", Items.NETHERITE_BLOCK, new ItemStack(Items.NETHERITE_BLOCK), Defaultpricec.getPrice("Незеритовый блок")));
        misc.add(new KrushItem("Спавнер", Items.SPAWNER, new ItemStack(Items.SPAWNER), Defaultpricec.getPrice("Спавнер")));
        misc.add(new KrushItem("Элитры", Items.ELYTRA, new ItemStack(Items.ELYTRA), Defaultpricec.getPrice("Элитры")));
        misc.add(new KrushItem("Эндер жемчуг", Items.ENDER_PEARL, new ItemStack(Items.ENDER_PEARL), Defaultpricec.getPrice("Эндер жемчуг")));
        misc.add(new KrushItem("Обсидиан", Items.OBSIDIAN, new ItemStack(Items.OBSIDIAN), Defaultpricec.getPrice("Обсидиан")));
        misc.add(new KrushItem("Тотем бессмертия", Items.TOTEM_OF_UNDYING, new ItemStack(Items.TOTEM_OF_UNDYING), Defaultpricec.getPrice("Тотем бессмертия")));
        misc.add(new KrushItem("Палка ифрита", Items.BLAZE_ROD, new ItemStack(Items.BLAZE_ROD), Defaultpricec.getPrice("Палка ифрита")));
        misc.add(new KrushItem("Динамит", Items.TNT, new ItemStack(Items.TNT), Defaultpricec.getPrice("Динамит")));
        misc.add(new KrushItem("Яйцо зомби-жителя", Items.ZOMBIE_VILLAGER_SPAWN_EGG, new ItemStack(Items.ZOMBIE_VILLAGER_SPAWN_EGG), Defaultpricec.getPrice("Яйцо зомби-жителя")));
        misc.add(new KrushItem("Голова скелета", Items.SKELETON_SKULL, new ItemStack(Items.SKELETON_SKULL), Defaultpricec.getPrice("Голова скелета")));
        misc.add(new KrushItem("Голова зомби", Items.ZOMBIE_HEAD, new ItemStack(Items.ZOMBIE_HEAD), Defaultpricec.getPrice("Голова зомби")));
        misc.add(new KrushItem("Голова крипера", Items.CREEPER_HEAD, new ItemStack(Items.CREEPER_HEAD), Defaultpricec.getPrice("Голова крипера")));
        misc.add(new KrushItem("Голова визер-скелета", Items.WITHER_SKELETON_SKULL, new ItemStack(Items.WITHER_SKELETON_SKULL), Defaultpricec.getPrice("Голова визер-скелета")));
        misc.add(new KrushItem("Голова пиглина", Items.PIGLIN_HEAD, new ItemStack(Items.PIGLIN_HEAD), Defaultpricec.getPrice("Голова пиглина")));
        misc.add(new KrushItem("Голова дракона", Items.DRAGON_HEAD, new ItemStack(Items.DRAGON_HEAD), Defaultpricec.getPrice("Голова дракона")));
        misc.add(new KrushItem("Алмазная руда", Items.DIAMOND_ORE, new ItemStack(Items.DIAMOND_ORE), Defaultpricec.getPrice("Алмазная руда")));
        misc.add(new KrushItem("Изумрудная руда", Items.EMERALD_ORE, new ItemStack(Items.EMERALD_ORE), Defaultpricec.getPrice("Изумрудная руда")));
        misc.add(new KrushItem("Торт", Items.CAKE, new ItemStack(Items.CAKE), Defaultpricec.getPrice("Торт")));
        misc.add(new KrushItem("Фейерверк", Items.FIREWORK_ROCKET, new ItemStack(Items.FIREWORK_ROCKET), Defaultpricec.getPrice("Фейерверк")));
        misc.add(new KrushItem("Яйцо жителя", Items.VILLAGER_SPAWN_EGG, new ItemStack(Items.VILLAGER_SPAWN_EGG), Defaultpricec.getPrice("Яйцо жителя")));
        misc.add(new KrushItem("Яйцо вихря", Items.BREEZE_SPAWN_EGG, new ItemStack(Items.BREEZE_SPAWN_EGG), Defaultpricec.getPrice("Яйцо вихря")));
        misc.add(new KrushItem("Мешок", Items.BUNDLE, new ItemStack(Items.BUNDLE), Defaultpricec.getPrice("Мешок")));
        misc.add(new KrushItem("Булава", Items.MACE, new ItemStack(Items.MACE), Defaultpricec.getPrice("Булава")));
        misc.add(new KrushItem("Зловещий ключ испытаний", Items.OMINOUS_TRIAL_KEY, new ItemStack(Items.OMINOUS_TRIAL_KEY), Defaultpricec.getPrice("Зловещий ключ испытаний")));
        misc.add(new KrushItem("Ключ испытаний", Items.TRIAL_KEY, new ItemStack(Items.TRIAL_KEY), Defaultpricec.getPrice("Ключ испытаний")));
        misc.add(new KrushItem("Заряд ветра", Items.WIND_CHARGE, new ItemStack(Items.WIND_CHARGE), Defaultpricec.getPrice("Заряд ветра")));
        misc.add(new KrushItem("Стержень вихря", Items.BREEZE_ROD, new ItemStack(Items.BREEZE_ROD), Defaultpricec.getPrice("Стержень вихря")));
        return misc;
    }
}