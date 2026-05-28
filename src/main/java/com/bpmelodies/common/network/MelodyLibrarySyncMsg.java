package com.bpmelodies.common.network;

import com.bpmelodies.BpMelodiesMod;
import immersive_melodies.resources.ClientMelodyManager;
import immersive_melodies.resources.MelodyDescriptor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record MelodyLibrarySyncMsg(Map<ResourceLocation, String> melodies) implements CustomPacketPayload {
    public static final Type<MelodyLibrarySyncMsg> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BpMelodiesMod.MODID, "melody_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MelodyLibrarySyncMsg> STREAM_CODEC =
            StreamCodec.of(
                    (buf, m) -> {
                        buf.writeVarInt(m.melodies.size());
                        for (Map.Entry<ResourceLocation, String> e : m.melodies.entrySet()) {
                            buf.writeResourceLocation(e.getKey());
                            buf.writeUtf(e.getValue());
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        Map<ResourceLocation, String> m = new HashMap<>(size);
                        for (int i = 0; i < size; i++) {
                            m.put(buf.readResourceLocation(), buf.readUtf());
                        }
                        return new MelodyLibrarySyncMsg(m);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MelodyLibrarySyncMsg msg, IPayloadContext ctx) {
        Map<ResourceLocation, MelodyDescriptor> dst = ClientMelodyManager.getMelodiesList();
        dst.clear();
        for (Map.Entry<ResourceLocation, String> e : msg.melodies.entrySet()) {
            dst.put(e.getKey(), new MelodyDescriptor(e.getValue()));
        }
    }
}
