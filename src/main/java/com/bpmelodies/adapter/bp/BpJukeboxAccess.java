package com.bpmelodies.adapter.bp;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BpJukeboxAccess implements JukeboxAccess {
    private final Player owner;
    private final Container contents;
    private final IItemHandler handler;
    private final UUID uuid;

    private BpJukeboxAccess(Player owner, int index, Container contents) {
        this.owner = owner;
        this.contents = contents;
        this.handler = new InvWrapper(contents);
        this.uuid = UUID.nameUUIDFromBytes(("bpmelodies:backpacked:" + owner.getUUID() + ":" + index)
                .getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<JukeboxAccess> find(Player player) {
        if (!(player.containerMenu instanceof BackpackContainerMenu menu)) return Optional.empty();
        Entity found = player.level().getEntity(menu.getOwnerId());
        if (!(found instanceof Player backpackOwner)) return Optional.empty();
        return Optional.of(new BpJukeboxAccess(backpackOwner, menu.getBackpackIndex(), menu.getBackpackInventory()));
    }

    @Override
    public UUID storageUuid() {
        return uuid;
    }

    @Override
    public IItemHandler discInventory() {
        return handler;
    }

    @Override
    public List<ItemStack> visibleSlotStacks() {
        List<ItemStack> out = new ArrayList<>(contents.getContainerSize());
        for (int i = 0; i < contents.getContainerSize(); i++) {
            out.add(contents.getItem(i));
        }
        return out;
    }

    // The worn backpack stack is never synced to clients, so state rides on the instrument instead.
    @Override
    public ItemStack upgradeStack() {
        int slot = findInstrumentSlot();
        return slot < 0 ? ItemStack.EMPTY : contents.getItem(slot);
    }

    @Override
    @Nullable
    public Entity bearerEntity() {
        return owner;
    }

    @Override
    @Nullable
    public BlockPos blockPos() {
        return null;
    }

    @Override
    public void markUpgradeDirty() {
        int slot = findInstrumentSlot();
        if (slot >= 0) markDiscSlotDirty(slot);
    }
}
