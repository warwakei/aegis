package rich.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rich.util.interfaces.IBuiltChunkAnimator;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class MixinBuiltChunk implements IBuiltChunkAnimator {
    @Unique
    private float animation = 100f;

    @Override
    public float getAnimation() {
        return animation;
    }

    @Override
    public void setAnimation(float value) {
        this.animation = value;
    }
}