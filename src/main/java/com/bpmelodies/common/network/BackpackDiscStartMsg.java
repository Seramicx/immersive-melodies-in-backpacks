package com.bpmelodies.common.network;

import com.bpmelodies.client.playback.ClientDiscTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

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

    public static void handle(BackpackDiscStartMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientDiscTracker.getInstance().start(msg)));
    }
}
