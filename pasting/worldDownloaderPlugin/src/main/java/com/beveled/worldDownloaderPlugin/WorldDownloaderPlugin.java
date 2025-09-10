package com.beveled.worldDownloaderPlugin;

import com.beveled.worldDownloaderPlugin.Commands.ChunktpCommand;
import com.beveled.worldDownloaderPlugin.Commands.PasteCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class WorldDownloaderPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        getLogger().info("Created data folder");
        Objects.requireNonNull(getCommand("paste")).setExecutor(new PasteCommand());
        Objects.requireNonNull(getCommand("chunktp")).setExecutor(new ChunktpCommand());
        getLogger().info("Registered commands");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static WorldDownloaderPlugin getInstance() {
        return getPlugin(WorldDownloaderPlugin.class);
    }
}
