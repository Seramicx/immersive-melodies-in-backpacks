package com.bpmelodies.client;

import net.minecraftforge.common.MinecraftForge;

public final class ClientEvents {
    private ClientEvents() {}

    public static void register() {
        com.bpmelodies.BpMelodiesMod.LOGGER.info("[bpmelodies] ClientEvents.register() called — registering watcher and playback tracker");
        MinecraftForge.EVENT_BUS.register(SbInstrumentSlotWatcher.class);
        MinecraftForge.EVENT_BUS.register(com.bpmelodies.client.playback.ClientPlaybackTracker.class);
    }
}
