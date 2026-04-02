package fun.aegis.utils.client.managers.event.impl;

import net.minecraft.entity.Entity;


public class EventAttack extends EventLayer {
    private Entity entity;
    boolean pre;

    public EventAttack(Entity entity, boolean pre){
        this.entity = entity;
        this.pre = pre;
    }

    public Entity getEntity(){
        return  entity;
    }

    public boolean isPre(){
        return pre;
    }
}
