package com.beveled.wdc.modules;

import com.beveled.wdc.WDC;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientModule extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgAutomation = this.settings.getDefaultGroup();
    private final SettingGroup sgBoundaries = this.settings.getDefaultGroup();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final Setting<String> centralUrl = sgGeneral.add(new StringSetting.Builder()
        .name("central-url")
        .description("The URL of the central server.")
        .defaultValue("http://us1.sythoptic.com:25008")
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgAutomation.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Should the module disable itself on leave")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnError = sgAutomation.add(new BoolSetting.Builder()
        .name("disable-on-error")
        .description("Should the module disable itself when theres a server error")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Say whats going on thru chat. BEWARE: this will clog chat")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> useBoundaries = sgBoundaries.add(new BoolSetting.Builder()
        .name("use-boundaries")
        .description("Should there be boundaries. Boundaries are the limits outside of which chunks will not be sent")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> boundPosAX = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-a-x")
        .description("The X coordinate for the first point of the boundary")
        .defaultValue(130000)
        .visible(useBoundaries::get)
        .build()
    );

    private final Setting<Integer> boundPosAZ = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-a-z")
        .description("The Z coordinate for the first point of the boundary")
        .defaultValue(130000)
        .visible(useBoundaries::get)
        .build()
    );

    private final Setting<Integer> boundPosBX = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-b-x")
        .description("The X coordinate for the second point of the boundary")
        .defaultValue(-130000)
        .visible(useBoundaries::get)
        .build()
    );

    private final Setting<Integer> boundPosBZ = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-b-z")
        .description("The Z coordinate for the second point of the boundary")
        .defaultValue(-130000)
        .visible(useBoundaries::get)
        .build()
    );

    public ClientModule() {
        super(WDC.CATEGORY, "downloader-client", "Sends the chunks you visit to a central server");
    }

    private static boolean isInBounds(Point bpos1, Point bpos2, Point pos) {
        int x1 = bpos1.x;
        int z1 = bpos1.y;

        int x2 = bpos2.x;
        int z2 = bpos2.y;

        int px = pos.x;
        int pz = pos.y;

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        return px >= minX && px <= maxX && pz >= minZ && pz <= maxZ;
    }

    @Override
    public void onActivate() {
        WDC.LOG.info(String.format("Sending chunks to %s", Modules.get().get(ClientModule.class).settings.getDefaultGroup().get("central-url")));
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get() && isActive()) toggle();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();
        ChunkPos chunkPos = chunk.getPos();

        if (
            !isInBounds(new Point(boundPosAX.get(), boundPosAZ.get()), new Point(boundPosBX.get(), boundPosBZ.get()), new Point(chunk.getPos().x, chunk.getPos().z)) &&
                useBoundaries.get()
        )
            return;

        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || serverInfo == null) return;

        String dimension = client.player.getWorld().getDimension().effects().toString();
        String username = client.getSession().getUsername();
        String version = client.getGameVersion();

        String finalUrl = String.format(
            "%s/blocks/%s/%s/%s",
            centralUrl.get().replaceAll("/$", ""),
            URLEncoder.encode(dimension, StandardCharsets.UTF_8),
            chunkPos.x,
            chunkPos.z
        );

        if (!executor.isShutdown())
            executor.submit(() -> {
                List<String> blocks = new ArrayList<>();

                ChunkSection[] sections = chunk.getSectionArray();

                for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    ChunkSection section = sections[sectionIndex];
                    if (section.isEmpty()) continue;

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                BlockState state = section.getBlockState(x, y, z);
                                if (state.isAir()) continue;

                                String blockName = Registries.BLOCK.getId(state.getBlock()).toString().replace("minecraft:", "");
                                blocks.add(String.format(
                                    "%s,%s,%s:%s",
                                    (chunkPos.x * 16) + x,
                                    (sectionIndex * 16) + y,
                                    (chunkPos.z * 16) + z,
                                    blockName
                                ));
                            }
                        }
                    }
                }

                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(finalUrl))
                        .header("Content-Type", "text/plain")
                        .header("Server-Address", serverInfo.address)
                        .header("Client-Version", version)
                        .header("Author", username)
                        .POST(HttpRequest.BodyPublishers.ofString(String.join(";", blocks)))
                        .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    blocks.clear();

                    if (debug.get())
                        ChatUtils.sendMsg(Text.of(String.format("Sent data in chunk %s,%s", chunkPos.x, chunkPos.z)));


                    if (response.statusCode() != 200) {
                        if (isActive()) {
                            ChatUtils.sendMsg(Text.of("Chunk server error: " + response.body()));
                            if (disableOnError.get()) toggle();
                        }
                    }
                } catch (IOException | URISyntaxException | InterruptedException e) {
                    if (isActive()) {
                        ChatUtils.sendMsg(Text.of("Error while sending chunk: " + e.getMessage()));
                        toggle();
                    }

                    e.printStackTrace();
                }
            });
    }
}
