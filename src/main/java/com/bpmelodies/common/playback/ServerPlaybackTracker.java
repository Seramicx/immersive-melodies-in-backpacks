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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
    private static final Map<UUID, WeakReference<Object>> WRAPPERS = new HashMap<>();
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
        if (toStop != null) for (UUID u : toStop) stop(u);
    }

    public static void stopSessionsForTbHandler(Object handler) {
        List<UUID> toStop = null;
        for (Map.Entry<UUID, JukeboxAccess> e : TB_ACCESS.entrySet()) {
            if (e.getValue().discInventory() == handler) {
                if (toStop == null) toStop = new ArrayList<>();
                toStop.add(e.getKey());
            }
        }
        if (toStop != null) for (UUID u : toStop) stop(u);
    }

    private ServerPlaybackTracker() {}

    // Signatures must stay loader-neutral: Forge scans every public method descriptor when the
    // class is registered on the event bus, which resolves SB types even with SB absent.
    public static void registerWrapper(UUID storageUuid, Object wrapper) {
        WRAPPERS.put(storageUuid, new WeakReference<>(wrapper));
    }

    @javax.annotation.Nullable
    public static Object getWrapperFor(UUID storageUuid) {
        WeakReference<Object> ref = WRAPPERS.get(storageUuid);
        return ref == null ? null : ref.get();
    }

    public static class Session {
        public ServerLevel level;
        public final UUID storageUuid;
        public final ItemStack instrumentStack;
        public ResourceLocation melodyId;
        public final ResourceLocation instrumentItemId;
        @Nullable public BlockPos blockPos;
        public int entityId;
        public long startGameTime;
        public long endGameTime;
        @Nullable public final Runnable sbOnFinished;
        public final Set<UUID> listeners = new HashSet<>();

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
        if (instrumentStack.isEmpty() || melodyId == null) return;
        long len = lengthInTicksOrFallback(melodyId);
        if (len <= 0) {
            BpMelodiesMod.LOGGER.warn("[tracker] start ABORT zero-length melody={} len={}", melodyId, len);
            return;
        }
        long now = level.getGameTime();
        Session s = SESSIONS.get(storageUuid);
        if (s != null) {
            s.melodyId = melodyId;
            s.startGameTime = now;
            s.endGameTime = now + len;
            s.level = level;
            s.blockPos = pos;
            s.entityId = entityId;
        } else {
            s = new Session(level, storageUuid, instrumentStack,
                    melodyId, instrumentItemId, pos, entityId, now, now + len, sbOnFinished);
            SESSIONS.put(storageUuid, s);
        }
        broadcastStart(s);
    }

    public static void startFromAccess(Player player, JukeboxAccess access) {
        ItemStack instrument = access.firstInstrumentStack();
        if (instrument == null) return;
        ResourceLocation melody = PlaybackNbt.getSelectedMelody(instrument);
        if (melody == null) return;
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
        TB_ACCESS.remove(storageUuid);
        if (s == null) return;
        sendToListeners(s, new BackpackPlayStopMsg(storageUuid));
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
        TB_ACCESS.clear();
        WRAPPERS.clear();
        SHUFFLE_HISTORY.clear();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.bpmelodies.common.network.RequestMelodyListMsg.sendLibraryTo(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        stopSessionsFor(event.getEntity().getId());
    }

    public static void stopSessionsFor(int entityId) {
        if (entityId < 0) return;
        List<UUID> toStop = null;
        for (Session s : SESSIONS.values()) {
            if (s.entityId == entityId) {
                if (toStop == null) toStop = new ArrayList<>();
                toStop.add(s.storageUuid);
            }
        }
        if (toStop != null) for (UUID u : toStop) stop(u);
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
                    sendToListeners(s, new BackpackPlayStopMsg(uuid));
                    switch (action) {
                        case DELEGATE_SB -> {
                            if (s.sbOnFinished != null) {
                                try { s.sbOnFinished.run(); }
                                catch (Throwable ignored) { }
                            }
                        }
                        case FORCE_PLAYNEXT -> {
                            Object wrapper = getWrapperFor(uuid);
                            if (wrapper != null) {
                                try { ((net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper) wrapper).playNext(); }
                                catch (Throwable t) { BpMelodiesMod.LOGGER.warn("[tracker] forced playNext failed", t); }
                            }
                        }
                        case STOP_END_OF_LIBRARY -> {
                            Object wrapper = getWrapperFor(uuid);
                            if (wrapper != null) invokeSetIsPlayingFalse(wrapper);
                        }
                        case TB_RESTART -> {
                            JukeboxAccess access = TB_ACCESS.get(uuid);
                            if (access != null) {
                                Player p = (s.entityId >= 0 && s.level.getEntity(s.entityId) instanceof Player pl) ? pl : null;
                                if (p != null) startFromAccess(p, access);
                            }
                        }
                        case TB_STOP -> { }
                    }
                }
            }
        }
    }

    enum NextAction { DELEGATE_SB, FORCE_PLAYNEXT, STOP_END_OF_LIBRARY, TB_RESTART, TB_STOP }

    private static NextAction applyShuffleRepeat(Session s, UUID uuid) {
        if (s.instrumentStack.isEmpty()) return NextAction.DELEGATE_SB;
        Object wrapper = getWrapperFor(uuid);

        boolean shuffle;
        PlaybackNbt.RepeatMode tbRep = null;
        net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode sbRep = null;
        boolean isSb = wrapper != null;
        JukeboxAccess tbAccess = isSb ? null : TB_ACCESS.get(uuid);
        ItemStack up;
        if (isSb) {
            net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper sbWrapper =
                    (net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper) wrapper;
            shuffle = sbWrapper.isShuffleEnabled();
            sbRep = sbWrapper.getRepeatMode();
            up = sbWrapper.getUpgradeStack();
        } else if (tbAccess != null) {
            up = tbAccess.upgradeStack();
            shuffle = PlaybackNbt.getShuffle(up);
            tbRep = PlaybackNbt.getRepeat(up);
        } else {
            return NextAction.DELEGATE_SB;
        }

        var cur = PlaybackNbt.getSelectedMelody(s.instrumentStack);
        String playerName = null;
        if (s.entityId >= 0) {
            Entity e = s.level.getEntity(s.entityId);
            if (e instanceof Player p) playerName = p.getName().getString();
        }
        var all = playlistIds(playerName, up);
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
            pick = pickShuffle(all, cur);
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
    private static void invokeSetIsPlayingFalse(Object wrapper) {
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

    public static List<ResourceLocation> playlistIds(@Nullable String ownerPlayerName, ItemStack upgradeStack) {
        Set<String> excluded = PlaybackNbt.getExcluded(upgradeStack);
        List<ResourceLocation> all = sortedLibraryIds(ownerPlayerName);
        if (excluded.isEmpty()) return all;
        List<ResourceLocation> kept = new ArrayList<>();
        for (ResourceLocation id : all) {
            if (!excluded.contains(id.toString())) kept.add(id);
        }
        return kept;
    }

    @Nullable
    public static ResourceLocation pickShuffle(List<ResourceLocation> pool, @Nullable ResourceLocation cur) {
        if (pool.isEmpty()) return null;
        int idx = cur == null ? -1 : pool.indexOf(cur);
        if (idx < 0 || pool.size() == 1) return pool.get(PICK_RANDOM.nextInt(pool.size()));
        int r = PICK_RANDOM.nextInt(pool.size() - 1);
        return pool.get(r >= idx ? r + 1 : r);
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

    private static Vec3 sourcePos(Session s) {
        Entity e = s.entityId >= 0 ? s.level.getEntity(s.entityId) : null;
        if (e != null) return e.position();
        if (s.blockPos != null) return Vec3.atCenterOf(s.blockPos);
        return new Vec3(0, 64, 0);
    }

    // Stops go to whoever got the start, not to whoever is nearby now: a listener who walked
    // out of the broadcast radius still has a session running client-side.
    private static void sendToListeners(Session s, Object msg) {
        MinecraftServer server = s.level.getServer();
        if (server == null) return;
        for (UUID id : s.listeners) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
        }
    }

    private static void broadcastStart(Session s) {
        BackpackPlayStartMsg msg = new BackpackPlayStartMsg(s.storageUuid, s.instrumentItemId, s.melodyId,
                s.startGameTime, s.entityId, s.blockPos);
        Vec3 at = sourcePos(s);
        s.listeners.clear();
        for (ServerPlayer p : s.level.players()) {
            if (p.distanceToSqr(at) > 64 * 64) continue;
            s.listeners.add(p.getUUID());
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
        }
    }
}
