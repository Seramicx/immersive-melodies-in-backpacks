package com.bpmelodies.adapter.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class SbJukeboxAccess implements JukeboxAccess {
    private static Field upgradeSaveHandlerField;

    private final JukeboxUpgradeWrapper wrapper;
    private final JukeboxUpgradeContainer container;
    private final IStorageWrapper storageWrapper;
    private final Player player;
    @Nullable private final BlockPos blockPos;

    private SbJukeboxAccess(JukeboxUpgradeWrapper wrapper, JukeboxUpgradeContainer container, IStorageWrapper storageWrapper, Player player, @Nullable BlockPos blockPos) {
        this.wrapper = wrapper;
        this.container = container;
        this.storageWrapper = storageWrapper;
        this.player = player;
        this.blockPos = blockPos;
    }

    public static Optional<JukeboxAccess> find(Player player) {
        if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) return Optional.empty();
        IStorageWrapper sw = menu.getStorageWrapper();
        BlockPos pos = menu.getBlockPosition().orElse(null);
        for (UpgradeContainerBase<?, ?> c : menu.getUpgradeContainers().values()) {
            if (c instanceof JukeboxUpgradeContainer juke) {
                return Optional.of(new SbJukeboxAccess(juke.getUpgradeWrapper(), juke, sw, player, pos));
            }
        }
        return Optional.empty();
    }

    @Override
    public UUID storageUuid() {
        return storageWrapper.getContentsUuid().orElse(new UUID(0L, 0L));
    }

    @Override
    public IItemHandler discInventory() {
        return wrapper.getDiscInventory();
    }

    @Override
    public java.util.List<ItemStack> visibleSlotStacks() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>(container.getSlots().size());
        for (net.minecraft.world.inventory.Slot s : container.getSlots()) {
            out.add(s.getItem());
        }
        return out;
    }

    @Override
    public ItemStack upgradeStack() {
        return wrapper.getUpgradeStack();
    }

    @Override
    @Nullable
    public Entity bearerEntity() {
        return blockPos == null ? player : null;
    }

    @Override
    @Nullable
    public BlockPos blockPos() {
        return blockPos;
    }

    @Override
    public void markUpgradeDirty() {
        try {
            if (upgradeSaveHandlerField == null) {
                upgradeSaveHandlerField = UpgradeWrapperBase.class.getDeclaredField("upgradeSaveHandler");
                upgradeSaveHandlerField.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            Consumer<ItemStack> save = (Consumer<ItemStack>) upgradeSaveHandlerField.get(wrapper);
            save.accept(wrapper.getUpgradeStack());
        } catch (Exception e) {
            com.bpmelodies.BpMelodiesMod.LOGGER.warn("SbJukeboxAccess.markUpgradeDirty failed", e);
        }
    }
}
