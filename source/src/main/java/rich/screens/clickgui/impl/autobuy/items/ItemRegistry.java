package rich.screens.clickgui.impl.autobuy.items;

import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.defaults.MiscProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.DonatorProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.PotionProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.SphereProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.TalismanProvider;
import rich.screens.clickgui.impl.autobuy.util.KrushProvider;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

import java.util.ArrayList;
import java.util.List;

public class ItemRegistry {
    private static List<AutoBuyableItem> allItems = null;
    private static List<AutoBuyableItem> krushItems = null;
    private static List<AutoBuyableItem> talismanItems = null;
    private static List<AutoBuyableItem> sphereItems = null;
    private static List<AutoBuyableItem> miscItems = null;
    private static List<AutoBuyableItem> donatorItems = null;
    private static List<AutoBuyableItem> potionItems = null;
    private static boolean initialized = false;

    public static void ensureSettingsLoaded() {
        if (!initialized) {
            getAllItems();
            initialized = true;
        }
    }

    public static List<AutoBuyableItem> getAllItems() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            allItems.addAll(getKrush());
            allItems.addAll(getTalismans());
            allItems.addAll(getSpheres());
            allItems.addAll(getMisc());
            allItems.addAll(getDonator());
            allItems.addAll(getPotions());
            initializeAllItemsDisabled();
            loadSavedSettings();
        }
        return allItems;
    }

    private static void initializeAllItemsDisabled() {
        for (AutoBuyableItem item : allItems) {
            item.setEnabled(false);
        }
    }

    private static void loadSavedSettings() {
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        for (AutoBuyableItem item : allItems) {
            if (config.hasItemConfig(item.getDisplayName())) {
                AutoBuyConfig.ItemConfig itemConfig = config.getItemConfigOrNull(item.getDisplayName());
                if (itemConfig != null) {
                    item.getSettings().setBuyBelow(itemConfig.getBuyBelow());
                    item.getSettings().setMinQuantity(itemConfig.getMinQuantity());
                    item.setEnabled(itemConfig.isEnabled());
                }
            }
        }
    }

    public static void reloadSettings() {
        if (allItems != null) {
            initializeAllItemsDisabled();
            loadSavedSettings();
        }
    }

    public static void saveItemState(AutoBuyableItem item) {
        item.setEnabled(!item.isEnabled());
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        config.setItemEnabled(item.getDisplayName(), item.isEnabled());
        config.setItemBuyBelow(item.getDisplayName(), item.getSettings().getBuyBelow());
        config.setItemMinQuantity(item.getDisplayName(), item.getSettings().getMinQuantity());
        config.save();
    }

    public static void saveItemSettings(AutoBuyableItem item) {
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        config.setItemEnabled(item.getDisplayName(), item.isEnabled());
        config.setItemBuyBelow(item.getDisplayName(), item.getSettings().getBuyBelow());
        config.setItemMinQuantity(item.getDisplayName(), item.getSettings().getMinQuantity());
        config.save();
    }

    public static List<AutoBuyableItem> getKrush() {
        if (krushItems == null) {
            krushItems = KrushProvider.getKrush();
        }
        return krushItems;
    }

    public static List<AutoBuyableItem> getTalismans() {
        if (talismanItems == null) {
            talismanItems = TalismanProvider.getTalismans();
        }
        return talismanItems;
    }

    public static List<AutoBuyableItem> getSpheres() {
        if (sphereItems == null) {
            sphereItems = SphereProvider.getSpheres();
        }
        return sphereItems;
    }

    public static List<AutoBuyableItem> getMisc() {
        if (miscItems == null) {
            miscItems = MiscProvider.getMisc();
        }
        return miscItems;
    }

    public static List<AutoBuyableItem> getDonator() {
        if (donatorItems == null) {
            donatorItems = DonatorProvider.getDonator();
        }
        return donatorItems;
    }

    public static List<AutoBuyableItem> getPotions() {
        if (potionItems == null) {
            potionItems = PotionProvider.getPotions();
        }
        return potionItems;
    }

    public static void clearCache() {
        allItems = null;
        krushItems = null;
        talismanItems = null;
        sphereItems = null;
        miscItems = null;
        donatorItems = null;
        potionItems = null;
        initialized = false;
    }
}