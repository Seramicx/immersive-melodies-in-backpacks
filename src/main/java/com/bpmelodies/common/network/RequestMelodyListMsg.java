package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import immersive_melodies.resources.ServerMelodyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record RequestMelodyListMsg() {
    public static void encode(RequestMelodyListMsg msg, FriendlyByteBuf buf) {}

    public static RequestMelodyListMsg decode(FriendlyByteBuf buf) {
        return new RequestMelodyListMsg();
    }

    public static void handle(RequestMelodyListMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        sendLibraryTo(player);
    }

    public static void sendLibraryTo(ServerPlayer player) {
        try {
            Map<ResourceLocation, String> melodies = new HashMap<>();
            ServerMelodyManager.getDatapackMelodies().forEach((id, lazy) ->
                    melodies.put(id, lazy.getDescriptor().getName()));
            try {
                ServerMelodyManager.getIndex().getMelodies().forEach((id, desc) ->
                        melodies.put(id, desc.getName()));
            } catch (Throwable t) {
                BpMelodiesMod.LOGGER.info("[bpmelodies] Index not initialized yet for {} — sending {} datapack melodies only", player.getGameProfile().getName(), melodies.size());
            }
            BpMelodiesMod.LOGGER.info("[bpmelodies] Pushing {} melodies to {}", melodies.size(), player.getGameProfile().getName());
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MelodyLibrarySyncMsg(melodies));
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to push melody library to {}", player.getGameProfile().getName(), t);
        }
    }
}
