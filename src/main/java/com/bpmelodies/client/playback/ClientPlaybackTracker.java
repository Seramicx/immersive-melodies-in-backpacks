package com.bpmelodies.client.playback;

import com.bpmelodies.common.network.BackpackPlayStartMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ClientPlaybackTracker {
    private static final Map<UUID, BackpackPlaybackSession> SESSIONS = new HashMap<>();

    private ClientPlaybackTracker() {}

    public static void start(BackpackPlayStartMsg msg) {
        com.bpmelodies.BpMelodiesMod.LOGGER.info("[client-tracker] start uuid={} melody={} startTime={} entityId={} hadExisting={}",
                msg.storageUuid(), msg.melodyId(), msg.startGameTime(), msg.sourceEntityId(),
                SESSIONS.containsKey(msg.storageUuid()));
        BackpackPlaybackSession existing = SESSIONS.get(msg.storageUuid());
        if (existing != null) existing.cancelActive();
        BackpackPlaybackSession fresh = new BackpackPlaybackSession(
                msg.storageUuid(), msg.instrumentItemId(), msg.melodyId(),
                msg.startGameTime(), msg.sourceEntityId(), msg.sourceBlockPos());
        SESSIONS.put(msg.storageUuid(), fresh);
        com.bpmelodies.BpMelodiesMod.LOGGER.info("[client-tracker] session put — map size={} new instance hash={}", SESSIONS.size(), System.identityHashCode(fresh));
    }

    public static void stop(UUID storageUuid) {
        BackpackPlaybackSession s = SESSIONS.remove(storageUuid);
        com.bpmelodies.BpMelodiesMod.LOGGER.info("[client-tracker] STOP called uuid={} hadSession={} stackTrace={}",
                storageUuid, s != null, java.util.Arrays.stream(Thread.currentThread().getStackTrace())
                        .limit(6).map(StackTraceElement::toString).reduce("", (a, b) -> a + " | " + b));
        if (s != null) s.stop();
    }

    public static boolean isPlaying(UUID storageUuid) {
        return SESSIONS.containsKey(storageUuid);
    }

    @Nullable
    public static ResourceLocation getCurrentMelody(UUID storageUuid) {
        BackpackPlaybackSession s = SESSIONS.get(storageUuid);
        return s == null ? null : s.melodyId;
    }

    private static int wholeTrackerTickCount = 0;
    private static boolean lastWasPaused = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        wholeTrackerTickCount++;
        boolean paused = Minecraft.getInstance().isPaused();
        if (paused != lastWasPaused) {
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[client-tracker] paused state changed: {} sessions={}", paused, SESSIONS.size());
            lastWasPaused = paused;
        }
        if (paused) return;
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        Iterator<Map.Entry<UUID, BackpackPlaybackSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BackpackPlaybackSession> e = it.next();
            BackpackPlaybackSession s = e.getValue();
            s.tick();
            if (s.ended(now)) {
                com.bpmelodies.BpMelodiesMod.LOGGER.info("[client-tracker] session ENDED uuid={} melody={}", s.storageUuid, s.melodyId);
                s.stop();
                it.remove();
            }
        }
    }
}
