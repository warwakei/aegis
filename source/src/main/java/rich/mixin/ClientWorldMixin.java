package rich.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.IMinecraft;
import rich.events.api.EventManager;
import rich.events.impl.EntitySpawnEvent;
import rich.events.impl.WorldLoadEvent;
import rich.modules.impl.render.Ambience;
import rich.util.string.PlayerInteractionHelper;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements IMinecraft {

    @Shadow
    @Final
    private ClientWorld.Properties clientWorldProperties;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(CallbackInfo info) {
        EventManager.callEvent(new WorldLoadEvent());
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    public void addEntityHook(Entity entity, CallbackInfo ci) {
        if (PlayerInteractionHelper.nullCheck()) return;
        EntitySpawnEvent event = new EntitySpawnEvent(entity);
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void onTickTime(CallbackInfo ci) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isState()) {
            this.clientWorldProperties.setTimeOfDay(ambience.getCustomTime());
            ci.cancel();
        }
    }
}