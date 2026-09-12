package com.bpmelodies.client.playback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;

public class BackpackDiscSound extends AbstractTickableSoundInstance {
    private final int entityId;

    BackpackDiscSound(ResourceLocation soundId, int entityId, @Nullable BlockPos pos) {
        super(SoundEvent.createVariableRangeEvent(soundId), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.entityId = entityId;
        this.volume = 4.0F;
        this.looping = false;
        this.delay = 0;
        if (pos != null) {
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
        }
    }

    @Override
    public void tick() {
        if (entityId < 0) return;
        if (Minecraft.getInstance().level == null) {
            stop();
            return;
        }
        Entity e = Minecraft.getInstance().level.getEntity(entityId);
        if (e == null) {
            stop();
            return;
        }
        this.x = e.getX();
        this.y = e.getY();
        this.z = e.getZ();
    }
}
