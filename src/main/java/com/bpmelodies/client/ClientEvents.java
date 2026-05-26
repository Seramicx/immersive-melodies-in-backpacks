package com.bpmelodies.client;

import net.minecraftforge.common.MinecraftForge;

public final class ClientEvents {
    private ClientEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(SbInstrumentSlotWatcher.class);
        MinecraftForge.EVENT_BUS.register(com.bpmelodies.client.playback.ClientPlaybackTracker.class);
    }
}
