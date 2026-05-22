package com.bpmelodies.common.playback;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.network.BackpackPlayStartMsg;
import com.bpmelodies.common.network.BackpackPlayStopMsg;
import com.bpmelodies.common.network.ModNetwork;
import immersive_melodies.resources.ServerMelodyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;

public final class ServerPlaybackTracker {

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, WeakReference<net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper>> WRAPPERS = new HashMap<>();
    private static final Map<UUID, Deque<ResourceLocation>> SHUFFLE_HISTORY = new HashMap<>();
    private static final Map<UUID, JukeboxAccess> TB_ACCESS = new HashMap<>();
    private static final Random PICK_RANDOM = new Random();
    public static void pushShuffleHistory(UUID storageUuid, ResourceLocation melody) {
        if (melody == null) return;
        Deque<ResourceLocation> d = SHUFFLE_HISTORY.computeIfAbsent(storageUuid, k -> new ArrayDeque<>());
        d.push(melody);
        while (d.size() > 50) d.pollLast();
    }

    @Nullable
    public static ResourceLocation popShuffleHistory(UUID storageUuid) {
        Deque<ResourceLocation> d = SHUFFLE_HISTORY.get(storageUuid);
        return d == null || d.isEmpty() ? null : d.pop();
    }

    public static void clearShuffleHistory(UUID storageUuid) {
        SHUFFLE_HISTORY.remove(storageUuid);
    }

    public static boolean hasActiveSession(UUID storageUuid) {
        return SESSIONS.containsKey(storageUuid);
    }

    public static void stopSessionsForTbUpgrade(Object upgrade) {
        List<UUID> toStop = null;
        for (Map.Entry<UUID, JukeboxAccess> e : TB_ACCESS.entrySet()) {
            if (e.getValue() instanceof com.bpmelodies.adapter.tb.TbJukeboxAccess tba && tba.getUpgrade() == upgrade) {
                if (toStop == null) toStop = new ArrayList<>();
                toStop.add(e.getKey());
            }
        }
        if (toStop != null) for (UUID u : toStop) {
            BpMelodiesMod.LOGGER.info("[tracker] TB upgrade ejected → stopping uuid={}", u);
            stop(u);
        }
    }

    public static void stopSessionsForTbHandler(Object handler) {
        List<UUID> toStop = null;
        for (Map.Entry<UUID, JukeboxAccess> e : TB_ACCESS.entrySet()) {
            if (e.getValue().discInventory() == handler) {
                if (toStop == null) toStop = new ArrayList<>();
                toStop.add(e.getKey());
            }
        }
        if (toStop != null) for (UUID u : toStop) {
            BpMelodiesMod.LOGGER.info("[tracker] TB instrument removed → stopping uuid={}", u);
            stop(u);
        }
    }

    private ServerPlaybackTracker() {}

    public static void registerWrapper(UUID storageUuid, net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper wrapper) {
        WRAPPERS.put(storageUuid, new WeakReference<>(wrapper));
    }

    @javax.annotation.Nullable
    public static net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper getWrapperFor(UUID storageUuid) {
        var ref = WRAPPERS.get(storageUuid);
        return ref == null ? null : ref.get();
    }

    public static class Session {
        public final ServerLevel level;
        public final UUID storageUuid;
        public final ItemStack instrumentStack;
        public ResourceLocation melodyId;
        public final ResourceLocation instrumentItemId;
        @Nullable public final BlockPos blockPos;
        public final int entityId;
        public long startGameTime;
        public long endGameTime;
        @Nullable public final Runnable sbOnFinished;

        Session(ServerLevel level, UUID storageUuid, ItemStack instrumentStack,
                ResourceLocation melodyId, ResourceLocation instrumentItemId,
                @Nullable BlockPos blockPos, int entityId,
                long startGameTime, long endGameTime, @Nullable Runnable sbOnFinished) {
            this.level = level;
            this.storageUuid = storageUuid;
            this.instrumentStack = instrumentStack;
            this.melodyId = melodyId;
            this.instrumentItemId = instrumentItemId;
            this.blockPos = blockPos;
            this.entityId = entityId;
            this.startGameTime = startGameTime;
            this.endGameTime = endGameTime;
            this.sbOnFinished = sbOnFinished;
        }
    }

