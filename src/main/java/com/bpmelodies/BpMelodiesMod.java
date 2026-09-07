package com.bpmelodies;

import com.bpmelodies.common.network.ModNetwork;
import com.bpmelodies.common.network.RequestMelodyListMsg;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BpMelodiesMod implements ModInitializer {
    public static final String MODID = "bpmelodies";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static boolean SB_PRESENT = false;
    public static boolean TB_PRESENT = false;

    @Override
    public void onInitialize() {
        SB_PRESENT = FabricLoader.getInstance().isModLoaded("sophisticatedcore");
        TB_PRESENT = FabricLoader.getInstance().isModLoaded("travelersbackpack");

        ModNetwork.registerServer();

        ServerTickEvents.END_WORLD_TICK.register(ServerPlaybackTracker::onLevelTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ServerPlaybackTracker.onServerStopping());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RequestMelodyListMsg.sendLibraryTo(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerPlaybackTracker.stopSessionsFor(handler.player.getId()));

        if (!SB_PRESENT && !TB_PRESENT) {
            LOGGER.info("Neither Sophisticated Backpacks nor Traveler's Backpack found — bpmelodies will be inactive.");
        }
    }
}
