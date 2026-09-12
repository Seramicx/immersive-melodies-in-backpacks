package com.bpmelodies.common.playback;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.network.BackpackDiscStartMsg;
import com.bpmelodies.common.network.BackpackPlayStopMsg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public final class ServerDiscTracker {
    private static final ServerDiscTracker INSTANCE = new ServerDiscTracker();

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Deque<Integer>> shuffleHistory = new HashMap<>();
    private final Random pickRandom = new Random();

    public static ServerDiscTracker getInstance() {
        return INSTANCE;
    }

    private static class Session {
        final ServerLevel level;
        final UUID storageUuid;
        final JukeboxAccess access;
        final int discSlot;
        final long endGameTime;
        final Set<UUID> listeners = new HashSet<>();

        Session(ServerLevel level, UUID storageUuid, JukeboxAccess access, int discSlot, long endGameTime) {
            this.level = level;
            this.storageUuid = storageUuid;
            this.access = access;
            this.discSlot = discSlot;
            this.endGameTime = endGameTime;
        }
    }

    public void startFromAccess(Player player, JukeboxAccess access) {
        int slot = access.findDiscSlot();
        if (slot >= 0) start(player, access, slot);
    }

    private void start(Player player, JukeboxAccess access, int slot) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack disc = access.discInventory().getStackInSlot(slot);
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), disc);
        if (song.isEmpty()) return;
        UUID uuid = access.storageUuid();
        ServerPlaybackTracker.stop(uuid);
        stopSession(uuid);
        Session s = new Session(level, uuid, access, slot, level.getGameTime() + song.get().value().lengthInTicks());
        sessions.put(uuid, s);
        broadcastStart(s, song.get().value().soundEvent().value().getLocation());
    }

    public void stop(UUID storageUuid) {
        shuffleHistory.remove(storageUuid);
        stopSession(storageUuid);
    }

    private void stopSession(UUID storageUuid) {
        Session s = sessions.remove(storageUuid);
        if (s == null) return;
        MinecraftServer server = s.level.getServer();
        if (server == null) return;
        BackpackPlayStopMsg msg = new BackpackPlayStopMsg(storageUuid);
        for (UUID id : s.listeners) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) PacketDistributor.sendToPlayer(p, msg);
        }
    }

    public void skip(Player player, JukeboxAccess access, int dir) {
        List<Integer> slots = access.discSlots();
        if (slots.isEmpty()) return;
        UUID uuid = access.storageUuid();
        ItemStack upgrade = access.upgradeStack();
        PlaybackNbt.RepeatMode rep = PlaybackNbt.getRepeat(upgrade);
        Session cur = sessions.get(uuid);
        int curSlot = cur == null ? -1 : cur.discSlot;

        Integer pick;
        if (rep == PlaybackNbt.RepeatMode.ONE && curSlot >= 0) {
            pick = curSlot;
        } else if (PlaybackNbt.getShuffle(upgrade)) {
            if (dir > 0) {
                pick = pickShuffle(slots, curSlot);
                if (pick != null && curSlot >= 0) {
                    shuffleHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>()).push(curSlot);
                }
            } else {
                Deque<Integer> hist = shuffleHistory.get(uuid);
                pick = hist == null || hist.isEmpty() ? null : hist.pop();
            }
        } else {
            int idx = slots.indexOf(curSlot);
            int target = (idx < 0 ? 0 : idx) + dir;
            if (target < 0 || target >= slots.size()) {
                pick = rep == PlaybackNbt.RepeatMode.ALL ? slots.get(Math.floorMod(target, slots.size())) : null;
            } else {
                pick = slots.get(target);
            }
        }

        if (pick != null) start(player, access, pick);
        else stop(uuid);
    }

    @Nullable
    private Integer pickShuffle(List<Integer> slots, int current) {
        if (slots.size() == 1) return slots.get(0);
        List<Integer> pool = new ArrayList<>(slots);
        pool.remove(Integer.valueOf(current));
        if (pool.isEmpty()) return null;
        return pool.get(pickRandom.nextInt(pool.size()));
    }

    @SubscribeEvent
    public void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        long now = level.getGameTime();
        for (Session s : new ArrayList<>(sessions.values())) {
            if (s.level != level) continue;
            Entity bearer = s.access.bearerEntity();
            if (bearer != null && bearer.isRemoved()) {
                stop(s.storageUuid);
            } else if (now >= s.endGameTime) {
                if (bearer instanceof Player player) skip(player, s.access, +1);
                else stop(s.storageUuid);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        sessions.clear();
        shuffleHistory.clear();
    }

    private void broadcastStart(Session s, ResourceLocation sound) {
        Entity bearer = s.access.bearerEntity();
        BlockPos pos = s.access.blockPos();
        BackpackDiscStartMsg msg = new BackpackDiscStartMsg(s.storageUuid, sound,
                bearer == null ? -1 : bearer.getId(), pos);
        Vec3 at;
        if (bearer != null) at = bearer.position();
        else if (pos != null) at = Vec3.atCenterOf(pos);
        else at = new Vec3(0, 64, 0);
        for (ServerPlayer p : s.level.players()) {
            if (p.distanceToSqr(at) > 64 * 64) continue;
            s.listeners.add(p.getUUID());
            PacketDistributor.sendToPlayer(p, msg);
        }
    }
}
