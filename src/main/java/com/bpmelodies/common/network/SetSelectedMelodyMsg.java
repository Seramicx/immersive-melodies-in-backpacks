package com.bpmelodies.common.network;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;

import java.util.function.Supplier;

public record SetSelectedMelodyMsg(ResourceLocation melody) {
    public static void encode(SetSelectedMelodyMsg msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.melody);
    }

    public static SetSelectedMelodyMsg decode(FriendlyByteBuf buf) {
        return new SetSelectedMelodyMsg(buf.readResourceLocation());
    }

    public static void handle(SetSelectedMelodyMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        Player player = ctx.get().getSender();
        if (player == null) return;
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            int slot = access.findInstrumentSlot();
            if (slot < 0) return;
            ItemStack instrument = access.discInventory().getStackInSlot(slot);
            PlaybackNbt.setSelectedMelody(instrument, msg.melody);
            access.markDiscSlotDirty(slot);
            java.util.UUID uuid = access.storageUuid();
            Object wrapper = com.bpmelodies.common.playback.ServerPlaybackTracker.getWrapperFor(uuid);
            if (wrapper != null && ((JukeboxUpgradeWrapper) wrapper).isPlaying()) {
                try { ((JukeboxUpgradeWrapper) wrapper).playNext(); } catch (Throwable ignored) {}
            } else if (com.bpmelodies.common.playback.ServerPlaybackTracker.hasActiveSession(uuid)) {
                com.bpmelodies.common.playback.ServerPlaybackTracker.startFromAccess(player, access);
            }
        });
    }
}
