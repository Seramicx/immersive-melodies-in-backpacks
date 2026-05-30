package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import immersive_melodies.resources.ServerMelodyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public final class RequestMelodyListMsg {
    private RequestMelodyListMsg() {}

    public static void sendLibraryTo(ServerPlayer player) {
        try {
            Map<ResourceLocation, String> melodies = new HashMap<>();
            ServerMelodyManager.getDatapackMelodies().forEach((id, lazy) ->
                    melodies.put(id, lazy.getDescriptor().getName()));
            try {
                ServerMelodyManager.getIndex().getMelodies().forEach((id, desc) ->
                        melodies.put(id, desc.getName()));
            } catch (Throwable ignored) {
            }
            FriendlyByteBuf buf = ModNetwork.buf();
            MelodyLibrarySyncMsg.encode(new MelodyLibrarySyncMsg(melodies), buf);
            ModNetwork.toPlayer(player, ModNetwork.LIBRARY_SYNC, buf);
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to push melody library to {}", player.getGameProfile().getName(), t);
        }
    }
}
