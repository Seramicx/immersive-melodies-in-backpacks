package com.bpmelodies.common.network;

import immersive_melodies.resources.ClientMelodyManager;
import immersive_melodies.resources.MelodyDescriptor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record MelodyLibrarySyncMsg(Map<ResourceLocation, String> melodies) {

    public static void encode(MelodyLibrarySyncMsg m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.melodies.size());
        for (Map.Entry<ResourceLocation, String> e : m.melodies.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    public static MelodyLibrarySyncMsg decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, String> m = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            m.put(buf.readResourceLocation(), buf.readUtf());
        }
        return new MelodyLibrarySyncMsg(m);
    }

    public static void handle(MelodyLibrarySyncMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Map<ResourceLocation, MelodyDescriptor> dst = ClientMelodyManager.getMelodiesList();
            dst.clear();
            for (Map.Entry<ResourceLocation, String> e : msg.melodies.entrySet()) {
                dst.put(e.getKey(), new MelodyDescriptor(e.getValue()));
            }
        }));
    }
}
