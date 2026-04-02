package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import rich.events.api.EventHandler;
import rich.events.impl.PlayerCollisionEvent;
import rich.events.impl.PushEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;

public class NoPush extends ModuleStructure {

    MultiSelectSetting ignoreSetting = new MultiSelectSetting("Игнорировать", "")
            .value("Вода", "Блоки", "Коллизию сущностей", "Рыхлый снег", "Ягоды");

    public NoPush() {
        super("AntiPush", "Anti Push", ModuleCategory.PLAYER);
        settings(ignoreSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPush(PushEvent e) {
        switch (e.getType()) {
            case PushEvent.Type.COLLISION -> e.setCancelled(ignoreSetting.isSelected("Коллизию сущностей"));
            case PushEvent.Type.WATER -> e.setCancelled(ignoreSetting.isSelected("Вода"));
            case PushEvent.Type.BLOCK -> e.setCancelled(ignoreSetting.isSelected("Блоки"));
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPlayerCollision(PlayerCollisionEvent e) {
        Block block = e.getBlock();
        if (block.equals(Blocks.POWDER_SNOW)) e.setCancelled(ignoreSetting.isSelected("Рыхлый снег"));
        else if (block.equals(Blocks.SWEET_BERRY_BUSH)) e.setCancelled(ignoreSetting.isSelected("Ягоды"));
    }
}