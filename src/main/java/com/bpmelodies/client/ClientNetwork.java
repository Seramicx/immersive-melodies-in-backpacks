package com.bpmelodies.client;

import com.bpmelodies.client.playback.ClientDiscTracker;
import com.bpmelodies.client.playback.ClientPlaybackTracker;
import com.bpmelodies.common.network.BackpackDiscStartMsg;
import com.bpmelodies.common.network.BackpackPlayStartMsg;
import com.bpmelodies.common.network.BackpackPlayStopMsg;
import com.bpmelodies.common.network.MelodyLibrarySyncMsg;
import com.bpmelodies.common.network.ModNetwork;
import com.bpmelodies.common.network.SetSelectedMelodyMsg;
import com.bpmelodies.common.network.SetTransportFlagMsg;
import com.bpmelodies.common.network.ToggleImModeMsg;
import com.bpmelodies.common.network.TogglePlaylistMsg;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ClientNetwork {
    private ClientNetwork() {}

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.PLAY_START, (client, handler, buf, sender) -> {
            BackpackPlayStartMsg msg = BackpackPlayStartMsg.decode(buf);
            client.execute(() -> ClientPlaybackTracker.start(msg));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.PLAY_STOP, (client, handler, buf, sender) -> {
            BackpackPlayStopMsg msg = BackpackPlayStopMsg.decode(buf);
            client.execute(() -> {
                ClientPlaybackTracker.stop(msg.storageUuid());
                ClientDiscTracker.getInstance().stop(msg.storageUuid());
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.DISC_START, (client, handler, buf, sender) -> {
            BackpackDiscStartMsg msg = BackpackDiscStartMsg.decode(buf);
            client.execute(() -> ClientDiscTracker.getInstance().start(msg));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.LIBRARY_SYNC, (client, handler, buf, sender) -> {
            MelodyLibrarySyncMsg msg = MelodyLibrarySyncMsg.decode(buf);
            client.execute(() -> MelodyLibrarySyncMsg.apply(msg));
        });
    }

    public static void setSelected(ResourceLocation melody) {
        FriendlyByteBuf buf = ModNetwork.buf();
        SetSelectedMelodyMsg.encode(new SetSelectedMelodyMsg(melody), buf);
        ClientPlayNetworking.send(ModNetwork.SET_SELECTED, buf);
    }

    public static void transport(SetTransportFlagMsg.Op op) {
        FriendlyByteBuf buf = ModNetwork.buf();
        SetTransportFlagMsg.encode(new SetTransportFlagMsg(op, 0), buf);
        ClientPlayNetworking.send(ModNetwork.SET_TRANSPORT, buf);
    }

    public static void requestList() {
        ClientPlayNetworking.send(ModNetwork.REQUEST_LIST, ModNetwork.buf());
    }

    public static void toggleIm(boolean enabled) {
        FriendlyByteBuf buf = ModNetwork.buf();
        ToggleImModeMsg.encode(new ToggleImModeMsg(enabled), buf);
        ClientPlayNetworking.send(ModNetwork.TOGGLE_IM, buf);
    }

    public static void togglePlaylist(ResourceLocation melody, boolean included) {
        FriendlyByteBuf buf = ModNetwork.buf();
        TogglePlaylistMsg.encode(new TogglePlaylistMsg(melody, included), buf);
        ClientPlayNetworking.send(ModNetwork.TOGGLE_PLAYLIST, buf);
    }
}
