package com.bpmelodies.mixin.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Mixin(JukeboxUpgradeWrapper.class)
public abstract class JukeboxUpgradeWrapperMixin {

    @Shadow(remap = false) public abstract ItemStack getDisc();
    @Shadow(remap = false) public abstract boolean isShuffleEnabled();
    @Shadow(remap = false) public abstract RepeatMode getRepeatMode();
    @Shadow(remap = false) public abstract boolean isPlaying();

    private static Field BPM$STORAGE_WRAPPER_FIELD;
    private static Method BPM$SET_IS_PLAYING_METHOD;
    private static Method BPM$PLAY_NEXT_METHOD;
    private static final Random BPM$RANDOM = new Random();

    @Inject(method = "stop", at = @At("HEAD"), remap = false)
    private void bpm$broadcastStopForInstrument(LivingEntity entity, CallbackInfo ci) {
        if (!JukeboxAccess.isInstrument(getDisc())) return;
        bpm$tellTracker();
    }

    /** When IM is ON, restrict SB's playlist to instrument slots only — without this,
     *  SB picks the lowest-numbered slot which means a vanilla disc in slot 0 always
     *  plays before an instrument in slot 1+. */
    @Inject(method = "initPlaylist", at = @At("RETURN"), remap = false)
    private void bpm$filterAfterInitPlaylist(boolean excludeActive, CallbackInfo ci) {
        bpm$filterPlaylistIfImOn("initPlaylist");
    }

    /** SB also appends discs to the playlist incrementally via tick() when a slot
     *  is filled — so initPlaylist might never fire. Re-filter on every playNext
     *  to catch that path. */
    @Inject(method = "playNext(Z)V", at = @At("HEAD"), remap = false)
    private void bpm$filterBeforePlayNext(boolean startOverIfAtTheEnd, CallbackInfo ci) {
        bpm$filterPlaylistIfImOn("playNext");
    }

