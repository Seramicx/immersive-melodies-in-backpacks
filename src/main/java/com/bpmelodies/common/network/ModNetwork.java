package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(BpMelodiesMod.MODID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private ModNetwork() {}

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(SetSelectedMelodyMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetSelectedMelodyMsg::encode)
                .decoder(SetSelectedMelodyMsg::decode)
                .consumerMainThread(SetSelectedMelodyMsg::handle)
                .add();

        CHANNEL.messageBuilder(SetTransportFlagMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetTransportFlagMsg::encode)
                .decoder(SetTransportFlagMsg::decode)
                .consumerMainThread(SetTransportFlagMsg::handle)
                .add();

        CHANNEL.messageBuilder(RequestMelodyListMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestMelodyListMsg::encode)
                .decoder(RequestMelodyListMsg::decode)
                .consumerMainThread(RequestMelodyListMsg::handle)
                .add();

        CHANNEL.messageBuilder(MelodyLibrarySyncMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MelodyLibrarySyncMsg::encode)
                .decoder(MelodyLibrarySyncMsg::decode)
                .consumerMainThread(MelodyLibrarySyncMsg::handle)
                .add();

        CHANNEL.messageBuilder(ToggleImModeMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleImModeMsg::encode)
                .decoder(ToggleImModeMsg::decode)
                .consumerMainThread(ToggleImModeMsg::handle)
                .add();

        CHANNEL.messageBuilder(BackpackPlayStartMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BackpackPlayStartMsg::encode)
                .decoder(BackpackPlayStartMsg::decode)
                .consumerMainThread(BackpackPlayStartMsg::handle)
                .add();

        CHANNEL.messageBuilder(BackpackPlayStopMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BackpackPlayStopMsg::encode)
                .decoder(BackpackPlayStopMsg::decode)
                .consumerMainThread(BackpackPlayStopMsg::handle)
                .add();
    }
}
