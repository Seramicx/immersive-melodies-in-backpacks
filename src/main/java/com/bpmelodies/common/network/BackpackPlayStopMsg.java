package com.bpmelodies.common.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record BackpackPlayStopMsg(UUID storageUuid) {
    public static void encode(BackpackPlayStopMsg m, FriendlyByteBuf buf) {
        buf.writeUUID(m.storageUuid);
    }

    public static BackpackPlayStopMsg decode(FriendlyByteBuf buf) {
        return new BackpackPlayStopMsg(buf.readUUID());
    }
}
