package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.client.playback.ClientPlaybackTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record BackpackPlayStartMsg(
        UUID storageUuid,
        ResourceLocation instrumentItemId,
        ResourceLocation melodyId,
        long startGameTime,
        int sourceEntityId,
        BlockPos sourceBlockPos
) implements CustomPacketPayload {
    public static final Type<BackpackPlayStartMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "play_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackPlayStartMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, m) -> {
                        buf.writeUUID(m.storageUuid);
                        buf.writeResourceLocation(m.instrumentItemId);
                        buf.writeResourceLocation(m.melodyId);
                        buf.writeLong(m.startGameTime);
                        buf.writeVarInt(m.sourceEntityId);
                        buf.writeBoolean(m.sourceBlockPos != null);
                        if (m.sourceBlockPos != null) buf.writeBlockPos(m.sourceBlockPos);
                    },
                    buf -> {
                        UUID uuid = buf.readUUID();
                        ResourceLocation item = buf.readResourceLocation();
                        ResourceLocation mel = buf.readResourceLocation();
                        long startGT = buf.readLong();
                        int eid = buf.readVarInt();
                        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
                        return new BackpackPlayStartMsg(uuid, item, mel, startGT, eid, pos);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BackpackPlayStartMsg msg, IPayloadContext ctx) {
        ClientPlaybackTracker.start(msg);
    }
}
