package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.modules.module.setting.implement.TextSetting;
import rich.util.timer.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotbarSeller extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    TextSetting priceSetting = new TextSetting("Цена", "Цена для продажи (например: 3200000)")
            .setText("3200000");

    SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между командами (мс)")
            .setValue(100).range(20, 400);

    BooleanSetting autoSorterSetting = new BooleanSetting("Авто-сортер", "Автоматически включать HotbarSorter после продажи")
            .setValue(true);

    int currentSlot = 0;

    public HotbarSeller() {
        super("HotbarSeller", "Jenro Hotbar Seller - автоматическая продажа предметов с хотбара", ModuleCategory.MISC);
        settings(priceSetting, delaySetting, autoSorterSetting);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        super.activate();
        currentSlot = 0;
        stopWatch.reset();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
        // слот взад возвращаем
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.player.getInventory().setSelectedSlot(0);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(0));
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            setState(false);
            return;
        }

        // чек все слоты проданы
        if (currentSlot > 8) {
            // все слоты продали
            finishSelling();
            return;
        }

        if (!stopWatch.every((long) delaySetting.getValue())) {
            return;
        }

        // ставим текущий слот
        mc.player.getInventory().setSelectedSlot(currentSlot);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));

        // шлем команду sell
        String price = priceSetting.getText();
        mc.player.networkHandler.sendChatCommand("ah sell " + price);

        // дальше слот
        currentSlot++;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void finishSelling() {
        // если вкл врубаем сортер
        if (autoSorterSetting.isValue()) {
            HotbarSorter hotbarSorter = getHotbarSorter();
            if (hotbarSorter != null && !hotbarSorter.isState()) {
                hotbarSorter.setState(true);
            }
        }

        // гасим селлер
        setState(false);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private HotbarSorter getHotbarSorter() {
        // ищем сортер модуль
        var initialization = Initialization.getInstance();
        if (initialization == null) return null;

        var moduleProvider = initialization.getManager().getModuleProvider();
        if (moduleProvider == null) return null;

        return moduleProvider.get(HotbarSorter.class);
    }
}
