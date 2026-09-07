package com.bpmelodies.client.playback;

import immersive_melodies.client.sound.CancelableSoundInstance;
import immersive_melodies.item.InstrumentItem;
import immersive_melodies.resources.ClientMelodyManager;
import immersive_melodies.resources.Melody;
import immersive_melodies.resources.Note;
import immersive_melodies.resources.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BackpackPlaybackSession {
    public final UUID storageUuid;
    public final ResourceLocation instrumentItemId;
    public final ResourceLocation melodyId;
    public long startGameTime;
    public final int sourceEntityId;
    @Nullable public final BlockPos sourceBlockPos;

    @Nullable private Melody resolvedMelody;
    @Nullable private InstrumentItem resolvedInstrument;
    @Nullable private ArmorStand blockSpeaker;
    private final Map<Integer, Integer> lastNoteIndex = new HashMap<>();
    private final List<CancelableSoundInstance> activeNotes = new ArrayList<>();
    private int tickCount = 0;

    public BackpackPlaybackSession(UUID storageUuid, ResourceLocation instrumentItemId,
                                   ResourceLocation melodyId, long startGameTime,
                                   int sourceEntityId, @Nullable BlockPos sourceBlockPos) {
        this.storageUuid = storageUuid;
        this.instrumentItemId = instrumentItemId;
        this.melodyId = melodyId;
        this.startGameTime = startGameTime;
        this.sourceEntityId = sourceEntityId;
        this.sourceBlockPos = sourceBlockPos;
    }

    public boolean ended(long currentGameTime) {
        long elapsedMs = (currentGameTime - startGameTime) * 50L;
        if (resolvedMelody == null || resolvedMelody.getLength() <= 0) return elapsedMs > 30000;
        return elapsedMs > resolvedMelody.getLength() + 1000;
    }

    public void tick() {
        tickCount++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (resolvedInstrument == null) {
            Item it = BuiltInRegistries.ITEM.get(instrumentItemId);
            if (it instanceof InstrumentItem inst) resolvedInstrument = inst;
            else return;
        }
        if (resolvedMelody == null || resolvedMelody.getLength() <= 0) {
            Melody m = ClientMelodyManager.getMelody(melodyId);
            if (m.getLength() <= 0) return;
            resolvedMelody = m;
        }

        Entity source = sourceEntityId >= 0 ? mc.level.getEntity(sourceEntityId) : speaker(mc);
        boolean audible = source != null;
        if (audible) {
            double dx = source.getX() - mc.player.getX();
            double dy = source.getY() - mc.player.getY();
            double dz = source.getZ() - mc.player.getZ();
            audible = dx * dx + dy * dy + dz * dz <= 96 * 96;
        }
        if (!audible) cancelActive();

        long elapsedMs = (mc.level.getGameTime() - startGameTime) * 50L;

        List<Track> tracks = resolvedMelody.getTracks();
        for (int t = 0; t < tracks.size(); t++) {
            List<Note> notes = tracks.get(t).getNotes();
            int idx = lastNoteIndex.getOrDefault(t, 0);
            for (int i = idx; i < notes.size(); i++) {
                Note n = notes.get(i);
                if (n.getTime() > elapsedMs) {
                    lastNoteIndex.put(t, i);
                    break;
                }
                if (audible) {
                    try {
                        CancelableSoundInstance si = resolvedInstrument.playNote(source, n, elapsedMs);
                        if (si != null) activeNotes.add(si);
                    } catch (Throwable t2) {
                        com.bpmelodies.BpMelodiesMod.LOGGER.warn("[playback-session] playNote threw", t2);
                    }
                }
                lastNoteIndex.put(t, i + 1);
            }
        }
    }

    // Immersive Melodies anchors every note to an Entity, so a placed backpack needs a stand-in at the block.
    @Nullable
    private ArmorStand speaker(Minecraft mc) {
        if (sourceBlockPos == null) return null;
        if (blockSpeaker == null) {
            blockSpeaker = new ArmorStand(mc.level, sourceBlockPos.getX() + 0.5, sourceBlockPos.getY() + 0.5, sourceBlockPos.getZ() + 0.5);
            blockSpeaker.setInvisible(true);
        }
        return blockSpeaker;
    }

    public void cancelActive() {
        for (CancelableSoundInstance si : activeNotes) {
            try { si.cancel(); } catch (Throwable ignored) {}
        }
        activeNotes.clear();
    }

    public void stop() {
        cancelActive();
        if (blockSpeaker != null) {
            blockSpeaker.discard();
            blockSpeaker = null;
        }
    }

}
