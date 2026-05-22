package com.bpmelodies.common.network;

import com.bpmelodies.client.playback.ClientPlaybackTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record BackpackPlayStartMsg(
        UUID storageUuid,
        ResourceLocation instrumentItemId,
        ResourceLocation melodyId,
        long startGameTime,
        int sourceEntityId,
        BlockPos sourceBlockPos
) {
    public static void encode(BackpackPlayStartMsg m, FriendlyByteBuf buf) {
        buf.writeUUID(m.storageUuid);
        buf.writeResourceLocation(m.instrumentItemId);
        buf.writeResourceLocation(m.melodyId);
        buf.writeLong(m.startGameTime);
        buf.writeVarInt(m.sourceEntityId);
        buf.writeBoolean(m.sourceBlockPos != null);
        if (m.sourceBlockPos != null) buf.writeBlockPos(m.sourceBlockPos);
    }

    public static BackpackPlayStartMsg decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        ResourceLocation item = buf.readResourceLocation();
        ResourceLocation mel = buf.readResourceLocation();
        long startGT = buf.readLong();
        int eid = buf.readVarInt();
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new BackpackPlayStartMsg(uuid, item, mel, startGT, eid, pos);
    }

    public static void handle(BackpackPlayStartMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPlaybackTracker.start(msg)));
    }
}
