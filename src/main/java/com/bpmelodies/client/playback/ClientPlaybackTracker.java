package com.bpmelodies.client.playback;

import com.bpmelodies.common.network.BackpackPlayStartMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ClientPlaybackTracker {
    private static final Map<UUID, BackpackPlaybackSession> SESSIONS = new HashMap<>();

    private ClientPlaybackTracker() {}

    public static void start(BackpackPlayStartMsg msg) {
        BackpackPlaybackSession existing = SESSIONS.get(msg.storageUuid());
        if (existing != null) existing.cancelActive();
        SESSIONS.put(msg.storageUuid(), new BackpackPlaybackSession(
                msg.storageUuid(), msg.instrumentItemId(), msg.melodyId(),
                msg.startGameTime(), msg.sourceEntityId(), msg.sourceBlockPos()));
    }

    public static void stop(UUID storageUuid) {
        BackpackPlaybackSession s = SESSIONS.remove(storageUuid);
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        Iterator<Map.Entry<UUID, BackpackPlaybackSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BackpackPlaybackSession> e = it.next();
            BackpackPlaybackSession s = e.getValue();
            s.tick();
            if (s.ended(now)) {
                s.stop();
                it.remove();
            }
        }
    }
}
