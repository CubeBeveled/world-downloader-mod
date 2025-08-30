package com.beveled.wdc.modules;

import com.beveled.wdc.WDC;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import org.joml.Vector3d;

public class ClientModule extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgAutomation = this.settings.getDefaultGroup();
    private final SettingGroup sgBoundaries = this.settings.getDefaultGroup();

    private final Setting<String> centralUrl = sgGeneral.add(new StringSetting.Builder()
        .name("central-url")
        .description("The URL of the central server.")
        .defaultValue("http://localhost:3000")
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgAutomation.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Should the module be disabled on leave")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useBoundaries = sgBoundaries.add(new BoolSetting.Builder()
        .name("use-boundaries")
        .description("Should there be boundaries. Boundaries are the limits outside of which chunks will not be sent")
        .build()
    );

    private final Setting<Integer> boundPosAX = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-a-x")
        .description("The X coordinate for the first point of the boundary")
        .defaultValue(130000)
        .build()
    );

    private final Setting<Integer> boundPosAZ = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-a-z")
        .description("The Z coordinate for the first point of the boundary")
        .defaultValue(130000)
        .build()
    );

    private final Setting<Integer> boundPosBX = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-b-x")
        .description("The X coordinate for the second point of the boundary")
        .defaultValue(130000)
        .build()
    );

    private final Setting<Integer> boundPosBZ = sgBoundaries.add(new IntSetting.Builder()
        .name("boundary-corner-b-z")
        .description("The Z coordinate for the second point of the boundary")
        .defaultValue(130000)
        .build()
    );

    public ClientModule() {
        super(WDC.CATEGORY, "downloader-client", "Sends the chunks you visit to a central server");
    }

    @Override
    public void onActivate() {
        WDC.LOG.info(String.format("Sending chunks to %s", Modules.get().get(ClientModule.class).settings.getDefaultGroup().get("central-url")));
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get() && isActive()) toggle();
    }
}
