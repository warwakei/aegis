package fun.aegis.features.impl.render;

import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;

public class ItemPhysic extends Module {
   public final SelectSetting mode = (new SelectSetting("Физика", "")).value("Обычная").selected("Обычная");

   public static ItemPhysic getInstance() {
      return (ItemPhysic)Instance.get(ItemPhysic.class);
   }

   public ItemPhysic() {
      super("ItemPhysic", "Item Physic", ModuleCategory.RENDER);
   }

   @EventHandler
   public void onTick(TickEvent e) {
   }
}
