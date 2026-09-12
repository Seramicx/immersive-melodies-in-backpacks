package com.bpmelodies.client;

import com.bpmelodies.client.playback.ClientPlaybackTracker;
import com.bpmelodies.client.playback.ClientDiscTracker;
import com.bpmelodies.common.network.BackpackDiscStartMsg;
import com.bpmelodies.common.network.BackpackPlayStartMsg;
import com.bpmelodies.common.network.BackpackPlayStopMsg;
import com.bpmelodies.common.network.MelodyLibrarySyncMsg;
import com.bpmelodies.common.network.RequestMelodyListMsg;
import com.bpmelodies.common.network.SetSelectedMelodyMsg;
import com.bpmelodies.common.network.SetTransportFlagMsg;
import com.bpmelodies.common.network.ToggleImModeMsg;
import com.bpmelodies.common.network.TogglePlaylistMsg;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public final class ClientNetwork {
    private ClientNetwork() {}

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BackpackPlayStartMsg.TYPE, (msg, ctx) ->
                ctx.client().execute(() -> ClientPlaybackTracker.start(msg)));
        ClientPlayNetworking.registerGlobalReceiver(BackpackPlayStopMsg.TYPE, (msg, ctx) ->
                ctx.client().execute(() -> {
                    ClientPlaybackTracker.stop(msg.storageUuid());
                    ClientDiscTracker.getInstance().stop(msg.storageUuid());
                }));
        ClientPlayNetworking.registerGlobalReceiver(BackpackDiscStartMsg.TYPE, (msg, ctx) ->
                ctx.client().execute(() -> ClientDiscTracker.getInstance().start(msg)));
        ClientPlayNetworking.registerGlobalReceiver(MelodyLibrarySyncMsg.TYPE, (msg, ctx) ->
                ctx.client().execute(() -> MelodyLibrarySyncMsg.apply(msg)));
    }

    public static void setSelected(ResourceLocation melody) {
        ClientPlayNetworking.send(new SetSelectedMelodyMsg(melody));
    }

    public static void transport(SetTransportFlagMsg.Op op) {
        ClientPlayNetworking.send(new SetTransportFlagMsg(op, 0));
    }

    public static void requestList() {
        ClientPlayNetworking.send(new RequestMelodyListMsg());
    }

    public static void toggleIm(boolean enabled) {
        ClientPlayNetworking.send(new ToggleImModeMsg(enabled));
    }

    public static void togglePlaylist(ResourceLocation melody, boolean included) {
        ClientPlayNetworking.send(new TogglePlaylistMsg(melody, included));
    }
}
