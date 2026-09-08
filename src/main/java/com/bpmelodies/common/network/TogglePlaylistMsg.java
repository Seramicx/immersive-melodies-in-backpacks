package com.bpmelodies.common.network;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TogglePlaylistMsg(ResourceLocation melody, boolean included) {
    public static void encode(TogglePlaylistMsg m, FriendlyByteBuf buf) {
        buf.writeResourceLocation(m.melody);
        buf.writeBoolean(m.included);
    }

    public static TogglePlaylistMsg decode(FriendlyByteBuf buf) {
        return new TogglePlaylistMsg(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(TogglePlaylistMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        Player player = ctx.get().getSender();
        if (player == null) return;
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            PlaybackNbt.setInPlaylist(access.upgradeStack(), msg.melody, msg.included);
            access.markUpgradeDirty();
        });
    }
}
