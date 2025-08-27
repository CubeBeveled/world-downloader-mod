package com.beveled.wdc.modules;

import com.beveled.wdc.WDC;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

public class ClientModule extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgAutomation = this.settings.getDefaultGroup();

    private final Setting<String> centralUrl = sgGeneral.add(new StringSetting.Builder()
        .name("central-url")
        .description("The URL of the central server.")
        .defaultValue("http://us1.sythoptic.com:25008")
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgAutomation.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Should the module be disabled on leave")
        .defaultValue(true)
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
        if (disableOnLeave.get()) toggle();
    }
}
