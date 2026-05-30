package com.bpmelodies.common.handler;

import net.minecraft.world.item.ItemStack;

public interface DiscInv {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    void setStackInSlot(int slot, ItemStack stack);

    Object raw();
}
