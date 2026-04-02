package fun.aegis.features.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.events.player.BoundingBoxControlEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HitBoxModule extends Module {
    SliderSettings xzExpandSetting = new SliderSettings("Расширение XZ", "Позволяет расширить хитбокс по осям XZ")
            .setValue(0.2F).range(0.0F, 3.0F);

    SliderSettings yExpandSetting = new SliderSettings("Расширение Y", "Позволяет расширить хитбокс по оси Y")
            .setValue(0.0F)
            .range(0.0F, 3.0F);

    public HitBoxModule() {
        super("HitBox", "Hit Box", ModuleCategory.COMBAT);
        setup(xzExpandSetting, yExpandSetting);
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
