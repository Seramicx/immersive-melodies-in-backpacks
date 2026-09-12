package com.bpmelodies.common.handler;

import com.bpmelodies.BpMelodiesMod;
import immersive_melodies.item.InstrumentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public interface JukeboxAccess {
    UUID storageUuid();

    DiscInv discInventory();

    ItemStack upgradeStack();

    @Nullable
    Entity bearerEntity();

    @Nullable
    BlockPos blockPos();

    void markUpgradeDirty();

    default void markDiscSlotDirty(int slot) {
        DiscInv inv = discInventory();
        inv.setStackInSlot(slot, inv.getStackInSlot(slot).copy());
    }

    java.util.List<ItemStack> visibleSlotStacks();

    default int findInstrumentSlot() {
        java.util.List<ItemStack> stacks = visibleSlotStacks();
        for (int i = 0; i < stacks.size(); i++) {
            if (isInstrument(stacks.get(i))) return i;
        }
        return -1;
    }

    @Nullable
    default ItemStack firstInstrumentStack() {
        java.util.List<ItemStack> stacks = visibleSlotStacks();
        for (ItemStack s : stacks) if (isInstrument(s)) return s;
        return null;
    }

    static boolean isInstrument(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof InstrumentItem;
    }

    static boolean isDisc(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    default java.util.List<Integer> discSlots() {
        java.util.List<ItemStack> stacks = visibleSlotStacks();
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            if (isDisc(stacks.get(i))) out.add(i);
        }
        return out;
    }

    default int findDiscSlot() {
        java.util.List<Integer> slots = discSlots();
        return slots.isEmpty() ? -1 : slots.get(0);
    }

    // SB and TB hand discs to their own host-mod jukebox, so only Backpacked opts in here.
    default boolean needsDiscPlayback() {
        return false;
    }

    default boolean useDiscPlayback() {
        return needsDiscPlayback() && (findInstrumentSlot() < 0 || !isImEnabled());
    }

    default boolean isImEnabled() {
        return com.bpmelodies.common.playback.PlaybackNbt.isImEnabled(upgradeStack());
    }

    static Optional<JukeboxAccess> findJukeboxInOpenMenu(Player player) {
        if (BpMelodiesMod.SB_PRESENT) {
            try {
                Optional<JukeboxAccess> sb = com.bpmelodies.adapter.sb.SbJukeboxAccess.find(player);
                if (sb.isPresent()) return sb;
            } catch (Throwable t) {
                BpMelodiesMod.LOGGER.debug("SbJukeboxAccess.find failed", t);
            }
        }
        if (BpMelodiesMod.TB_PRESENT) {
            try {
                Optional<JukeboxAccess> tb = com.bpmelodies.adapter.tb.TbJukeboxAccess.find(player);
                if (tb.isPresent()) return tb;
            } catch (Throwable t) {
                BpMelodiesMod.LOGGER.debug("TbJukeboxAccess.find failed", t);
            }
        }
        if (BpMelodiesMod.BP_PRESENT) {
            try {
                Optional<JukeboxAccess> bp = com.bpmelodies.adapter.bp.BpJukeboxAccess.find(player);
                if (bp.isPresent()) return bp;
            } catch (Throwable t) {
                BpMelodiesMod.LOGGER.debug("BpJukeboxAccess.find failed", t);
            }
        }
        return Optional.empty();
    }
}
