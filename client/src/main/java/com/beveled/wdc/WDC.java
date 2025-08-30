package com.beveled.wdc;

import com.beveled.wdc.modules.ClientModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import java.awt.*;

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

    public static boolean isInBounds(Point bpos1, Point bpos2, Point pos) {
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
}
