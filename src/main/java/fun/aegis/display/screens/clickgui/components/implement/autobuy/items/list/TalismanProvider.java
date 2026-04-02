package fun.aegis.display.screens.clickgui.components.implement.autobuy.items.list;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.customitem.CustomItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.defaultsetpricec.Defaultpricec;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class TalismanProvider {
    public static List<AutoBuyableItem> getTalismans() {
        List<AutoBuyableItem> talismans = new ArrayList<>();
        List<Text> graniLore = List.of(
                Text.literal("Талисман Грани - это"),
                Text.literal("безграничность силы"),
                Text.literal("и духа свободы")
        );
        talismans.add(new CustomItem("[★] Талисман Грани", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Грани"), null, graniLore));
        List<Text> dedalLore = List.of(
                Text.literal("Талисман Дедала - это"),
                Text.literal("сила инженерного духа,"),
                Text.literal("вдохновения и мастерства")
        );
        talismans.add(new CustomItem("[★] Талисман Дедала", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Дедала"), null, dedalLore));
        List<Text> tritonLore = List.of(
                Text.literal("Талисман Тритона - это"),
                Text.literal("неискончаемая мощь"),
                Text.literal("над природными стихиями")
        );
        talismans.add(new CustomItem("[★] Талисман Тритона", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Тритона"), null, tritonLore));
        List<Text> harmonyLore = List.of(
                Text.literal("Талисман Гармонии - это"),
                Text.literal("сбалансированные силы"),
                Text.literal("защиты, мощи и здоровья")
        );
        talismans.add(new CustomItem("[★] Талисман Гармонии", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Гармонии"), null, harmonyLore));
        List<Text> phoenixLore = List.of(
                Text.literal("Талисман Феникса - это"),
                Text.literal("стихии силы и возрождения,"),
                Text.literal("кованые ангельским пламенем")
        );
        talismans.add(new CustomItem("[★] Талисман Феникса", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Феникса"), null, phoenixLore));
        List<Text> echidnaLore = List.of(
                Text.literal("Талисман Ехидны - это"),
                Text.literal("смертоносные змеиные рывки,"),
                Text.literal("ослабленные ядовитой аурой")
        );
        talismans.add(new CustomItem("[★] Талисман Ехидны", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Ехидны"), null, echidnaLore));
        List<Text> punisherLore = List.of(
                Text.literal("Талисман Карателя - это"),
                Text.literal("мощь небесного палача,"),
                Text.literal("сокрушающего недругов")
        );
        talismans.add(new CustomItem("[★] Талисман Карателя", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Карателя"), null, punisherLore));
        List<Text> krushitelLore = List.of(
                Text.literal("Талисман Крушителя - это"),
                Text.literal("легендарный талисман"),
                Text.literal("несокрушимых крушителей")
        );
        talismans.add(new CustomItem("[★] Талисман Крушителя", null, Items.TOTEM_OF_UNDYING, Defaultpricec.getPrice("Талисман Крушителя"), null, krushitelLore));
        return talismans;
    }
}
