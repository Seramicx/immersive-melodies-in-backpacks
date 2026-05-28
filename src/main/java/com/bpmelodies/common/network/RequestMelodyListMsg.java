package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import immersive_melodies.resources.ServerMelodyManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record RequestMelodyListMsg() implements CustomPacketPayload {
    public static final Type<RequestMelodyListMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "req_melodies"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMelodyListMsg> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {}, buf -> new RequestMelodyListMsg());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestMelodyListMsg msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
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
            } catch (Throwable ignored) {
            }
            PacketDistributor.sendToPlayer(player, new MelodyLibrarySyncMsg(melodies));
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to push melody library to {}", player.getGameProfile().getName(), t);
        }
    }
}
