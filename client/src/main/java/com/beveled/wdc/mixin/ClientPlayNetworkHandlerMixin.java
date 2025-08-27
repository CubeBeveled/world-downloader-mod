package com.beveled.wdc.mixin;

import com.beveled.wdc.StateManager;
import com.beveled.wdc.WDC;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (Thread.currentThread().getName().contains("Netty") && StateManager.getSaveChunks()) {
            int x = packet.getChunkX();
            int z = packet.getChunkZ();

        }
    }
}
