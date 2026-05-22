package com.bpmelodies.common.handler;

import com.bpmelodies.BpMelodiesMod;
import immersive_melodies.item.InstrumentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public interface JukeboxAccess {
    UUID storageUuid();

    IItemHandler discInventory();

    ItemStack upgradeStack();

    @Nullable
    Entity bearerEntity();

    @Nullable
    BlockPos blockPos();

    void markUpgradeDirty();

    default void markDiscSlotDirty(int slot) {
        IItemHandler inv = discInventory();
        if (inv instanceof IItemHandlerModifiable mod) {
            mod.setStackInSlot(slot, inv.getStackInSlot(slot).copy());
        }
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
        return Optional.empty();
    }
}
