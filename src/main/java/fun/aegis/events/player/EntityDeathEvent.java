package fun.aegis.events.player;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import fun.aegis.utils.client.managers.event.events.Event;

@Getter
public class EntityDeathEvent implements Event {
    private final Entity entity;
    private final DamageSource source;

    public EntityDeathEvent(Entity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }
}
