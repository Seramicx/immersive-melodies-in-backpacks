package com.bpmelodies.common.network;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ToggleImModeMsg(boolean enabled) {
    public static void encode(ToggleImModeMsg m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.enabled);
    }

    public static ToggleImModeMsg decode(FriendlyByteBuf buf) {
        return new ToggleImModeMsg(buf.readBoolean());
    }

    public static void handle(ToggleImModeMsg msg, Player player) {
        if (player == null) return;
        JukeboxAccess.findJukeboxInOpenMenu(player).ifPresent(access -> {
            PlaybackNbt.setImEnabled(access.upgradeStack(), msg.enabled);
            access.markUpgradeDirty();
            if (!msg.enabled) {
                ServerPlaybackTracker.stop(access.storageUuid());
            }
        });
    }
}
