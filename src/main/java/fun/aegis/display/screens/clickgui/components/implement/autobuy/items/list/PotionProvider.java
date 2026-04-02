package fun.aegis.display.screens.clickgui.components.implement.autobuy.items.list;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.customitem.CustomItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.defaultsetpricec.Defaultpricec;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PotionProvider {
    public static List<AutoBuyableItem> getPotions() {
        List<AutoBuyableItem> potions = new ArrayList<>();

        List<Text> mochaFlashLore = List.of(
                Text.literal("Испей сок Флеша,"),
                Text.literal("дабы получить его"),
                Text.literal("силу и скорость")
        );
        potions.add(new CustomItem("[★] Моча Флеша", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Моча Флеша"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x5CF7FF), List.of(), Optional.empty()), mochaFlashLore));

        List<Text> medicLore = List.of(
                Text.literal("Эликсир от Медика"),
                Text.literal("помогает выстоять"),
                Text.literal("даже в смертельном бою")
        );
        potions.add(new CustomItem("[★] Зелье Медика", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Медика"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFF00DE), List.of(), Optional.empty()), medicLore));

        List<Text> agentLore = List.of(
                Text.literal("Ловкость и скрытость"),
                Text.literal("тайных Агентов"),
                Text.literal("таятся в этом напитке")
        );
        potions.add(new CustomItem("[★] Зелье Агента", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Агента"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFFFB00), List.of(), Optional.empty()), agentLore));

        List<Text> winnerLore = List.of(
                Text.literal("Храбрая душа Победителя"),
                Text.literal("и немного магии"),
                Text.literal("образовали это зелье")
        );
        potions.add(new CustomItem("[★] Зелье Победителя", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Победителя"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x00FF00), List.of(), Optional.empty()), winnerLore));

        List<Text> killerLore = List.of(
                Text.literal("Осторожно! Зелье Киллера"),
                Text.literal("вызывает кровожадность"),
                Text.literal("и повышает выносливость!")
        );
        potions.add(new CustomItem("[★] Зелье Киллера", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Киллера"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFF0000), List.of(), Optional.empty()), killerLore));

        List<Text> burpLore = List.of(
                Text.literal("Опасная жидкость в сосуде"),
                Text.literal("содержит Отрыжку василиска"),
                Text.literal("и других мерзких тварей")
        );
        potions.add(new CustomItem("[★] Зелье Отрыжки", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Отрыжки"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFF5D00), List.of(), Optional.empty()), burpLore));

        List<Text> acidLore = List.of(
                Text.literal("Куча авантюристов положили"),
                Text.literal("свои жизни в попытках"),
                Text.literal("собрать эту Кислоту")
        );
        potions.add(new CustomItem("[★] Серная кислота", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Серная кислота"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x00C200), List.of(), Optional.empty()), acidLore));

        List<Text> flashLore = List.of(
                Text.literal("Всего одна бутылочка"),
                Text.literal("Вспышки способна ослепить"),
                Text.literal("целую орду врагов!")
        );
        potions.add(new CustomItem("[★] Вспышка", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Вспышка"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFFFFFF), List.of(), Optional.empty()), flashLore));

        return potions;
    }
}
