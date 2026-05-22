package com.bpmelodies.common.handler;

import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import immersive_melodies.item.InstrumentItem;
import immersive_melodies.resources.Melody;
import immersive_melodies.resources.MelodyDescriptor;
import immersive_melodies.resources.ServerMelodyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;

import java.util.Optional;
import java.util.UUID;

public class ImInstrumentDiscHandler implements IDiscHandler<MelodyDescriptor> {

    @Override
    public Optional<MelodyDescriptor> getSongInfo(ItemStack itemStack, Level level) {
        ResourceLocation rl = PlaybackNbt.getSelectedMelody(itemStack);
        if (rl == null || level.isClientSide()) return Optional.empty();
        try {
            return Optional.of(ServerMelodyManager.getMelody(rl));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public void playDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, ItemStack discItemStack, Runnable onFinished) {
        if (!isImEnabledFor(storageUuid)) {
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[handler] playDisc (block) skipped — IM disabled for {}", storageUuid);
            return;
        }
        ResourceLocation rl = PlaybackNbt.getSelectedMelody(discItemStack);
        com.bpmelodies.BpMelodiesMod.LOGGER.info("[handler] playDisc (block) storageUuid={} melody={}", storageUuid, rl);
        if (rl == null) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(discItemStack.getItem());
        keepSbAlive(serverLevel, position, storageUuid);
        ServerPlaybackTracker.start(serverLevel, storageUuid, discItemStack, rl, itemId, position, -1, onFinished);
    }

    @Override
    public void playDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, ItemStack discItemStack, int entityId, Runnable onFinished) {
        if (!isImEnabledFor(storageUuid)) {
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[handler] playDisc (entity) skipped — IM disabled for {}", storageUuid);
            return;
        }
        ResourceLocation rl = PlaybackNbt.getSelectedMelody(discItemStack);
        com.bpmelodies.BpMelodiesMod.LOGGER.info("[handler] playDisc (entity) storageUuid={} entityId={} melody={}", storageUuid, entityId, rl);
        if (rl == null) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(discItemStack.getItem());
        keepSbAlive(serverLevel, BlockPos.containing(position), storageUuid);
        ServerPlaybackTracker.start(serverLevel, storageUuid, discItemStack, rl, itemId, BlockPos.containing(position), entityId, onFinished);
    }

    private static void keepSbAlive(ServerLevel level, BlockPos pos, UUID storageUuid) {
        try {
            ServerStorageSoundHandler.putSoundInfo(level, storageUuid, () -> {}, Vec3.atCenterOf(pos), Long.MAX_VALUE);
        } catch (Throwable t) {
        }
    }

    private static boolean isImEnabledFor(UUID storageUuid) {
        var wrapper = com.bpmelodies.common.playback.ServerPlaybackTracker.getWrapperFor(storageUuid);
        if (wrapper == null) return true;
        return PlaybackNbt.isImEnabled(wrapper.getUpgradeStack());
    }

    @Override
    public Optional<Integer> getMusicLengthInTicks(ItemStack itemStack, Level level) {
        if (level.isClientSide()) return Optional.empty();
        ResourceLocation rl = PlaybackNbt.getSelectedMelody(itemStack);
        if (rl == null) return Optional.empty();
        try {
            int ms = ServerMelodyManager.getMelody(rl).getLength();
            return ms <= 0 ? Optional.empty() : Optional.of(Math.max(20, ms / 50));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public boolean supports(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getItem() instanceof InstrumentItem;
    }

    @Override
    public Optional<ItemStack> getRandomDisc(RandomSource randomSource) {
        return Optional.empty();
    }

    @Override
    public int getMusicDiscSize() {
        return 0;
    }
}
