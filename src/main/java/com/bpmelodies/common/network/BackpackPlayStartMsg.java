package com.bpmelodies.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

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
}
