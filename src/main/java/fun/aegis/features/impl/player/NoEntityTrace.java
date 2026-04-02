package fun.aegis.features.impl.player;

import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.utils.client.Instance;

public class NoEntityTrace extends Module {

    private final BooleanSetting noSword = new BooleanSetting("Без меча", "Не работает с мечом в руке").setValue(true);

    public NoEntityTrace() {
        super("NoEntityTrace", "No Entity Trace", ModuleCategory.PLAYER);
        setup(noSword);
    }

    public static NoEntityTrace getInstance() {
        return Instance.get(NoEntityTrace.class);
    }

    public boolean shouldIgnoreEntityTrace() {
        return isState() && !(mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.SwordItem && noSword.isValue());
    }
}
