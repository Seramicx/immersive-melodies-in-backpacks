package com.bpmelodies.client.playback;

import com.bpmelodies.common.network.BackpackDiscStartMsg;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientDiscTracker {
    private static final ClientDiscTracker INSTANCE = new ClientDiscTracker();

    private final Map<UUID, BackpackDiscSound> sounds = new HashMap<>();

    public static ClientDiscTracker getInstance() {
        return INSTANCE;
    }

    public void start(BackpackDiscStartMsg msg) {
        stop(msg.storageUuid());
        BackpackDiscSound sound = new BackpackDiscSound(msg.soundId(), msg.sourceEntityId(), msg.sourceBlockPos());
        sounds.put(msg.storageUuid(), sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public void stop(UUID storageUuid) {
        BackpackDiscSound sound = sounds.remove(storageUuid);
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
    }

    public void clearAll() {
        for (BackpackDiscSound s : sounds.values()) Minecraft.getInstance().getSoundManager().stop(s);
        sounds.clear();
    }
}
