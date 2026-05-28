package com.bpmelodies;

import com.bpmelodies.common.network.ModNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BpMelodiesMod.MODID)
public final class BpMelodiesMod {
    public static final String MODID = "bpmelodies";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static boolean SB_PRESENT = false;
    public static boolean TB_PRESENT = false;

    public BpMelodiesMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::commonSetup);
        modBus.addListener(ModNetwork::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.bpmelodies.client.ClientEvents.register();
        }

        NeoForge.EVENT_BUS.register(com.bpmelodies.common.playback.ServerPlaybackTracker.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        SB_PRESENT = ModList.get().isLoaded("sophisticatedcore");
        TB_PRESENT = ModList.get().isLoaded("travelersbackpack");

        if (SB_PRESENT) {
            event.enqueueWork(com.bpmelodies.adapter.sb.SbAdapter::init);
            LOGGER.info("Sophisticated Backpacks adapter enabled.");
        }
        if (TB_PRESENT) {
            event.enqueueWork(com.bpmelodies.adapter.tb.TbAdapter::init);
            LOGGER.info("Traveler's Backpack adapter enabled.");
        }
        if (!SB_PRESENT && !TB_PRESENT) {
            LOGGER.info("Neither Sophisticated Backpacks nor Traveler's Backpack found — bpmelodies will be inactive.");
        }
    }
}
