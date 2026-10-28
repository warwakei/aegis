package rich.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.AttackEvent;
import rich.events.impl.ClickSlotEvent;
import rich.events.impl.InteractEntityEvent;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "attackEntity", at = @At("HEAD"),cancellable = true)
    public void attackEntityHook(PlayerEntity player, Entity target, CallbackInfo info) {
        InteractEntityEvent event = new InteractEntityEvent(target);
        EventManager.callEvent(event);
        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(target);
        EventManager.callEvent(event);
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
        ClickSlotEvent event = new ClickSlotEvent(syncId,slotId,button,actionType);
        EventManager.callEvent(event);
        if (event.isCancelled()) info.cancel();
    }

}