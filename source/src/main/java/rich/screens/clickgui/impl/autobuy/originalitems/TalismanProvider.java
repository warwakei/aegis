package rich.screens.clickgui.impl.autobuy.originalitems;

import net.minecraft.item.Items;
import net.minecraft.text.Text;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.customitem.CustomItem;
import rich.screens.clickgui.impl.autobuy.items.price.Defaultpricec;

import java.util.ArrayList;
import java.util.List;

public class TalismanProvider {
    public static List<AutoBuyableItem> getTalismans() {
        List<AutoBuyableItem> talismans = new ArrayList<>();
        List<Text> krushitelLore = List.of(
                Text.literal("Легендарный символ."),
                Text.literal("Несокрушимая мощь,"),
                Text.literal("Ломающая преграды.")
        );
        talismans.add(new CustomItem("[★] Талисман Крушителя", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Крушителя"), null, krushitelLore, true));
        List<Text> razdorLore = List.of(
                Text.literal("Раздор жаждет хаоса,"),
                Text.literal("Даруя безумный темп,"),
                Text.literal("Но разрушая броню")
        );
        talismans.add(new CustomItem("[★] Талисман Раздора", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Раздора"), null, razdorLore, true));
        List<Text> tiranLore = List.of(
                Text.literal("Тиран подавляет слабых."),
                Text.literal("Дает защиту и силу,"),
                Text.literal("Взимая кровавый налог.")
        );
        talismans.add(new CustomItem("[★] Талисман Тирана", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Тирана"), null, tiranLore, true));
        List<Text> yarostLore = List.of(
                Text.literal("Чистая, дикая агрессия."),
                Text.literal("Граничит с безумием,"),
                Text.literal("Меняя жизнь на урон.")
        );
        talismans.add(new CustomItem("[★] Талисман Ярости", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Ярости"), null, yarostLore, true));
        List<Text> vihrLore = List.of(
                Text.literal("Вихрь не знает покоя,"),
                Text.literal("Ускоряя владельца"),
                Text.literal("И закаляя его дух.")
        );
        talismans.add(new CustomItem("[★] Талисман Вихря", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Вихря"), null, vihrLore, true));
        List<Text> mrakLore = List.of(
                Text.literal("Мрак сгущается рядом,"),
                Text.literal("Укрывая владельца"),
                Text.literal("И питая его силы.")
        );
        talismans.add(new CustomItem("[★] Талисман Мрака", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Мрака"), null, mrakLore, true));
        List<Text> demonLore = List.of(
                Text.literal("Печать разжигает ярость,"),
                Text.literal("Ускоряя удары сердца"),
                Text.literal("И силу каждой атаки.")
        );
        talismans.add(new CustomItem("[★] Талисман Демона", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Демона"), null, demonLore, true));
        List<Text> karatelLore = List.of(
                Text.literal("Несёт строгий приговор,"),
                Text.literal("Карая всех врагов,"),
                Text.literal("Но ослабляя тело.")
        );
        talismans.add(new CustomItem("[★] Талисман Карателя", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Карателя"), null, karatelLore, true));
        List<Text> grinchLore = List.of(
                Text.literal("Похититель праздника легок,"),
                Text.literal("Его карманы полны удачи,"),
                Text.literal("Но сердце слишком мало")
        );
        talismans.add(new CustomItem("[★] Талисман Гринча", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Гринча"), null, grinchLore, true));
        return talismans;
    }
}