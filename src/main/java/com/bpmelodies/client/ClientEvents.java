package com.bpmelodies.client;

import net.neoforged.neoforge.common.NeoForge;

public final class ClientEvents {
    private ClientEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(SbInstrumentSlotWatcher.class);
        NeoForge.EVENT_BUS.register(com.bpmelodies.client.playback.ClientPlaybackTracker.class);
    }
}
