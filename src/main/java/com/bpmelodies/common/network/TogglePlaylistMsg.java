package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record TogglePlaylistMsg(ResourceLocation melody, boolean included) implements CustomPacketPayload {
    public static final Type<TogglePlaylistMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "toggle_playlist"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TogglePlaylistMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, m) -> {
                        buf.writeResourceLocation(m.melody);
                        buf.writeBoolean(m.included);
                    },
                    buf -> new TogglePlaylistMsg(buf.readResourceLocation(), buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TogglePlaylistMsg msg, Player player) {
        if (player == null) return;
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            PlaybackNbt.setInPlaylist(access.upgradeStack(), msg.melody, msg.included);
            access.markUpgradeDirty();
        });
    }
}
