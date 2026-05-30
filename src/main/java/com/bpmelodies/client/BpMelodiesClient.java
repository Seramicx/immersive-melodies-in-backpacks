package com.bpmelodies.client;

import com.bpmelodies.client.playback.ClientPlaybackTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class BpMelodiesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetwork.registerClient();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SbInstrumentSlotWatcher.tick();
            ClientPlaybackTracker.tick();
        });
    }
}
