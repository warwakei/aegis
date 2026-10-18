package rich.modules.impl.render.particles;

import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
public class TotemEmitter {

    private final Entity entity;
    private final int maxAge;
    private int age;

    public TotemEmitter(Entity entity, int maxAge) {
        this.entity = entity;
        this.maxAge = maxAge;
        this.age = 0;
    }

    public void tick() {
        age++;
    }

    public boolean isAlive() {
        return age < maxAge && entity != null && !entity.isRemoved();
    }

    public float getProgress() {
        return (float) age / maxAge;
    }
}