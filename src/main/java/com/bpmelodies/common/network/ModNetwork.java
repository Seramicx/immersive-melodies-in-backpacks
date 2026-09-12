package com.bpmelodies.common.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(SetSelectedMelodyMsg.TYPE, SetSelectedMelodyMsg.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetTransportFlagMsg.TYPE, SetTransportFlagMsg.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestMelodyListMsg.TYPE, RequestMelodyListMsg.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleImModeMsg.TYPE, ToggleImModeMsg.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePlaylistMsg.TYPE, TogglePlaylistMsg.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(BackpackPlayStartMsg.TYPE, BackpackPlayStartMsg.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BackpackPlayStopMsg.TYPE, BackpackPlayStopMsg.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BackpackDiscStartMsg.TYPE, BackpackDiscStartMsg.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MelodyLibrarySyncMsg.TYPE, MelodyLibrarySyncMsg.STREAM_CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SetSelectedMelodyMsg.TYPE, (msg, ctx) ->
                ctx.server().execute(() -> SetSelectedMelodyMsg.handle(msg, ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(SetTransportFlagMsg.TYPE, (msg, ctx) ->
                ctx.server().execute(() -> SetTransportFlagMsg.handle(msg, ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(RequestMelodyListMsg.TYPE, (msg, ctx) ->
                ctx.server().execute(() -> RequestMelodyListMsg.sendLibraryTo(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(ToggleImModeMsg.TYPE, (msg, ctx) ->
                ctx.server().execute(() -> ToggleImModeMsg.handle(msg, ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(TogglePlaylistMsg.TYPE, (msg, ctx) ->
                ctx.server().execute(() -> TogglePlaylistMsg.handle(msg, ctx.player())));
    }

    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void toNear(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
        Vec3 origin = new Vec3(x, y, z);
        for (ServerPlayer p : PlayerLookup.around(level, origin, radius)) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}
