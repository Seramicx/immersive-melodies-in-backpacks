package com.bpmelodies.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BackpackDiscStartMsg(
        UUID storageUuid,
        ResourceLocation soundId,
        int sourceEntityId,
        @Nullable BlockPos sourceBlockPos
) {
    public static void encode(BackpackDiscStartMsg m, FriendlyByteBuf buf) {
        buf.writeUUID(m.storageUuid);
        buf.writeResourceLocation(m.soundId);
        buf.writeVarInt(m.sourceEntityId);
        buf.writeBoolean(m.sourceBlockPos != null);
        if (m.sourceBlockPos != null) buf.writeBlockPos(m.sourceBlockPos);
    }

    public static BackpackDiscStartMsg decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        ResourceLocation sound = buf.readResourceLocation();
        int eid = buf.readVarInt();
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new BackpackDiscStartMsg(uuid, sound, eid, pos);
    }
}
