package com.beveled.wdc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.WorldChunk;

public class WDCClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();

            if (serverInfo == null || !serverInfo.address.contains("6b6t.org")) StateManager.setSaveChunks(false);
            else if (message.contains(Text.of("Welcome to 6b6t.org, ")) && serverInfo.address.contains("6b6t.org")) StateManager.setSaveChunks(true);

            return true;
        });
    }
}
