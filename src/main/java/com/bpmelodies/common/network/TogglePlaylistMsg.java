package com.bpmelodies.common.network;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record TogglePlaylistMsg(ResourceLocation melody, boolean included) {
    public static void encode(TogglePlaylistMsg m, FriendlyByteBuf buf) {
        buf.writeResourceLocation(m.melody);
        buf.writeBoolean(m.included);
    }

    public static TogglePlaylistMsg decode(FriendlyByteBuf buf) {
        return new TogglePlaylistMsg(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(TogglePlaylistMsg msg, Player player) {
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            PlaybackNbt.setInPlaylist(access.upgradeStack(), msg.melody, msg.included);
            access.markUpgradeDirty();
        });
    }
}
