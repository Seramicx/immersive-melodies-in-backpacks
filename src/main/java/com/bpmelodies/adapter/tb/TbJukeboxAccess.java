package com.bpmelodies.adapter.tb;

import com.bpmelodies.common.handler.DiscInv;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxUpgrade;
import com.tiviacz.travelersbackpack.util.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class TbJukeboxAccess implements JukeboxAccess {
    private final JukeboxUpgrade upgrade;
    private final BackpackWrapper wrapper;
    private final Player player;
    private DiscInv discInv;

    private TbJukeboxAccess(JukeboxUpgrade upgrade, BackpackWrapper wrapper, Player player) {
        this.upgrade = upgrade;
        this.wrapper = wrapper;
        this.player = player;
    }

    public JukeboxUpgrade getUpgrade() { return upgrade; }

    public static Optional<JukeboxAccess> find(Player player) {
        if (!(player.containerMenu instanceof BackpackBaseMenu menu)) return Optional.empty();
        BackpackWrapper bw = menu.getWrapper();
        if (bw == null) return Optional.empty();
        for (Optional<UpgradeBase<?>> u : bw.getUpgradeManager().mappedUpgrades.values()) {
            if (u.isPresent() && u.get() instanceof JukeboxUpgrade juke) {
                return Optional.of(new TbJukeboxAccess(juke, bw, player));
            }
        }
        return Optional.empty();
    }

    @Override
    public UUID storageUuid() {
        BlockPos p = blockPos();
        if (p != null) {
            return new UUID((((long) p.getX()) << 32) ^ p.getZ(), p.getY());
        }
        return player.getUUID();
    }

    @Override
    public DiscInv discInventory() {
        if (discInv == null) {
            discInv = new DiscInv() {
                @Override public int getSlots() { return upgrade.diskHandler.getSlots(); }
                @Override public ItemStack getStackInSlot(int slot) { return upgrade.diskHandler.getStackInSlot(slot); }
                @Override public void setStackInSlot(int slot, ItemStack stack) { upgrade.diskHandler.setStackInSlot(slot, stack); }
                @Override public Object raw() { return upgrade.diskHandler; }
            };
        }
        return discInv;
    }

    @Override
    public java.util.List<ItemStack> visibleSlotStacks() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>(upgrade.diskHandler.getSlots());
        for (int i = 0; i < upgrade.diskHandler.getSlots(); i++) {
            out.add(upgrade.diskHandler.getStackInSlot(i));
        }
        return out;
    }

    @Override
    public ItemStack upgradeStack() {
        return upgrade.getDataHolderStack();
    }

    @Override
    @Nullable
    public Entity bearerEntity() {
        return wrapper.getScreenID() == Reference.BLOCK_ENTITY_SCREEN_ID ? null : player;
    }

    @Override
    @Nullable
    public BlockPos blockPos() {
        return wrapper.getScreenID() == Reference.BLOCK_ENTITY_SCREEN_ID ? wrapper.backpackPos : null;
    }

    @Override
    public void markUpgradeDirty() {
        ItemStack live = upgrade.getDataHolderStack();
        if (live.isEmpty()) return;
        upgrade.getUpgradeManager().getUpgradesHandler().setStackInSlot(upgrade.getDataHolderSlot(), live.copy());
    }
}
