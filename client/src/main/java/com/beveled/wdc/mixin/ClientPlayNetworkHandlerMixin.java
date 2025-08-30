package com.beveled.wdc.mixin;

import com.beveled.wdc.WDC;
import com.beveled.wdc.modules.ClientModule;
import meteordevelopment.meteorclient.settings.SettingGroup;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || serverInfo == null) return;

        if (Thread.currentThread().getName().contains("Netty") && Modules.get().isActive(ClientModule.class)) {
            SettingGroup settingGroup = Modules.get().get(ClientModule.class).settings.getDefaultGroup();
            String centralUrl = (String) settingGroup.get("central-url").get();
            Boolean useBounds = (Boolean) settingGroup.get("use-boundaries").get();

            if (useBounds) {
                Integer boundAX = (Integer) settingGroup.get("boundary-corner-a-x").get();
                Integer boundAZ = (Integer) settingGroup.get("boundary-corner-a-z").get();
                Integer boundBX = (Integer) settingGroup.get("boundary-corner-b-x").get();
                Integer boundBZ = (Integer) settingGroup.get("boundary-corner-b-z").get();

                if (WDC.isInBounds(new Point(boundAX, boundAZ), new Point(boundBX, boundBZ), new Point(packet.getChunkX(), packet.getChunkZ())))
                    return;
            }

            String dimension = client.player.getWorld().getDimension().effects().toString();
            String username = client.getSession().getUsername();
            String version = client.getGameVersion();

            PacketByteBuf sectionsDataBuf = packet.getChunkData().getSectionsDataBuf();
            byte[] chunkData = new byte[sectionsDataBuf.readableBytes()];
            sectionsDataBuf.readBytes(chunkData);
            //String base64Data = Base64.getEncoder().encodeToString(chunkData);

            executor.submit(() -> {
                try {
                    String finalUrl = String.format(
                        "%s/newchunk/%s/%s/%s/%s/%s/%s",
                        centralUrl.replaceAll("/$", ""),
                        URLEncoder.encode(serverInfo.address, StandardCharsets.UTF_8),
                        URLEncoder.encode(username, StandardCharsets.UTF_8),
                        URLEncoder.encode(version, StandardCharsets.UTF_8),
                        URLEncoder.encode(dimension, StandardCharsets.UTF_8),
                        packet.getChunkX(),
                        packet.getChunkZ()
                    );

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(finalUrl))
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(chunkData))
                        .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException | URISyntaxException | InterruptedException e) {
                    WDC.LOG.error("Error while sending chunk: {}", e);
                    e.printStackTrace();
                }
            });
        }
    }
}
