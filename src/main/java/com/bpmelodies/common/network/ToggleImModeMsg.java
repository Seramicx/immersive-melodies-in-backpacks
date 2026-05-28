package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleImModeMsg(boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleImModeMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "toggle_im"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleImModeMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, m) -> buf.writeBoolean(m.enabled),
                    buf -> new ToggleImModeMsg(buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ToggleImModeMsg msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer)) return;
        JukeboxAccess.findJukeboxInOpenMenu(ctx.player()).ifPresent(access -> {
            PlaybackNbt.setImEnabled(access.upgradeStack(), msg.enabled);
            access.markUpgradeDirty();
            if (!msg.enabled) {
                ServerPlaybackTracker.stop(access.storageUuid());
            }
        });
    }
}
