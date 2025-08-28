package com.beveled.wdc.mixin;

import com.beveled.wdc.WDC;
import com.beveled.wdc.modules.ClientModule;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.component.Component;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        String dimension = client.player.getWorld().getDimension().effects().toString();
        String username = client.getSession().getUsername();
        String version = client.getGameVersion();

        if (Thread.currentThread().getName().contains("Netty") && Modules.get().isActive(ClientModule.class)) {
            new Thread(() -> {
                try {
                    if (serverInfo == null) return;

                    PacketByteBuf sectionsDataBuf = packet.getChunkData().getSectionsDataBuf();
                    byte[] chunkData = new byte[sectionsDataBuf.readableBytes()];
                    sectionsDataBuf.readBytes(chunkData);
                    String base64Data = Base64.getEncoder().encodeToString(chunkData);
                    String centralUrl = (String) Modules.get().get(ClientModule.class).settings.getDefaultGroup().get("central-url").get();
                    String finalUrl = String.format(
                        "%s/newchunk/%s/%s/%s/%s/%s/%s",
                        centralUrl.replaceAll("/$", ""),
                        serverInfo.address,
                        username,
                        version,
                        dimension,
                        packet.getChunkX(),
                        packet.getChunkZ()
                    );

                    WDC.LOG.info(finalUrl);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(finalUrl))
                        .header("Content-Type", "application/text")
                        .POST(HttpRequest.BodyPublishers.ofString(base64Data))
                        .build();

                    HttpClient httpClient = HttpClient.newHttpClient();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException | URISyntaxException | InterruptedException e) {
                    ChatUtils.sendMsg(Text.of("Error while sending chunk: " + e));
                    throw new RuntimeException(e);
                }
            })
                .start();
        }
    }
}