    public static void start(ServerLevel level, UUID storageUuid, ItemStack instrumentStack,
                             ResourceLocation melodyId, ResourceLocation instrumentItemId,
                             @Nullable BlockPos pos, int entityId, @Nullable Runnable sbOnFinished) {
        if (instrumentStack.isEmpty() || melodyId == null) {
            BpMelodiesMod.LOGGER.info("[tracker] start ABORT empty={} melodyId={}", instrumentStack.isEmpty(), melodyId);
            return;
        }
        long len = lengthInTicksOrFallback(melodyId);
        if (len <= 0) {
            BpMelodiesMod.LOGGER.warn("[tracker] start ABORT zero-length melody={} len={}", melodyId, len);
            return;
        }
        long now = level.getGameTime();
        Session existing = SESSIONS.get(storageUuid);
        boolean wasExisting = existing != null;
        if (existing != null) {
            existing.melodyId = melodyId;
            existing.startGameTime = now;
            existing.endGameTime = now + len;
        } else {
            SESSIONS.put(storageUuid, new Session(level, storageUuid, instrumentStack,
                    melodyId, instrumentItemId, pos, entityId, now, now + len, sbOnFinished));
        }
        BpMelodiesMod.LOGGER.info("[tracker] start uuid={} melody={} len={} replacedExisting={} entityId={} pos={}",
                storageUuid, melodyId, len, wasExisting, entityId, pos);
        broadcastStart(level, pos, entityId, storageUuid, instrumentItemId, melodyId, now);
    }

