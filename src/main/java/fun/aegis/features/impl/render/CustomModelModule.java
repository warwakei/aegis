package fun.aegis.features.impl.render;

import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;

public class CustomModelModule extends Module {
   public final SelectSetting models = new SelectSetting("Model", "Select custom player model");
   public final BooleanSetting friends = new BooleanSetting("Apply to Friends", "Apply custom models to friends");
   private static CustomModelModule instance;

   public CustomModelModule() {
      super("CustomModel", "Custom Models", ModuleCategory.RENDER);
      this.models.value("White Demon", "Red Demon", "Crazy Rabbit", "Freddy Bear", "Sonic", "Amogus", "Jeff Killer", "CupHead", "Crab", "Chinchilla").selected("White Demon");
      this.friends.setValue(true);
      this.setup(new Setting[]{this.models, this.friends});
      instance = this;
   }

   public static CustomModelModule getInstance() {
      return instance;
   }
}
