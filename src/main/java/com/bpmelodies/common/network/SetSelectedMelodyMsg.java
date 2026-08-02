package com.bpmelodies.common.network;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;

public record SetSelectedMelodyMsg(ResourceLocation melody) {
    public static void encode(SetSelectedMelodyMsg msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.melody);
    }

    public static SetSelectedMelodyMsg decode(FriendlyByteBuf buf) {
        return new SetSelectedMelodyMsg(buf.readResourceLocation());
    }

    public static void handle(SetSelectedMelodyMsg msg, Player player) {
        if (player == null) return;
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            int slot = access.findInstrumentSlot();
            if (slot < 0) return;
            ItemStack instrument = access.discInventory().getStackInSlot(slot);
            PlaybackNbt.setSelectedMelody(instrument, msg.melody);
            access.markDiscSlotDirty(slot);
            java.util.UUID uuid = access.storageUuid();
            Object wrapper = ServerPlaybackTracker.getWrapperFor(uuid);
            if (wrapper != null && ((JukeboxUpgradeWrapper) wrapper).isPlaying()) {
                try { ((JukeboxUpgradeWrapper) wrapper).playNext(); } catch (Throwable ignored) {}
            } else if (ServerPlaybackTracker.hasActiveSession(uuid)) {
                ServerPlaybackTracker.startFromAccess(player, access);
            }
        });
    }
}