    public static void startFromAccess(Player player, JukeboxAccess access) {
        ItemStack instrument = access.firstInstrumentStack();
        if (instrument == null) {
            BpMelodiesMod.LOGGER.info("[tracker] startFromAccess BAIL — no instrument in slot for uuid={}", access.storageUuid());
            return;
        }
        ResourceLocation melody = PlaybackNbt.getSelectedMelody(instrument);
        if (melody == null) {
            BpMelodiesMod.LOGGER.info("[tracker] startFromAccess BAIL — no selected_melody NBT on instrument for uuid={}", access.storageUuid());
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(instrument.getItem());
        Entity bearer = access.bearerEntity();
        BlockPos pos = access.blockPos();
        int eid = bearer != null ? bearer.getId() : -1;
        BlockPos resolvedPos = pos != null ? pos : (bearer != null ? bearer.blockPosition() : null);
        TB_ACCESS.put(access.storageUuid(), access);
        start(level, access.storageUuid(), instrument, melody, itemId, resolvedPos, eid, null);
    }

    public static void stop(UUID storageUuid) {
        Session s = SESSIONS.remove(storageUuid);
        clearShuffleHistory(storageUuid);
        BpMelodiesMod.LOGGER.info("[tracker] STOP called uuid={} hadSession={}", storageUuid, s != null);
        if (s == null) return;
        sendNear(s, new BackpackPlayStopMsg(storageUuid));
        if (BpMelodiesMod.SB_PRESENT) {
            try {
                net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler.stopPlayingDisc(
                        s.level,
                        s.blockPos != null ? net.minecraft.world.phys.Vec3.atCenterOf(s.blockPos) : net.minecraft.world.phys.Vec3.ZERO,
                        storageUuid);
            } catch (Throwable t) { }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SESSIONS.clear();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.bpmelodies.common.network.RequestMelodyListMsg.sendLibraryTo(sp);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();
        List<Session> snapshot = new ArrayList<>(SESSIONS.values());
        List<UUID> finished = null;
        for (Session s : snapshot) {
            if (s.level != level) continue;
            if (s.instrumentStack.isEmpty()) {
                if (finished == null) finished = new ArrayList<>();
                finished.add(s.storageUuid);
                continue;
            }
            if (now >= s.endGameTime) {
                if (finished == null) finished = new ArrayList<>();
                finished.add(s.storageUuid);
            }
        }
        if (finished != null) {
            for (UUID uuid : finished) {
                Session s = SESSIONS.remove(uuid);
                if (s != null) {
                    NextAction action = applyShuffleRepeat(s, uuid);
                    sendNear(s, new BackpackPlayStopMsg(uuid));
                    switch (action) {
                        case DELEGATE_SB -> {
                            if (s.sbOnFinished != null) {
                                BpMelodiesMod.LOGGER.info("[tracker] invoking sbOnFinished for uuid={}", uuid);
                                try { s.sbOnFinished.run(); }
                                catch (Throwable t) { BpMelodiesMod.LOGGER.debug("sbOnFinished threw", t); }
                            } else {
                                BpMelodiesMod.LOGGER.info("[tracker] no sbOnFinished for uuid={} (TB session?)", uuid);
                            }
                        }
                        case FORCE_PLAYNEXT -> {
                            var ref = WRAPPERS.get(uuid);
                            var wrapper = ref == null ? null : ref.get();
                            if (wrapper != null) {
                                BpMelodiesMod.LOGGER.info("[tracker] forcing wrapper.playNext() for uuid={}", uuid);
                                try { wrapper.playNext(); }
                                catch (Throwable t) { BpMelodiesMod.LOGGER.warn("[tracker] forced playNext failed", t); }
                            }
                        }
                        case STOP_END_OF_LIBRARY -> {
                            var ref = WRAPPERS.get(uuid);
                            var wrapper = ref == null ? null : ref.get();
                            if (wrapper != null) {
                                BpMelodiesMod.LOGGER.info("[tracker] end of library — stopping wrapper isPlaying for uuid={}", uuid);
                                invokeSetIsPlayingFalse(wrapper);
                            }
                        }
                        case TB_RESTART -> {
                            JukeboxAccess access = TB_ACCESS.get(uuid);
                            if (access != null) {
                                Player p = (s.entityId >= 0 && s.level.getEntity(s.entityId) instanceof Player pl) ? pl : null;
                                if (p != null) {
                                    BpMelodiesMod.LOGGER.info("[tracker] TB autoplay restart uuid={}", uuid);
                                    startFromAccess(p, access);
                                } else {
                                    BpMelodiesMod.LOGGER.info("[tracker] TB autoplay skipped — no player online for uuid={}", uuid);
                                }
                            }
                        }
                        case TB_STOP -> {
                            BpMelodiesMod.LOGGER.info("[tracker] TB end of library — stop uuid={}", uuid);
                        }
                    }
                }
            }
        }
    }

    enum NextAction { DELEGATE_SB, FORCE_PLAYNEXT, STOP_END_OF_LIBRARY, TB_RESTART, TB_STOP }

    private static NextAction applyShuffleRepeat(Session s, UUID uuid) {
        if (s.instrumentStack.isEmpty()) return NextAction.DELEGATE_SB;
        var ref = WRAPPERS.get(uuid);
        var wrapper = ref == null ? null : ref.get();

        boolean shuffle;
        PlaybackNbt.RepeatMode tbRep = null;
        net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode sbRep = null;
        boolean isSb = wrapper != null;
        JukeboxAccess tbAccess = isSb ? null : TB_ACCESS.get(uuid);
        if (isSb) {
            shuffle = wrapper.isShuffleEnabled();
            sbRep = wrapper.getRepeatMode();
            BpMelodiesMod.LOGGER.info("[tracker] session end (SB) uuid={} shuffle={} repeat={}", uuid, shuffle, sbRep);
        } else if (tbAccess != null) {
            ItemStack up = tbAccess.upgradeStack();
            shuffle = PlaybackNbt.getShuffle(up);
            tbRep = PlaybackNbt.getRepeat(up);
            BpMelodiesMod.LOGGER.info("[tracker] session end (TB) uuid={} shuffle={} repeat={}", uuid, shuffle, tbRep);
        } else {
            return NextAction.DELEGATE_SB;
        }

        var cur = PlaybackNbt.getSelectedMelody(s.instrumentStack);
        String playerName = null;
        if (s.entityId >= 0) {
            Entity e = s.level.getEntity(s.entityId);
            if (e instanceof Player p) playerName = p.getName().getString();
        }
        var all = sortedLibraryIds(playerName);
        ResourceLocation pick;
        boolean endOfLibrary = false;

        boolean isRepeatOne = isSb
                ? sbRep == net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode.ONE
                : tbRep == PlaybackNbt.RepeatMode.ONE;
        boolean isRepeatAll = isSb
                ? sbRep == net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode.ALL
                : tbRep == PlaybackNbt.RepeatMode.ALL;
        boolean isRepeatNo = !isRepeatOne && !isRepeatAll;

        if (isRepeatOne) {
            pick = cur;
        } else if (shuffle) {
            pick = all.isEmpty() ? null : all.get(PICK_RANDOM.nextInt(all.size()));
            if (pick != null && cur != null) pushShuffleHistory(uuid, cur);
        } else {
            if (all.isEmpty()) { pick = null; }
            else {
                int idx = cur == null ? -1 : all.indexOf(cur);
                int nextIdx = idx < 0 ? 0 : idx + 1;
                if (isRepeatNo && nextIdx >= all.size()) {
                    pick = null;
                    endOfLibrary = true;
                } else {
                    pick = all.get(Math.floorMod(nextIdx, all.size()));
                }
            }
        }

        if (pick != null && !pick.equals(cur)) {
            PlaybackNbt.setSelectedMelody(s.instrumentStack, pick);
            if (!isSb && tbAccess != null) tbAccess.markDiscSlotDirty(tbAccess.findInstrumentSlot());
            BpMelodiesMod.LOGGER.info("[tracker] advanced melody: {} -> {}", cur, pick);
        }

        if (!isSb) {
            if (pick == null) return NextAction.TB_STOP;
            return NextAction.TB_RESTART;
        }
        if (pick == null) return endOfLibrary ? NextAction.STOP_END_OF_LIBRARY : NextAction.DELEGATE_SB;
        if (isRepeatNo) return NextAction.FORCE_PLAYNEXT;
        return NextAction.DELEGATE_SB;
    }

    private static java.lang.reflect.Method SET_IS_PLAYING_METHOD;
    private static void invokeSetIsPlayingFalse(net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper wrapper) {
        try {
            if (SET_IS_PLAYING_METHOD == null) {
                SET_IS_PLAYING_METHOD = net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper.class.getDeclaredMethod("setIsPlaying", boolean.class);
                SET_IS_PLAYING_METHOD.setAccessible(true);
            }
            SET_IS_PLAYING_METHOD.invoke(wrapper, false);
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[tracker] invokeSetIsPlayingFalse failed", t);
        }
    }

    private static long lengthInTicksOrFallback(ResourceLocation rl) {
        try {
            int ms = ServerMelodyManager.getMelody(rl).getLength();
            return ms <= 0 ? -1 : Math.max(20, ms / 50);
        } catch (Throwable t) {
            return -1;
        }
    }

    public static List<ResourceLocation> sortedLibraryIds() {
        return sortedLibraryIds(null);
    }

    public static List<ResourceLocation> sortedLibraryIds(@Nullable String ownerPlayerName) {
        Set<ResourceLocation> ids = new HashSet<>();
        ids.addAll(ServerMelodyManager.getDatapackMelodies().keySet());
        try {
            ids.addAll(ServerMelodyManager.getIndex().getMelodies().keySet());
        } catch (Throwable t) { }
        List<ResourceLocation> list = new ArrayList<>(ids);
        list.sort((a, b) -> {
            int sa = imSortIndex(a, ownerPlayerName);
            int sb = imSortIndex(b, ownerPlayerName);
            if (sa != sb) return sb - sa;
            return a.toString().compareTo(b.toString());
        });
        return list;
    }

    public static int imSortIndex(ResourceLocation rl, @Nullable String ownerPlayerName) {
        if ("player".equals(rl.getNamespace())) {
            if (ownerPlayerName != null && rl.getPath().startsWith(ownerPlayerName + "/")) return 2;
            return 0;
        }
        return 1;
    }

    private static void sendNear(Session s, Object msg) {
        double x, y, z;
        Entity e = s.entityId >= 0 ? s.level.getEntity(s.entityId) : null;
        if (e != null) { x = e.getX(); y = e.getY(); z = e.getZ(); }
        else if (s.blockPos != null) { x = s.blockPos.getX() + 0.5; y = s.blockPos.getY() + 0.5; z = s.blockPos.getZ() + 0.5; }
        else { x = 0; y = 64; z = 0; }
        final double fx = x, fy = y, fz = z;
        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(fx, fy, fz, 64, s.level.dimension())),
                msg);
    }

    private static void broadcastStart(ServerLevel level, @Nullable BlockPos pos, int entityId, UUID storageUuid,
                                       ResourceLocation instrumentItemId, ResourceLocation melodyId, long startGameTime) {
        BackpackPlayStartMsg msg = new BackpackPlayStartMsg(storageUuid, instrumentItemId, melodyId, startGameTime, entityId, pos);
        double x, y, z;
        Entity e = entityId >= 0 ? level.getEntity(entityId) : null;
        if (e != null) { x = e.getX(); y = e.getY(); z = e.getZ(); }
        else if (pos != null) { x = pos.getX() + 0.5; y = pos.getY() + 0.5; z = pos.getZ() + 0.5; }
        else { x = 0; y = 64; z = 0; }
        final double fx = x, fy = y, fz = z;
        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(fx, fy, fz, 64, level.dimension())),
                msg);
    }
}
