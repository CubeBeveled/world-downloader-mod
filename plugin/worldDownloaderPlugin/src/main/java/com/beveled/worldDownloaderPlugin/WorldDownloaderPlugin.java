package com.beveled.worldDownloaderPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class WorldDownloaderPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static WorldDownloaderPlugin getInstance() {
        return getPlugin(WorldDownloaderPlugin.class);
    }
}
