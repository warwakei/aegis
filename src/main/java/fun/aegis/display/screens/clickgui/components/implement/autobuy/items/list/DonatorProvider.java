package fun.aegis.display.screens.clickgui.components.implement.autobuy.items.list;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.customitem.CustomItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.defaultsetpricec.Defaultpricec;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class DonatorProvider {
    public static List<AutoBuyableItem> getDonator() {
        List<AutoBuyableItem> donator = new ArrayList<>();
        List<Text> lightDustLore = List.of(
                Text.literal("Каст: Световая вспышка"),
                Text.literal("Радиус: 10 блоков"),
                Text.literal("Эффекты для противников:"),
                Text.literal(" • Свечение (00:30)"),
                Text.literal(" • Слепота (00:01)"),
                Text.literal("Чем ближе цель, тем дольше длительность эффектов")
        );
        donator.add(new CustomItem("[★] Явная пыль", null, Items.SUGAR, Defaultpricec.getPrice("Явная пыль"), null, lightDustLore));
        List<Text> disorientationLore = List.of(
                Text.literal("Чем ближе цель, тем дольше длительность эффектов")
        );
        donator.add(new CustomItem("[★] Дезориентация", null, Items.ENDER_EYE, Defaultpricec.getPrice("Дезориентация"), null, disorientationLore));
        List<Text> trapkaLore = List.of(
                Text.literal("Каст: Нерушимая клетка"),
                Text.literal("Длительность: 15 секунд"),
                Text.literal("Используйте скины: /tskins")
        );
        donator.add(new CustomItem("[★] Трапка", null, Items.NETHERITE_SCRAP, Defaultpricec.getPrice("Трапка"), null, trapkaLore));
        List<Text> lockpickLore = List.of(
                Text.literal("Этой отмычкой можно"),
                Text.literal("Открыть хранилище"),
                Text.literal("С Сферами")
        );
        donator.add(new CustomItem("Отмычка к Сферам", null, Items.TRIPWIRE_HOOK, Defaultpricec.getPrice("Отмычка к Сферам"), null, lockpickLore));
        List<Text> plast = List.of(
                Text.literal("Каст: Нерушимая стена"),
                Text.literal("Длительность:"),
                Text.literal("Вертикальный: 20 секунд"),
                Text.literal("Горизонтальный: 60 секунд")
        );
        donator.add(new CustomItem("[★] Пласт", null, Items.DRIED_KELP, Defaultpricec.getPrice("Пласт"), null, plast));
        List<Text> exp15Lore = List.of(Text.literal("Содержит 15 ур опыта"));
        donator.add(new CustomItem("Пузырек опыта [15 ур]", null, Items.EXPERIENCE_BOTTLE, Defaultpricec.getPrice("Пузырек опыта [15 ур]"), null, exp15Lore));
        List<Text> exp30Lore = List.of(Text.literal("Содержит: 30 Ур. опыта"));
        donator.add(new CustomItem("Пузырёк опыта [30 Ур.]", null, Items.EXPERIENCE_BOTTLE, Defaultpricec.getPrice("Пузырёк опыта [30 Ур.]"), null, exp30Lore));
        List<Text> exp50Lore = List.of(Text.literal("Содержит 50 ур опыта"));
        donator.add(new CustomItem("Пузырек опыта [50 ур]", null, Items.EXPERIENCE_BOTTLE, Defaultpricec.getPrice("Пузырек опыта [50 ур]"), null, exp50Lore));
        List<Text> tntWhiteLore = List.of(
                Text.literal("Этот динамит взрывается"),
                Text.literal("в 10 раз сильнее обычного")
        );
        donator.add(new CustomItem("[★] TNT - TIER WHITE", null, Items.TNT, Defaultpricec.getPrice("TNT - TIER WHITE"), null, tntWhiteLore));
        List<Text> tntBlackLore = List.of(
                Text.literal("Этот динамит взрывается"),
                Text.literal("в 10 раз сильнее обычного"),
                Text.literal("и способен взорвать обсидиан")
        );
        donator.add(new CustomItem("[★] TNT - TIER BLACK", null, Items.TNT, Defaultpricec.getPrice("TNT - TIER BLACK"), null, tntBlackLore));
        List<Text> signalRandomLore = List.of(
                Text.literal("Уровень лута: Случайный")
        );
        donator.add(new CustomItem("Сигнальный огонь [Случайный]", null, Items.CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Случайный]"), null, signalRandomLore));
        List<Text> signalRichLore = List.of(
                Text.literal("Уровень лута: Богатый")
        );
        donator.add(new CustomItem("Сигнальный огонь [Богатый]", null, Items.CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Богатый]"), null, signalRichLore));
        List<Text> signalLegendaryLore = List.of(
                Text.literal("Уровень лута: Легендарный")
        );
        donator.add(new CustomItem("Сигнальный огонь [Легендарный]", null, Items.CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Легендарный]"), null, signalLegendaryLore));
        List<Text> blockDamagerLore = List.of(
                Text.literal("● Каст: Нанесение урона"),
                Text.literal("● Радиус: 1,5 блока")
        );
        donator.add(new CustomItem("[★] Блок дамагер", null, Items.JIGSAW, Defaultpricec.getPrice("Блок дамагер"), null, blockDamagerLore));
        List<Text> chunkLoader1x1Lore = List.of(
                Text.literal("Прогружает чанк, в котором"),
                Text.literal("находится этот прогрузчик."),
                Text.literal("Нажмите на него, чтобы на"),
                Text.literal("30 секунд увидеть границы")
        );
        donator.add(new CustomItem("Прогрузчик чанков [1x1]", null, Items.STRUCTURE_BLOCK, Defaultpricec.getPrice("Прогрузчик чанков [1x1]"), null, chunkLoader1x1Lore));
        List<Text> chunkLoader3x3Lore = List.of(
                Text.literal("Прогружает чанк, в котором"),
                Text.literal("находится этот прогрузчик."),
                Text.literal("Нажмите на него, чтобы на"),
                Text.literal("30 секунд увидеть границы")
        );
        donator.add(new CustomItem("Прогрузчик чанков [3x3]", null, Items.STRUCTURE_BLOCK, Defaultpricec.getPrice("Прогрузчик чанков [3x3]"), null, chunkLoader3x3Lore));
        List<Text> chunkLoader5x5Lore = List.of(
                Text.literal("Прогружает чанк, в котором"),
                Text.literal("находится этот прогрузчик."),
                Text.literal("Нажмите на него, чтобы на"),
                Text.literal("30 секунд увидеть границы")
        );
        donator.add(new CustomItem("Прогрузчик чанков [5x5]", null, Items.STRUCTURE_BLOCK, Defaultpricec.getPrice("Прогрузчик чанков [5x5]"), null, chunkLoader5x5Lore));
        List<Text> mysteriousBeaconLore = List.of(
                Text.literal("Маяк установит временный"),
                Text.literal("ивент, раздающий Монеты"),
                Text.literal("игрокам поблизости.")
        );
        donator.add(new CustomItem("Загадочный маяк", null, Items.BEACON, Defaultpricec.getPrice("Загадочный маяк"), null, mysteriousBeaconLore));
        List<Text> cursedSoulLore = List.of(
                Text.literal("Обменяй души на ценные"),
                Text.literal("ресурсы у Собирателя душ"),
                Text.literal("/warp soulcollector")
        );
        donator.add(new CustomItem("[★] Проклятая душа", null, Items.SOUL_LANTERN, Defaultpricec.getPrice("Проклятая душа"), null, cursedSoulLore));
        List<Text> dragonSkinLore = List.of(
                Text.literal("Используя этот предмет"),
                Text.literal("Вы его расходуете"),
                Text.literal("и получаете Драконий скин взамен"),
                Text.literal("[ПКМ] чтобы использовать x1 скин"),
                Text.literal("[SHIFT+ПКМ] чтобы использовать все скины"),
                Text.literal("Предмет нужно держать в руке")
        );
        donator.add(new CustomItem("[★] Драконий скин", null, Items.PAPER, Defaultpricec.getPrice("Драконий скин"), null, dragonSkinLore));
        List<Text> fireWhirlwindLore = List.of(
                Text.literal("● Каст: Огненная волна"),
                Text.literal("● Радиус: 10 блоков"),
                Text.literal(""),
                Text.literal("● Эффекты для противников:"),
                Text.literal(" - Поджог (00:03)"),
                Text.literal(""),
                Text.literal("Чем ближе цель, тем дольше"),
                Text.literal("длительность эффектов")
        );
        donator.add(new CustomItem("[★] Огненный смерч", null, Items.FIRE_CHARGE, Defaultpricec.getPrice("Огненный смерч"), null, fireWhirlwindLore));
        List<Text> freezingSnowballLore = List.of(
                Text.literal("● Каст: Ледяная сфера"),
                Text.literal("● Радиус: 7 блоков"),
                Text.literal(""),
                Text.literal("● Эффекты для противников:"),
                Text.literal(" - Заморозка (00:01)"),
                Text.literal(" - Слабость (03:00)")
        );
        donator.add(new CustomItem("[★] Снежок заморозка", null, Items.SNOWBALL, Defaultpricec.getPrice("Снежок заморозка"), null, freezingSnowballLore));
        List<Text> godsAuraLore = List.of(
                Text.literal("● Каст: Божественная аура"),
                Text.literal("● Радиус: 2 блока"),
                Text.literal(""),
                Text.literal("● Эффекты для союзников:"),
                Text.literal(" - Снятие всех эффектов"),
                Text.literal(" - Невидимость (04:00)"),
                Text.literal(" - Сила II (03:00)"),
                Text.literal(" - Скорость II (03:00)")
        );
        donator.add(new CustomItem("[★] Божья аура", null, Items.PHANTOM_MEMBRANE, Defaultpricec.getPrice("Божья аура"), null, godsAuraLore));
        List<Text> silverLore = List.of(
                Text.literal("Это валюта для покупки"),
                Text.literal("отмычек к тайникам"),
                Text.literal("у Знахаря (/warp stash)")
        );
        donator.add(new CustomItem("[★] Серебро", null, Items.IRON_NUGGET, Defaultpricec.getPrice("Серебро"), null, silverLore));
        List<Text> godsTouchLore = List.of(
                Text.literal("Божье касание"),
                Text.literal(""),
                Text.literal("Может добыть спавнер,"),
                Text.literal("но только один раз")
        );
        donator.add(new CustomItem("[★] Божье касание", null, Items.GOLDEN_PICKAXE, Defaultpricec.getPrice("Божье касание"), null, godsTouchLore));
        List<Text> megaBulldozerLore = List.of(
                Text.literal("Вскапывает территорию"),
                Text.literal("размером 9x9x5 блоков")
        );
        donator.add(new CustomItem("[★] Кирка мега-бульдозер", null, Items.NETHERITE_PICKAXE, Defaultpricec.getPrice("Кирка мега-бульдозер"), null, megaBulldozerLore));
        List<Text> caramelAppleLore = List.of(
                Text.literal("Это кошмарная конфета для прохождении"),
                Text.literal("карты таинств - вводи /hellmap"),
                Text.literal(""),
                Text.literal("Кошмарность: +5")
        );
        donator.add(new CustomItem("Карамельное яблоко", null, Items.APPLE, Defaultpricec.getPrice("Карамельное яблоко"), null, caramelAppleLore));
        return donator;
    }
}
