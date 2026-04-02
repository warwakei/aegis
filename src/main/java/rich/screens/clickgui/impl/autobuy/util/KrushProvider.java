package rich.screens.clickgui.impl.autobuy.util;

import net.minecraft.item.Items;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.price.Defaultpricec;

import java.util.ArrayList;
import java.util.List;

public class KrushProvider {

    public static List<AutoBuyableItem> getKrush() {
        List<AutoBuyableItem> krush = new ArrayList<>();
        krush.add(new KrushItem("Шлем Крушителя", Items.NETHERITE_HELMET, KrushItems.getHelmet(), Defaultpricec.getPrice("Шлем крушителя"), false, true));
        krush.add(new KrushItem("Нагрудник Крушителя", Items.NETHERITE_CHESTPLATE, KrushItems.getChestplate(), Defaultpricec.getPrice("Нагрудник крушителя"), false, true));
        krush.add(new KrushItem("Поножи Крушителя", Items.NETHERITE_LEGGINGS, KrushItems.getLeggings(), Defaultpricec.getPrice("Поножи крушителя"), false, true));
        krush.add(new KrushItem("Ботинки Крушителя", Items.NETHERITE_BOOTS, KrushItems.getBoots(), Defaultpricec.getPrice("Ботинки крушителя"), false, true));
        krush.add(new KrushItem("Меч Крушителя", Items.NETHERITE_SWORD, KrushItems.getSword(), Defaultpricec.getPrice("Меч крушителя"), false, true));
        krush.add(new KrushItem("Кирка Крушителя", Items.NETHERITE_PICKAXE, KrushItems.getPickaxe(), Defaultpricec.getPrice("Кирка крушителя"), false, true));
        krush.add(new KrushItem("Арбалет Крушителя", Items.CROSSBOW, KrushItems.getCrossbow(), Defaultpricec.getPrice("Арбалет крушителя"), false, true));
        krush.add(new KrushItem("Лук Крушителя", Items.BOW, KrushItems.getBow(), Defaultpricec.getPrice("Лук крушителя"), false, true));
        krush.add(new KrushItem("Трезубец Крушителя", Items.TRIDENT, KrushItems.getTrident(), Defaultpricec.getPrice("Трезубец крушителя"), false, true));
        krush.add(new KrushItem("Булава Крушителя", Items.MACE, KrushItems.getMace(), Defaultpricec.getPrice("Булава крушителя"), false, true));
        krush.add(new KrushItem("Элитры Крушителя", Items.ELYTRA, KrushItems.getElytra(), Defaultpricec.getPrice("Элитры крушителя"), false, true));
        return krush;
    }
}