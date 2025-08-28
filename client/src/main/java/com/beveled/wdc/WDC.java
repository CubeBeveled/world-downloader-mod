package com.beveled.wdc;

import com.beveled.wdc.modules.ClientModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class WDC extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("World Downloader");

    @Override
    public void onInitialize() {
        LOG.info("Initializing World Downloader Client");

        Modules.get().add(new ClientModule());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.beveled.wdc";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("CubeBeveled", "world-downloader-mod");
    }
}
