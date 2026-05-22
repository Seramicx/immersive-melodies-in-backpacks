package com.bpmelodies.common.network;

import com.bpmelodies.client.playback.ClientPlaybackTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record BackpackPlayStopMsg(UUID storageUuid) {
    public static void encode(BackpackPlayStopMsg m, FriendlyByteBuf buf) {
        buf.writeUUID(m.storageUuid);
    }

    public static BackpackPlayStopMsg decode(FriendlyByteBuf buf) {
        return new BackpackPlayStopMsg(buf.readUUID());
    }

    public static void handle(BackpackPlayStopMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPlaybackTracker.stop(msg.storageUuid)));
    }
}
