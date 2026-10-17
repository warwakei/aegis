package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.InventoryUtils;
import rich.util.timer.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotbarSorter extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между действиями (мс)")
            .setValue(100).range(20, 500);

    BooleanSetting autoSellerSetting = new BooleanSetting("Авто-селлер", "Автоматически включать HotbarSeller после сортировки")
            .setValue(true);

    int currentHotbarSlot = 0;
    boolean waitingForScreen = false;
    boolean sortingComplete = false;

    public HotbarSorter() {
        super("HotbarSorter", "Jenro Hotbar Sorter - автоматически перемещает предметы из инвентаря в хотбар", ModuleCategory.MISC);
        settings(delaySetting, autoSellerSetting);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        super.activate();
        currentHotbarSlot = 0;
        waitingForScreen = false;
        sortingComplete = false;
        stopWatch.reset();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
        // закрываем инв если открыт
        if (mc.currentScreen instanceof InventoryScreen) {
            mc.setScreen(null);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            setState(false);
            return;
        }

        // все готово закрываем
        if (sortingComplete) {
            finishSorting();
            return;
        }

        // инв закрыт открываем разок
        if (!(mc.currentScreen instanceof InventoryScreen)) {
            if (!waitingForScreen) {
                mc.setScreen(new InventoryScreen(mc.player));
                waitingForScreen = true;
                stopWatch.reset();
            } else if (!stopWatch.every(100)) {
                // ждем открытия инва
                return;
            } else {
                // таймут инв не открылся
                setState(false);
            }
            return;
        }

        // инв открылся
        if (waitingForScreen) {
            waitingForScreen = false;
            stopWatch.reset();
        }

        // конец хотбара хорош
        if (currentHotbarSlot >= 9) {
            sortingComplete = true;
            return;
        }

        // не кликаем быстро
        if (!stopWatch.every((long) delaySetting.getValue())) {
            return;
        }

        // ищем шмот в инве
        int sourceSlot = -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
                sourceSlot = i;
                break;
            }
        }

        // инв пуст хорош
        if (sourceSlot == -1) {
            sortingComplete = true;
            return;
        }

        // кидаем шмот в хотбар
        InventoryUtils.click(sourceSlot, 0, SlotActionType.QUICK_MOVE);

        // дальше слот хотбара
        currentHotbarSlot++;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void finishSorting() {
        // закрываем инв
        if (mc.currentScreen instanceof InventoryScreen) {
            mc.setScreen(null);
        }

        // если вкл врубаем селлер
        if (autoSellerSetting.isValue()) {
            HotbarSeller hotbarSeller = getHotbarSeller();
            if (hotbarSeller != null && !hotbarSeller.isState()) {
                hotbarSeller.setState(true);
            }
        }

        // гасим сортер
        setState(false);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private HotbarSeller getHotbarSeller() {
        // ищем селлер модуль
        var initialization = rich.Initialization.getInstance();
        if (initialization == null) return null;

        var moduleProvider = initialization.getManager().getModuleProvider();
        if (moduleProvider == null) return null;

        return moduleProvider.get(HotbarSeller.class);
    }
}