    private void bpm$filterPlaylistIfImOn(String origin) {
        try {
            ItemStack upgradeStack = ((UpgradeWrapperBase<?, ?>) (Object) this).getUpgradeStack();
            if (!PlaybackNbt.isImEnabled(upgradeStack)) return;
            LinkedList<Integer> playlist = bpm$getPlaylist();
            net.minecraftforge.items.ItemStackHandler inv = bpm$getDiscInventory();
            if (playlist == null || inv == null || playlist.isEmpty()) return;
            LinkedList<Integer> filtered = new LinkedList<>();
            for (Integer slot : playlist) {
                if (JukeboxAccess.isInstrument(inv.getStackInSlot(slot))) filtered.add(slot);
            }
            if (filtered.size() != playlist.size()) {
                playlist.clear();
                playlist.addAll(filtered);
                com.bpmelodies.BpMelodiesMod.LOGGER.info("[wrapper-mixin] IM-mode filter via {} → {} instrument slot(s)", origin, filtered.size());
            }
        } catch (Throwable t) {
            com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] playlist filter failed", t);
        }
    }

    private static Field BPM$PLAYLIST_FIELD;
    private static Field BPM$DISC_INVENTORY_FIELD;

    @SuppressWarnings("unchecked")
    private LinkedList<Integer> bpm$getPlaylist() throws Exception {
        if (BPM$PLAYLIST_FIELD == null) {
            BPM$PLAYLIST_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("playlist");
            BPM$PLAYLIST_FIELD.setAccessible(true);
        }
        return (LinkedList<Integer>) BPM$PLAYLIST_FIELD.get(this);
    }

    private net.minecraftforge.items.ItemStackHandler bpm$getDiscInventory() throws Exception {
        if (BPM$DISC_INVENTORY_FIELD == null) {
            BPM$DISC_INVENTORY_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("discInventory");
            BPM$DISC_INVENTORY_FIELD.setAccessible(true);
        }
        return (net.minecraftforge.items.ItemStackHandler) BPM$DISC_INVENTORY_FIELD.get(this);
    }

    /** Capture this wrapper into the tracker so when a session ends we can
     *  read shuffle/repeat and pick the next melody. */
    @Inject(method = "play(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false)
    private void bpm$captureOnPlayEntity(Entity entity, CallbackInfo ci) {
        bpm$registerWrapper();
    }

    @Inject(method = "play(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), remap = false)
    private void bpm$captureOnPlayBlock(Level level, BlockPos pos, CallbackInfo ci) {
        bpm$registerWrapper();
    }

    private void bpm$registerWrapper() {
        try {
            if (BPM$STORAGE_WRAPPER_FIELD == null) {
                BPM$STORAGE_WRAPPER_FIELD = UpgradeWrapperBase.class.getDeclaredField("storageWrapper");
                BPM$STORAGE_WRAPPER_FIELD.setAccessible(true);
            }
            IStorageWrapper sw = (IStorageWrapper) BPM$STORAGE_WRAPPER_FIELD.get(this);
            if (sw != null) {
                JukeboxUpgradeWrapper self = (JukeboxUpgradeWrapper) (Object) this;
                sw.getContentsUuid().ifPresent(uuid -> ServerPlaybackTracker.registerWrapper(uuid, self));
            }
        } catch (Throwable ignored) { }
    }

    @Inject(method = "next", at = @At("HEAD"), remap = false, cancellable = true)
    private void bpm$nextSong(CallbackInfo ci) {
        ItemStack disc = getDisc();
        if (!isPlaying() || !JukeboxAccess.isInstrument(disc)) return;
        bpm$handleSkip(disc, +1);
        ci.cancel();
    }

    @Inject(method = "previous", at = @At("HEAD"), remap = false, cancellable = true)
    private void bpm$prevSong(CallbackInfo ci) {
        ItemStack disc = getDisc();
        if (!isPlaying() || !JukeboxAccess.isInstrument(disc)) return;
        bpm$handleSkip(disc, -1);
        ci.cancel();
    }

    /** Unified next/prev decision honoring shuffle + repeat. dir=+1 next, -1 prev. */
    private void bpm$handleSkip(ItemStack disc, int dir) {
        JukeboxUpgradeWrapper self = (JukeboxUpgradeWrapper) (Object) this;
        java.util.UUID uuid = bpm$getStorageUuid();
        Player player = bpm$getPlayingPlayer();
        String playerName = player == null ? null : player.getName().getString();
        ResourceLocation cur = PlaybackNbt.getSelectedMelody(disc);
        boolean shuffle = isShuffleEnabled();
        RepeatMode rep = getRepeatMode();

        ResourceLocation pick;
        boolean stopAtEdge = false;

        if (rep == RepeatMode.ONE) {
            // Repeat ONE: any skip just restarts current song
            pick = cur;
        } else if (shuffle) {
            if (dir > 0) {
                List<ResourceLocation> all = ServerPlaybackTracker.sortedLibraryIds(playerName);
                pick = all.isEmpty() ? null : all.get(BPM$RANDOM.nextInt(all.size()));
                if (uuid != null && cur != null && pick != null) {
                    ServerPlaybackTracker.pushShuffleHistory(uuid, cur);
                }
            } else {
                pick = uuid == null ? null : ServerPlaybackTracker.popShuffleHistory(uuid);
                // empty history → nothing to go back to; leave current playing
            }
        } else {
            List<ResourceLocation> all = ServerPlaybackTracker.sortedLibraryIds(playerName);
            if (all.isEmpty()) { pick = null; }
            else {
                int idx = cur == null ? -1 : all.indexOf(cur);
                int target = (idx < 0 ? 0 : idx) + dir;
                if (target < 0 || target >= all.size()) {
                    if (rep == RepeatMode.ALL) {
                        pick = all.get(Math.floorMod(target, all.size()));
                    } else {
                        pick = null;
                        stopAtEdge = true;
                    }
                } else {
                    pick = all.get(target);
                }
            }
        }

        com.bpmelodies.BpMelodiesMod.LOGGER.info("[wrapper-mixin] skip dir={} shuffle={} repeat={} cur={} -> pick={} stopAtEdge={}",
                dir, shuffle, rep, cur, pick, stopAtEdge);

        if (pick != null) {
            PlaybackNbt.setSelectedMelody(disc, pick);
            try { self.playNext(); }
            catch (Throwable t) { com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] forced playNext failed", t); }
        } else if (stopAtEdge && player != null) {
            try { self.stop(player); }
            catch (Throwable t) { com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] stop failed", t); }
        }
        // shuffle back with empty history: do nothing (current keeps playing)
    }

    @Nullable
    private java.util.UUID bpm$getStorageUuid() {
        try {
            if (BPM$STORAGE_WRAPPER_FIELD == null) {
                BPM$STORAGE_WRAPPER_FIELD = UpgradeWrapperBase.class.getDeclaredField("storageWrapper");
                BPM$STORAGE_WRAPPER_FIELD.setAccessible(true);
            }
            IStorageWrapper sw = (IStorageWrapper) BPM$STORAGE_WRAPPER_FIELD.get(this);
            return sw == null ? null : sw.getContentsUuid().orElse(null);
        } catch (Throwable t) { return null; }
    }

    @Nullable
    private Player bpm$getPlayingPlayer() {
        try {
            if (BPM$ENTITY_PLAYING_FIELD == null) {
                BPM$ENTITY_PLAYING_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("entityPlaying");
                BPM$ENTITY_PLAYING_FIELD.setAccessible(true);
            }
            Object e = BPM$ENTITY_PLAYING_FIELD.get(this);
            return e instanceof Player p ? p : null;
        } catch (Throwable t) { return null; }
    }

    private static Field BPM$ENTITY_PLAYING_FIELD;

    // onDiscFinished mixin removed — for unknown reasons Mixin couldn't hook
    // that private method (next/previous public hooks worked on the same class).
    // Shuffle/repeat is now applied from ServerPlaybackTracker.applyShuffleRepeat
    // before sbOnFinished is invoked.

    private void bpm$invokeSetIsPlaying(boolean playing) {
        try {
            if (BPM$SET_IS_PLAYING_METHOD == null) {
                BPM$SET_IS_PLAYING_METHOD = JukeboxUpgradeWrapper.class.getDeclaredMethod("setIsPlaying", boolean.class);
                BPM$SET_IS_PLAYING_METHOD.setAccessible(true);
            }
            BPM$SET_IS_PLAYING_METHOD.invoke(this, playing);
        } catch (Throwable ignored) { }
    }

    private void bpm$invokePlayNext() {
        try {
            if (BPM$PLAY_NEXT_METHOD == null) {
                BPM$PLAY_NEXT_METHOD = JukeboxUpgradeWrapper.class.getDeclaredMethod("playNext");
                BPM$PLAY_NEXT_METHOD.setAccessible(true);
            }
            BPM$PLAY_NEXT_METHOD.invoke(this);
        } catch (Throwable ignored) { }
    }

    private void bpm$tellTracker() {
        try {
            if (BPM$STORAGE_WRAPPER_FIELD == null) {
                BPM$STORAGE_WRAPPER_FIELD = UpgradeWrapperBase.class.getDeclaredField("storageWrapper");
                BPM$STORAGE_WRAPPER_FIELD.setAccessible(true);
            }
            IStorageWrapper sw = (IStorageWrapper) BPM$STORAGE_WRAPPER_FIELD.get(this);
            if (sw != null) sw.getContentsUuid().ifPresent(ServerPlaybackTracker::stop);
        } catch (Exception ignored) { }
    }

    private ResourceLocation bpm$advanceLibrary(ResourceLocation current, int delta) {
        List<ResourceLocation> all = ServerPlaybackTracker.sortedLibraryIds();
        if (all.isEmpty()) return null;
        int idx = current == null ? -1 : all.indexOf(current);
        int next = Math.floorMod((idx < 0 ? 0 : idx + delta), all.size());
        return all.get(next);
    }

    private ResourceLocation bpm$pickRandom() {
        List<ResourceLocation> all = ServerPlaybackTracker.sortedLibraryIds();
        if (all.isEmpty()) return null;
        return all.get(BPM$RANDOM.nextInt(all.size()));
    }
}
