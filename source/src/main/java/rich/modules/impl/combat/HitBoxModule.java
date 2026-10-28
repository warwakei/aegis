package rich.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import rich.events.api.EventHandler;
import rich.events.impl.BoundingBoxControlEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HitBoxModule extends ModuleStructure {
    SliderSettings xzExpandSetting = new SliderSettings("Расширение XZ", "Позволяет расширить хитбокс по осям XZ")
            .setValue(0.2F).range(0.0F, 3.0F);

    SliderSettings yExpandSetting = new SliderSettings("Расширение Y", "Позволяет расширить хитбокс по оси Y")
            .setValue(0.0F)
            .range(0.0F, 3.0F);

    public HitBoxModule() {
        super("HitBox", "Hit Box", ModuleCategory.COMBAT);
        settings(xzExpandSetting, yExpandSetting);
    }

    @EventHandler
    public void onBoundingBoxControl(BoundingBoxControlEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            Box box = event.getBox();

            float xzExpand = xzExpandSetting.getValue();
            float yExpand = yExpandSetting.getValue();
            Box changedBox = new Box(box.minX - xzExpand / 2.0f, box.minY - yExpand / 2.0f,
                    box.minZ - xzExpand / 2.0f, box.maxX + xzExpand / 2.0f,
                    box.maxY + yExpand / 2.0f, box.maxZ + xzExpand / 2.0f);

            if (living != mc.player && !FriendUtils.isFriend(living)) {
                event.setBox(changedBox);
            }
        }
    }
}
