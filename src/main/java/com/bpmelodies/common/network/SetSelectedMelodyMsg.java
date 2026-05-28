package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSelectedMelodyMsg(ResourceLocation melody) implements CustomPacketPayload {
    public static final Type<SetSelectedMelodyMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "set_melody"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSelectedMelodyMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeResourceLocation(msg.melody),
                    buf -> new SetSelectedMelodyMsg(buf.readResourceLocation())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetSelectedMelodyMsg msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            int slot = access.findInstrumentSlot();
            if (slot < 0) return;
            ItemStack instrument = access.discInventory().getStackInSlot(slot);
            PlaybackNbt.setSelectedMelody(instrument, msg.melody);
            access.markDiscSlotDirty(slot);
            java.util.UUID uuid = access.storageUuid();
            var wrapper = com.bpmelodies.common.playback.ServerPlaybackTracker.getWrapperFor(uuid);
            if (wrapper != null && wrapper.isPlaying()) {
                try { wrapper.playNext(); } catch (Throwable ignored) {}
            } else if (com.bpmelodies.common.playback.ServerPlaybackTracker.hasActiveSession(uuid)) {
                com.bpmelodies.common.playback.ServerPlaybackTracker.startFromAccess(player, access);
            }
        });
    }
}
