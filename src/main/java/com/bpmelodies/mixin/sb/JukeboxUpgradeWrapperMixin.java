package com.bpmelodies.mixin.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode;
import net.p3pp3rf1y.sophisticatedcore.util.ComponentItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Mixin(JukeboxUpgradeWrapper.class)
public abstract class JukeboxUpgradeWrapperMixin {

    @Shadow(remap = false) public abstract ItemStack getDisc();
    @Shadow(remap = false) public abstract boolean isShuffleEnabled();
    @Shadow(remap = false) public abstract RepeatMode getRepeatMode();
    @Shadow(remap = false) public abstract boolean isPlaying();
    @Shadow(remap = false) public abstract int getDiscSlotActive();

    private static Field BPM$STORAGE_WRAPPER_FIELD;
    private static Field BPM$PLAYLIST_FIELD;
    private static Field BPM$DISC_INVENTORY_FIELD;
    private static Field BPM$ENTITY_PLAYING_FIELD;
    private static Field BPM$LEVEL_PLAYING_FIELD;
    private static Field BPM$POS_PLAYING_FIELD;
    private static Field BPM$ON_FINISHED_FIELD;
    private static final Random BPM$RANDOM = new Random();

    @Inject(method = "playDisc", at = @At("RETURN"), remap = false)
    private void bpm$startInstrument(CallbackInfo ci) {
        ItemStack disc = getDisc();
        if (!JukeboxAccess.isInstrument(disc)) return;
        ItemStack upgradeStack = ((UpgradeWrapperBase<?, ?>) (Object) this).getUpgradeStack();
        if (!PlaybackNbt.isImEnabled(upgradeStack)) return;
        ResourceLocation melody = PlaybackNbt.getSelectedMelody(disc);
        if (melody == null) return;

        Entity entity = bpm$entityPlaying();
        Level level = entity != null ? entity.level() : bpm$levelPlaying();
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos pos = entity != null ? entity.blockPosition() : bpm$posPlaying();
        int eid = entity != null ? entity.getId() : -1;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(disc.getItem());
        UUID uuid = bpm$getStorageUuid();
        if (uuid == null) return;

        JukeboxUpgradeWrapper self = (JukeboxUpgradeWrapper) (Object) this;
        ServerPlaybackTracker.registerWrapper(uuid, self);
        ServerPlaybackTracker.start(serverLevel, uuid, disc, melody, itemId, pos, eid, bpm$onFinished());
    }

    @Inject(method = "stop", at = @At("HEAD"), remap = false)
    private void bpm$broadcastStopForInstrument(LivingEntity entity, CallbackInfo ci) {
        if (!JukeboxAccess.isInstrument(getDisc())) return;
        bpm$tellTracker();
    }

    // SC keep-alive clears playing state for IM instruments — block while tracker session is active
    @Inject(method = "setIsPlaying", at = @At("HEAD"), cancellable = true, remap = false)
    private void bpm$blockKeepAliveStop(boolean playing, CallbackInfo ci) {
        if (playing) return;
        if (!JukeboxAccess.isInstrument(getDisc())) return;
        UUID uuid = bpm$getStorageUuid();
        if (uuid != null && ServerPlaybackTracker.hasActiveSession(uuid)) ci.cancel();
    }

    @Inject(method = "initPlaylist", at = @At("RETURN"), remap = false)
    private void bpm$filterAfterInitPlaylist(boolean excludeActive, CallbackInfo ci) {
        bpm$filterPlaylistIfImOn();
    }

    @Inject(method = "playNext(Z)V", at = @At("HEAD"), remap = false)
    private void bpm$filterBeforePlayNext(boolean startOverIfAtTheEnd, CallbackInfo ci) {
        bpm$filterPlaylistIfImOn();
    }

