package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.client.playback.ClientPlaybackTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record BackpackPlayStopMsg(UUID storageUuid) implements CustomPacketPayload {
    public static final Type<BackpackPlayStopMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "play_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackPlayStopMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, m) -> buf.writeUUID(m.storageUuid),
                    buf -> new BackpackPlayStopMsg(buf.readUUID())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BackpackPlayStopMsg msg, IPayloadContext ctx) {
        ClientPlaybackTracker.stop(msg.storageUuid);
    }
}
