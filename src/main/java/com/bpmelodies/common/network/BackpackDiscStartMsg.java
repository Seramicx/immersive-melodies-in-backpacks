package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BackpackDiscStartMsg(
        UUID storageUuid,
        ResourceLocation soundId,
        int sourceEntityId,
        @Nullable BlockPos sourceBlockPos
) implements CustomPacketPayload {
    public static final Type<BackpackDiscStartMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "disc_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackDiscStartMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, m) -> {
                        buf.writeUUID(m.storageUuid);
                        buf.writeResourceLocation(m.soundId);
                        buf.writeVarInt(m.sourceEntityId);
                        buf.writeBoolean(m.sourceBlockPos != null);
                        if (m.sourceBlockPos != null) buf.writeBlockPos(m.sourceBlockPos);
                    },
                    buf -> {
                        UUID uuid = buf.readUUID();
                        ResourceLocation sound = buf.readResourceLocation();
                        int eid = buf.readVarInt();
                        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
                        return new BackpackDiscStartMsg(uuid, sound, eid, pos);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
