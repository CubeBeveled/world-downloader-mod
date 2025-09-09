package com.beveled.worldDownloaderPlugin.Commands;

import com.beveled.worldDownloaderPlugin.WorldDownloaderPlugin;
import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;

public class PasteCommand implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length > 3) {
            String serversPath = WorldDownloaderPlugin.getInstance().getDataPath() + "/" + args[0];
            File servers = new File(serversPath);

            if (servers.isDirectory()) {
                String dimensionsPath = serversPath + "/" + args[1];
                File dimensions = new File(dimensionsPath);

                if (dimensions.isDirectory()) {
                    String chunksPath = dimensionsPath + "/" + args[2];
                    File chunks = new File(chunksPath);

                    if (chunks.isDirectory()) {
                        sender.sendMessage("Pasting saved world");

                        World world;
                        if (sender instanceof Player player) world = player.getWorld();
                        else if (sender instanceof BlockCommandSender blockSender)
                            world = blockSender.getBlock().getWorld();
                        else {
                            sender.sendMessage("You must be a player or command block to use this command");
                            return true;
                        }

                        World finalWorld = world;
                        AtomicInteger delay = new AtomicInteger(0);

                        for (File chunkFile : Objects.requireNonNull(chunks.listFiles())) {
                            Bukkit.getScheduler().runTaskLater(WorldDownloaderPlugin.getInstance(), () -> {
                                try (FileReader reader = new FileReader(chunkFile.getPath())) {
                                    JsonArray chunkBlocks = JsonParser.parseReader(reader).getAsJsonArray();

                                    for (JsonElement block : chunkBlocks) {
                                        JsonObject obj = block.getAsJsonObject();

                                        Location blockLocation = new Location(
                                                finalWorld,
                                                obj.get("x").getAsDouble(),
                                                obj.get("y").getAsDouble(),
                                                obj.get("z").getAsDouble()
                                        );

                                        Block blockInWorld = finalWorld.getBlockAt(blockLocation);

                                        String state = obj.get("state").getAsString();
                                        Material material = Material.matchMaterial(state);
                                        if (material == null) {
                                            WorldDownloaderPlugin.getInstance().getLogger().warning("Block material not found for " + state);
                                        } else blockInWorld.setType(material);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }, delay.getAndAdd(parseInt(args[3])));
                        }

                        Bukkit.getScheduler().runTaskLater(WorldDownloaderPlugin.getInstance(), () -> {
                            String m = String.format("Done pasting %s blocks", delay.get() / parseInt(args[3]));

                            sender.sendMessage(m);
                            WorldDownloaderPlugin.getInstance().getLogger().info(m);
                        }, delay.get());

                        return true;
                    } else {
                        sender.sendMessage("Dimension folder not found");
                        sender.sendMessage(chunksPath);
                        return true;
                    }
                } else {
                    sender.sendMessage("Server folder not found");
                    sender.sendMessage(dimensionsPath);
                    return true;
                }
            } else {
                sender.sendMessage("Save folder not found");
                sender.sendMessage(serversPath);
                return true;
            }
        } else return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}