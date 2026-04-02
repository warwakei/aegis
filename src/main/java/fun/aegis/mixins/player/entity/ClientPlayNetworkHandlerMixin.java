package fun.aegis.mixins.player.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.events.block.BlockUpdateEvent;
import fun.aegis.events.chat.ChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements QuickImports {

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void onChunkDataHook(ChunkDataS2CPacket packet, CallbackInfo ci) {
        scanChunk(mc.world.getChunk(packet.getChunkX(), packet.getChunkZ()), BlockUpdateEvent.Type.LOAD);
    }

    @Inject(method = "onUnloadChunk", at = @At("HEAD"))
    private void onUnloadChunkHook(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        scanChunk(mc.world.getChunk(packet.pos().x, packet.pos().z), BlockUpdateEvent.Type.UNLOAD);
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void sendChatMessage(String string, CallbackInfo ci) {
        ChatEvent event = new ChatEvent(string, true);
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String message = packet.content().getString();
        
        ChatEvent event = new ChatEvent(message, false);
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Unique
    private void scanChunk(WorldChunk worldChunk, BlockUpdateEvent.Type type) {
        if (worldChunk == null) return;
        
        int startX = worldChunk.getPos().getStartX();
        int startZ = worldChunk.getPos().getStartZ();
        
        for (int sectionIndex = 0; sectionIndex <= worldChunk.getHighestNonEmptySection(); sectionIndex++) {
            ChunkSection section = worldChunk.getSection(sectionIndex);
            if (section == null || section.isEmpty()) continue;
            
            for (int sectionY = 0; sectionY < 16; sectionY++) {
                int y = (sectionIndex + (worldChunk.getBottomY() >> 4)) << 4 | sectionY;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState blockState = type.equals(BlockUpdateEvent.Type.LOAD) ? section.getBlockState(x, sectionY, z) : Blocks.AIR.getDefaultState();
                        BlockPos pos = new BlockPos(startX | x, y, startZ | z);
                        EventManager.callEvent(new BlockUpdateEvent(blockState, pos, type));
                    }
                }
            }
        }
    }
}