    private void bpm$filterPlaylistIfImOn() {
        try {
            ItemStack upgradeStack = ((UpgradeWrapperBase<?, ?>) (Object) this).getUpgradeStack();
            boolean imOn = PlaybackNbt.isImEnabled(upgradeStack);
            LinkedList<Integer> playlist = bpm$getPlaylist();
            ComponentItemHandler inv = bpm$getDiscInventory();
            if (playlist == null || inv == null || playlist.isEmpty()) return;
            LinkedList<Integer> filtered = new LinkedList<>();
            for (Integer slot : playlist) {
                boolean isInst = JukeboxAccess.isInstrument(inv.getStackInSlot(slot));
                if (imOn == isInst) filtered.add(slot);
            }
            if (filtered.size() != playlist.size()) {
                playlist.clear();
                playlist.addAll(filtered);
            }
        } catch (Throwable t) {
            com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] playlist filter failed", t);
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<Integer> bpm$getPlaylist() throws Exception {
        if (BPM$PLAYLIST_FIELD == null) {
            BPM$PLAYLIST_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("playlist");
            BPM$PLAYLIST_FIELD.setAccessible(true);
        }
        return (LinkedList<Integer>) BPM$PLAYLIST_FIELD.get(this);
    }

    private ComponentItemHandler bpm$getDiscInventory() throws Exception {
        if (BPM$DISC_INVENTORY_FIELD == null) {
            BPM$DISC_INVENTORY_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("discInventory");
            BPM$DISC_INVENTORY_FIELD.setAccessible(true);
        }
        return (ComponentItemHandler) BPM$DISC_INVENTORY_FIELD.get(this);
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

    private void bpm$handleSkip(ItemStack disc, int dir) {
        JukeboxUpgradeWrapper self = (JukeboxUpgradeWrapper) (Object) this;
        UUID uuid = bpm$getStorageUuid();
        Player player = bpm$getPlayingPlayer();
        String playerName = player == null ? null : player.getName().getString();
        ResourceLocation cur = PlaybackNbt.getSelectedMelody(disc);
        boolean shuffle = isShuffleEnabled();
        RepeatMode rep = getRepeatMode();

        ResourceLocation pick;
        boolean stopAtEdge = false;

        if (rep == RepeatMode.ONE) {
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

        if (pick != null) {
            PlaybackNbt.setSelectedMelody(disc, pick);
            try {
                int slot = getDiscSlotActive();
                ComponentItemHandler inv = bpm$getDiscInventory();
                if (slot >= 0 && inv != null) inv.setStackInSlot(slot, disc);
            } catch (Throwable t) {
                com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] persist skip melody failed", t);
            }
            try { self.playNext(); }
            catch (Throwable t) { com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] forced playNext failed", t); }
        } else if (stopAtEdge && player != null) {
            try { self.stop(player); }
            catch (Throwable t) { com.bpmelodies.BpMelodiesMod.LOGGER.warn("[wrapper-mixin] stop failed", t); }
        }
    }

    @Nullable
    private UUID bpm$getStorageUuid() {
        try {
            IStorageWrapper sw = bpm$storageWrapper();
            return sw == null ? null : sw.getContentsUuid().orElse(null);
        } catch (Throwable t) { return null; }
    }

    @Nullable
    private Player bpm$getPlayingPlayer() {
        Entity e = bpm$entityPlaying();
        return e instanceof Player p ? p : null;
    }

    private void bpm$tellTracker() {
        try {
            IStorageWrapper sw = bpm$storageWrapper();
            UUID uuid = sw == null ? null : sw.getContentsUuid().orElse(null);
            if (uuid != null) ServerPlaybackTracker.stop(uuid);
        } catch (Exception ignored) { }
    }

    @Nullable
    private IStorageWrapper bpm$storageWrapper() {
        try {
            if (BPM$STORAGE_WRAPPER_FIELD == null) {
                BPM$STORAGE_WRAPPER_FIELD = UpgradeWrapperBase.class.getDeclaredField("storageWrapper");
                BPM$STORAGE_WRAPPER_FIELD.setAccessible(true);
            }
            return (IStorageWrapper) BPM$STORAGE_WRAPPER_FIELD.get(this);
        } catch (Throwable t) { return null; }
    }

    @Nullable
    private Entity bpm$entityPlaying() {
        try {
            if (BPM$ENTITY_PLAYING_FIELD == null) {
                BPM$ENTITY_PLAYING_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("entityPlaying");
                BPM$ENTITY_PLAYING_FIELD.setAccessible(true);
            }
            return (Entity) BPM$ENTITY_PLAYING_FIELD.get(this);
        } catch (Throwable t) { return null; }
    }

    @Nullable
    private Level bpm$levelPlaying() {
        try {
            if (BPM$LEVEL_PLAYING_FIELD == null) {
                BPM$LEVEL_PLAYING_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("levelPlaying");
                BPM$LEVEL_PLAYING_FIELD.setAccessible(true);
            }
            return (Level) BPM$LEVEL_PLAYING_FIELD.get(this);
        } catch (Throwable t) { return null; }
    }

    @Nullable
    private BlockPos bpm$posPlaying() {
        try {
            if (BPM$POS_PLAYING_FIELD == null) {
                BPM$POS_PLAYING_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("posPlaying");
                BPM$POS_PLAYING_FIELD.setAccessible(true);
            }
            return (BlockPos) BPM$POS_PLAYING_FIELD.get(this);
        } catch (Throwable t) { return null; }
    }

    @Nullable
    private Runnable bpm$onFinished() {
        try {
            if (BPM$ON_FINISHED_FIELD == null) {
                BPM$ON_FINISHED_FIELD = JukeboxUpgradeWrapper.class.getDeclaredField("onFinishedCallback");
                BPM$ON_FINISHED_FIELD.setAccessible(true);
            }
            return (Runnable) BPM$ON_FINISHED_FIELD.get(this);
        } catch (Throwable t) { return null; }
    }
}
