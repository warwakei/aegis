package rich.screens.clickgui.impl.autobuy.originalitems;

import net.minecraft.item.Items;
import net.minecraft.text.Text;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.customitem.CustomItem;
import rich.screens.clickgui.impl.autobuy.items.customitem.UnbreakableItem;
import rich.screens.clickgui.impl.autobuy.items.price.Defaultpricec;

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
        donator.add(new CustomItem("[★] Явная пыль", null, Items.SUGAR, Defaultpricec.getPrice("Явная пыль"), null, lightDustLore, false, true));

        List<Text> disorientationLore = List.of(
                Text.literal("Чем ближе цель, тем дольше длительность эффектов")
        );
        donator.add(new CustomItem("[★] Дезориентация", null, Items.ENDER_EYE, Defaultpricec.getPrice("Дезориентация"), null, disorientationLore, false, true));

        List<Text> trapkaLore = List.of(
                Text.literal("Каст: Нерушимая клетка"),
                Text.literal("Длительность: 15 секунд"),
                Text.literal("Используйте скины: /tskins")
        );
        donator.add(new CustomItem("[★] Трапка", null, Items.NETHERITE_SCRAP, Defaultpricec.getPrice("Трапка"), null, trapkaLore, false, true));

        List<Text> lockpickSpheresLore = List.of(
                Text.literal("Этой отмычкой можно"),
                Text.literal("Открыть хранилище"),
                Text.literal("С Сферами")
        );
        donator.add(new CustomItem("[★] Отмычка к Сферам", null, Items.TRIPWIRE_HOOK, Defaultpricec.getPrice("Отмычка к Сферам"), null, lockpickSpheresLore, false, true));

        List<Text> plast = List.of(
                Text.literal("Каст: Нерушимая стена"),
                Text.literal("Длительность:"),
                Text.literal("Вертикальный: 20 секунд"),
                Text.literal("Горизонтальный: 60 секунд")
        );
        donator.add(new CustomItem("[★] Пласт", null, Items.DRIED_KELP, Defaultpricec.getPrice("Пласт"), null, plast, false, true));

        List<Text> exp15Lore = List.of(Text.literal("Содержит 15 ур опыта"));
        donator.add(new CustomItem("Пузырек опыта [15 ур]", null, Items.EXPERIENCE_BOTTLE, Defaultpricec.getPrice("Пузырек опыта [15 ур]"), null, exp15Lore, false, true));

        List<Text> exp30Lore = List.of(Text.literal("Содержит: 30 Ур. опыта"));
        donator.add(new CustomItem("Пузырёк опыта [30 Ур.]", null, Items.EXPERIENCE_BOTTLE, Defaultpricec.getPrice("Пузырёк опыта [30 Ур.]"), null, exp30Lore, false, true));

        List<Text> exp50Lore = List.of(Text.literal("Содержит 50 ур опыта"));
        donator.add(new CustomItem("Пузырек опыта [50 ур]", null, Items.EXPERIENCE_BOTTLE, Defaultpricec.getPrice("Пузырек опыта [50 ур]"), null, exp50Lore, false, true));

        List<Text> tntWhiteLore = List.of(
                Text.literal("Этот динамит взрывается"),
                Text.literal("в 10 раз сильнее обычного")
        );
        donator.add(new CustomItem("[★] TNT - TIER WHITE", null, Items.TNT, Defaultpricec.getPrice("TNT - TIER WHITE"), null, tntWhiteLore, false, true));

        List<Text> tntBlackLore = List.of(
                Text.literal("Этот динамит взрывается"),
                Text.literal("в 10 раз сильнее обычного"),
                Text.literal("и способен взорвать обсидиан")
        );
        donator.add(new CustomItem("[★] TNT - TIER BLACK", null, Items.TNT, Defaultpricec.getPrice("TNT - TIER BLACK"), null, tntBlackLore, false, true));

        List<Text> signalRandomLore = List.of(
                Text.literal("Уровень лута: Случайный"),
                Text.literal("При помощи этого предмета"),
                Text.literal("можно призвать оригинальный"),
                Text.literal("Мистический Сундук!")
        );
        donator.add(new CustomItem("Сигнальный огонь [Случайный]", null, Items.CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Случайный]"), null, signalRandomLore));

        List<Text> signalOrdinaryLore = List.of(
                Text.literal("Уровень лута: Обычный"),
                Text.literal("При помощи этого предмета"),
                Text.literal("можно призвать оригинальный"),
                Text.literal("Мистический Сундук!")
        );
        donator.add(new CustomItem("Сигнальный огонь [Обычный]", null, Items.CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Обычный]"), null, signalOrdinaryLore));

        List<Text> signalRichLore = List.of(
                Text.literal("Уровень лута: Богатый"),
                Text.literal("При помощи этого предмета"),
                Text.literal("можно призвать оригинальный"),
                Text.literal("Мистический Сундук!")
        );
        donator.add(new CustomItem("Сигнальный огонь [Богатый]", null, Items.CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Богатый]"), null, signalRichLore));

        List<Text> signalLegendaryLore = List.of(
                Text.literal("Уровень лута: Легендарный"),
                Text.literal("При помощи этого предмета"),
                Text.literal("можно призвать оригинальный"),
                Text.literal("Мистический Сундук!")
        );
        donator.add(new CustomItem("Сигнальный огонь [Легендарный]", null, Items.SOUL_CAMPFIRE, Defaultpricec.getPrice("Сигнальный огонь [Легендарный]"), null, signalLegendaryLore));

        List<Text> blockDamagerLore = List.of(
                Text.literal("● Каст: Нанесение урона"),
                Text.literal("● Радиус: 1,5 блока")
        );
        donator.add(new CustomItem("[★] Блок дамагер", null, Items.JIGSAW, Defaultpricec.getPrice("Блок дамагер"), null, blockDamagerLore));

        List<Text> chunkLoader1x1Lore = List.of(
                Text.literal("Прогружает чанк, в котором"),
                Text.literal("находится этот прогрузчик."),
                Text.literal("Нажмите на него, чтобы на"),
                Text.literal("30 секунд увидеть границы"),
                Text.literal("прогружаемой области (1x1).")
        );
        donator.add(new CustomItem("[★] Прогрузчик чанков [1x1]", null, Items.STRUCTURE_BLOCK, Defaultpricec.getPrice("Прогрузчик чанков [1x1]"), null, chunkLoader1x1Lore));

        List<Text> chunkLoader3x3Lore = List.of(
                Text.literal("Прогружает чанк, в котором"),
                Text.literal("находится этот прогрузчик."),
                Text.literal("Нажмите на него, чтобы на"),
                Text.literal("30 секунд увидеть границы"),
                Text.literal("прогружаемой области (3x3).")
        );
        donator.add(new CustomItem("[★] Прогрузчик чанков [3x3]", null, Items.STRUCTURE_BLOCK, Defaultpricec.getPrice("Прогрузчик чанков [3x3]"), null, chunkLoader3x3Lore));

        List<Text> chunkLoader5x5Lore = List.of(
                Text.literal("Прогружает чанк, в котором"),
                Text.literal("находится этот прогрузчик."),
                Text.literal("Нажмите на него, чтобы на"),
                Text.literal("30 секунд увидеть границы"),
                Text.literal("прогружаемой области (5x5).")
        );
        donator.add(new CustomItem("[★] Прогрузчик чанков [5x5]", null, Items.STRUCTURE_BLOCK, Defaultpricec.getPrice("Прогрузчик чанков [5x5]"), null, chunkLoader5x5Lore));

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
        donator.add(new CustomItem("[★] Проклятая душа", null, Items.SOUL_LANTERN, Defaultpricec.getPrice("Проклятая душа"), null, cursedSoulLore, false, true));

        List<Text> dragonSkinLore = List.of(
                Text.literal("Используя этот предмет"),
                Text.literal("Вы его расходуете"),
                Text.literal("и получаете Драконий скин взамен"),
                Text.literal("[ПКМ], чтобы использовать x1 скин"),
                Text.literal("[SHIFT+ПКМ], чтобы использовать все скины"),
                Text.literal("Предмет нужно держать в руке")
        );
        donator.add(new CustomItem("[★] Драконий скин", null, Items.PAPER, Defaultpricec.getPrice("Драконий скин"), null, dragonSkinLore, false, true));

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
        donator.add(new CustomItem("[★] Огненный смерч", null, Items.FIRE_CHARGE, Defaultpricec.getPrice("Огненный смерч"), null, fireWhirlwindLore, false, true));

        List<Text> freezingSnowballLore = List.of(
                Text.literal("● Каст: Ледяная сфера"),
                Text.literal("● Радиус: 7 блоков"),
                Text.literal(""),
                Text.literal("● Эффекты для противников:"),
                Text.literal(" - Заморозка (00:01)"),
                Text.literal(" - Слабость (03:00)")
        );
        donator.add(new CustomItem("[★] Снежок заморозка", null, Items.SNOWBALL, Defaultpricec.getPrice("Снежок заморозка"), null, freezingSnowballLore, false, true));

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
        donator.add(new CustomItem("[★] Божья аура", null, Items.PHANTOM_MEMBRANE, Defaultpricec.getPrice("Божья аура"), null, godsAuraLore, false, true));

        List<Text> silverLore = List.of(
                Text.literal("Это валюта для покупки"),
                Text.literal("отмычек к тайникам"),
                Text.literal("у Знахаря (/warp stash)")
        );
        donator.add(new CustomItem("[★] Серебро", null, Items.IRON_NUGGET, Defaultpricec.getPrice("Серебро"), null, silverLore, false, true));

        List<Text> godsTouchLore = List.of(
                Text.literal("Божье касание"),
                Text.literal("Может добыть спавнер,"),
                Text.literal("но только один раз")
        );
        donator.add(new CustomItem("[★] Божье касание", null, Items.GOLDEN_PICKAXE, Defaultpricec.getPrice("Божье касание"), null, godsTouchLore));

        List<Text> powerfulHitLore = List.of(
                Text.literal("Мощный удар"),
                Text.literal("Может разрушить бедрок,"),
                Text.literal("но только один раз")
        );
        donator.add(new CustomItem("[★] Мощный удар", null, Items.GOLDEN_PICKAXE, Defaultpricec.getPrice("Мощный удар"), null, powerfulHitLore));

        List<Text> megaBulldozerLore = List.of(
                Text.literal("Вскапывает территорию"),
                Text.literal("размером 9x9x5 блоков")
        );
        donator.add(new CustomItem("[★] Кирка мега-бульдозер", null, Items.NETHERITE_PICKAXE, Defaultpricec.getPrice("Кирка мега-бульдозер"), null, megaBulldozerLore));

        List<Text> unbreakableElytraLore = List.of(
                Text.literal("[⚒] Нерушимый предмет")
        );
        donator.add(new UnbreakableItem("[⚒] Нерушимые элитры", Items.ELYTRA, Defaultpricec.getPrice("Нерушимые элитры"), unbreakableElytraLore));

        return donator;
    }
}