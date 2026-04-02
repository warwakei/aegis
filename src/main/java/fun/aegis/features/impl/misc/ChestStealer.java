package fun.aegis.features.impl.misc;

import antidaunleak.api.annotation.Native;
import fun.aegis.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;



import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChestStealer extends Module {
    StopWatch stopWatch = new StopWatch();

    SelectSetting modeSetting = new SelectSetting("Тип", "Выбирает тип стила")
            .value("FunTime", "WhiteList", "Default").selected("FunTime");
    SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между кликами по слоту")
            .setValue(100).range(0, 1000).visible(() -> modeSetting.isSelected("WhiteList") || modeSetting.isSelected("Default"));
    MultiSelectSetting itemSettings = new MultiSelectSetting("Предметы", "Выберите предметы, которые вор будет подбирать")
            .value("Player Head", "Totem Of Undying", "Elytra", "Netherite Sword", "Netherite Helmet", "Netherite ChestPlate", "Netherite Leggings", "Netherite Boots", "Netherite Ingot", "Netherite Scrap")
            .visible(() -> modeSetting.isSelected("WhiteList"));

    public ChestStealer() {
        super("ChestStealer", "Chest Stealer", ModuleCategory.MISC);
        setup(modeSetting, delaySetting, itemSettings);
    }

    @EventHandler

    public void onTick(TickEvent e) {
        switch (modeSetting.getSelected()) {
            case "FunTime" -> {
                if (mc.currentScreen instanceof GenericContainerScreen sh && sh.getTitle().getString().toLowerCase().contains("мистический") && !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {
                    sh.getScreenHandler().slots.stream().filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && stopWatch.every(150))
                            .forEach(s -> InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true));
                }
            }
            case "WhiteList", "Default" -> {
                if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) sh.slots.forEach(s -> {
                    if (s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && (modeSetting.isSelected("Default") || whiteList(s.getStack().getItem())) && stopWatch.every(delaySetting.getValue())) {
                        InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true);
                    }
                });
            }
        }
    }

    private boolean whiteList(Item item) {
        return itemSettings.getSelected().toString().toLowerCase().contains(item.toString().toLowerCase().replace("_", ""));
    }
}
