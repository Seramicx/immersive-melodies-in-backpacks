package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ModNetwork {
    public static final ResourceLocation SET_SELECTED = id("set_selected");
    public static final ResourceLocation SET_TRANSPORT = id("set_transport");
    public static final ResourceLocation REQUEST_LIST = id("request_list");
    public static final ResourceLocation LIBRARY_SYNC = id("library_sync");
    public static final ResourceLocation TOGGLE_IM = id("toggle_im");
    public static final ResourceLocation PLAY_START = id("play_start");
    public static final ResourceLocation PLAY_STOP = id("play_stop");

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BpMelodiesMod.MODID, path);
    }

    private ModNetwork() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SET_SELECTED, (server, player, h, buf, sender) -> {
            SetSelectedMelodyMsg msg = SetSelectedMelodyMsg.decode(buf);
            server.execute(() -> SetSelectedMelodyMsg.handle(msg, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(SET_TRANSPORT, (server, player, h, buf, sender) -> {
            SetTransportFlagMsg msg = SetTransportFlagMsg.decode(buf);
            server.execute(() -> SetTransportFlagMsg.handle(msg, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_LIST, (server, player, h, buf, sender) ->
                server.execute(() -> RequestMelodyListMsg.sendLibraryTo(player)));
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_IM, (server, player, h, buf, sender) -> {
            ToggleImModeMsg msg = ToggleImModeMsg.decode(buf);
            server.execute(() -> ToggleImModeMsg.handle(msg, player));
        });
    }

    public static FriendlyByteBuf buf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static void toPlayer(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void toNear(ServerLevel level, double x, double y, double z, double radius,
                              ResourceLocation channel, FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), radius)) {
            ServerPlayNetworking.send(player, channel, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
        }
    }
}
