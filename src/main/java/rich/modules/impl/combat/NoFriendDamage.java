package rich.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.events.api.EventHandler;
import rich.events.impl.InteractEntityEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFriendDamage extends ModuleStructure {

    public NoFriendDamage() {
        super("NoFriendDamage", "No Friend Damage", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onAttack(InteractEntityEvent e) {
        e.setCancelled(FriendUtils.isFriend(e.getEntity()));
    }
}

