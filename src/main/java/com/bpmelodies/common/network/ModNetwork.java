package com.bpmelodies.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");

        reg.playToServer(SetSelectedMelodyMsg.TYPE, SetSelectedMelodyMsg.STREAM_CODEC, SetSelectedMelodyMsg::handle);
        reg.playToServer(SetTransportFlagMsg.TYPE, SetTransportFlagMsg.STREAM_CODEC, SetTransportFlagMsg::handle);
        reg.playToServer(RequestMelodyListMsg.TYPE, RequestMelodyListMsg.STREAM_CODEC, RequestMelodyListMsg::handle);
        reg.playToServer(ToggleImModeMsg.TYPE, ToggleImModeMsg.STREAM_CODEC, ToggleImModeMsg::handle);

        reg.playToClient(BackpackPlayStartMsg.TYPE, BackpackPlayStartMsg.STREAM_CODEC, BackpackPlayStartMsg::handle);
        reg.playToClient(BackpackPlayStopMsg.TYPE, BackpackPlayStopMsg.STREAM_CODEC, BackpackPlayStopMsg::handle);
        reg.playToClient(MelodyLibrarySyncMsg.TYPE, MelodyLibrarySyncMsg.STREAM_CODEC, MelodyLibrarySyncMsg::handle);
    }
}
