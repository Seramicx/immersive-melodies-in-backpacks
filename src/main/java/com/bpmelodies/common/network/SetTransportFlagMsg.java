package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SetTransportFlagMsg(Op op, int value) implements CustomPacketPayload {
    public enum Op { PLAY, STOP, NEXT, PREV, TOGGLE_SHUFFLE, CYCLE_REPEAT }

    public static final Type<SetTransportFlagMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "transport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetTransportFlagMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> { buf.writeEnum(msg.op); buf.writeVarInt(msg.value); },
                    buf -> new SetTransportFlagMsg(buf.readEnum(Op.class), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetTransportFlagMsg msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer)) return;
        Player player = ctx.player();
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            switch (msg.op) {
                case PLAY -> ServerPlaybackTracker.startFromAccess(player, access);
                case STOP -> ServerPlaybackTracker.stop(access.storageUuid());
                case NEXT -> handleSkip(player, access, +1);
                case PREV -> handleSkip(player, access, -1);
                case TOGGLE_SHUFFLE -> {
                    boolean cur = PlaybackNbt.getShuffle(access.upgradeStack());
                    PlaybackNbt.setShuffle(access.upgradeStack(), !cur);
                    access.markUpgradeDirty();
                }
                case CYCLE_REPEAT -> {
                    PlaybackNbt.RepeatMode next = PlaybackNbt.getRepeat(access.upgradeStack()).next();
                    PlaybackNbt.setRepeat(access.upgradeStack(), next);
                    access.markUpgradeDirty();
                }
            }
        });
    }

    private static void handleSkip(Player player, JukeboxAccess access, int dir) {
        int slot = access.findInstrumentSlot();
        if (slot < 0) return;
        ItemStack instrument = access.discInventory().getStackInSlot(slot);
        if (instrument.isEmpty()) return;
        ItemStack upgrade = access.upgradeStack();
        boolean shuffle = PlaybackNbt.getShuffle(upgrade);
        PlaybackNbt.RepeatMode rep = PlaybackNbt.getRepeat(upgrade);
        ResourceLocation cur = PlaybackNbt.getSelectedMelody(instrument);
        java.util.UUID uuid = access.storageUuid();
        String playerName = player.getName().getString();

        ResourceLocation pick;
        boolean stopAtEdge = false;
        if (rep == PlaybackNbt.RepeatMode.ONE) {
            pick = cur;
        } else if (shuffle) {
            if (dir > 0) {
                List<ResourceLocation> all = ServerPlaybackTracker.sortedLibraryIds(playerName);
                pick = all.isEmpty() ? null : all.get(new java.util.Random().nextInt(all.size()));
                if (pick != null && cur != null) ServerPlaybackTracker.pushShuffleHistory(uuid, cur);
            } else {
                pick = ServerPlaybackTracker.popShuffleHistory(uuid);
            }
        } else {
            List<ResourceLocation> all = ServerPlaybackTracker.sortedLibraryIds(playerName);
            if (all.isEmpty()) { pick = null; }
            else {
                int idx = cur == null ? -1 : all.indexOf(cur);
                int target = (idx < 0 ? 0 : idx) + dir;
                if (target < 0 || target >= all.size()) {
                    if (rep == PlaybackNbt.RepeatMode.ALL) pick = all.get(Math.floorMod(target, all.size()));
                    else { pick = null; stopAtEdge = true; }
                } else pick = all.get(target);
            }
        }

        if (pick != null) {
            PlaybackNbt.setSelectedMelody(instrument, pick);
            access.markDiscSlotDirty(slot);
            ServerPlaybackTracker.startFromAccess(player, access);
        } else if (stopAtEdge) {
            ServerPlaybackTracker.stop(uuid);
        }
    }
}
