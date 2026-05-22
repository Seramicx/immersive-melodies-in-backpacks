package com.bpmelodies.mixin.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer$1")
public abstract class JukeboxContainerSlotMixin extends SlotItemHandler {
    public JukeboxContainerSlotMixin(net.minecraftforge.items.IItemHandler handler, int slot, int x, int y) {
        super(handler, slot, x, y);
    }

    @Inject(method = "setChanged", at = @At(
            value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/jukebox/JukeboxUpgradeWrapper;stop(Lnet/minecraft/world/entity/LivingEntity;)V",
            remap = false),
            cancellable = true)
    private void bpm$skipStopForInstrument(CallbackInfo ci) {
        ItemStack disc = getItem();
        if (JukeboxAccess.isInstrument(disc)) {
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[slot-mixin] skipping wrapper.stop on instrument NBT change slot={}", getSlotIndex());
            ci.cancel();
        }
    }

    private static Field BPM$UPGRADE_WRAPPER_FIELD;
    private static Field BPM$STORAGE_WRAPPER_FIELD;
    private static Field BPM$SAVE_HANDLER_FIELD;

    @Inject(method = "setChanged", at = @At("HEAD"))
    private void bpm$autoToggleImBasic(CallbackInfo ci) {
        if (getSlotIndex() != 0) return;
        IItemHandler inv = getItemHandler();
        if (inv.getSlots() != 1) return;
        try {
            JukeboxUpgradeWrapper wrapper = bpm$getWrapper();
            if (wrapper == null) return;
            ItemStack upgradeStack = ((UpgradeWrapperBase<?, ?>) (Object) wrapper).getUpgradeStack();
            boolean wantOn = JukeboxAccess.isInstrument(inv.getStackInSlot(0));
            boolean isOn = PlaybackNbt.isImEnabled(upgradeStack);
            if (wantOn == isOn) return;
            PlaybackNbt.setImEnabled(upgradeStack, wantOn);
            bpm$saveUpgrade(wrapper, upgradeStack);
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[slot-mixin] Basic auto-IM: {} -> {}", isOn, wantOn);
            if (!wantOn) {
                IStorageWrapper sw = bpm$getStorageWrapper(wrapper);
                if (sw != null) sw.getContentsUuid().ifPresent(ServerPlaybackTracker::stop);
            }
        } catch (Throwable t) {
            com.bpmelodies.BpMelodiesMod.LOGGER.warn("[slot-mixin] Basic auto-IM failed", t);
        }
    }

    private JukeboxUpgradeWrapper bpm$getWrapper() throws Exception {
        if (BPM$UPGRADE_WRAPPER_FIELD == null) {
            BPM$UPGRADE_WRAPPER_FIELD = this.getClass().getDeclaredField("val$upgradeWrapper");
            BPM$UPGRADE_WRAPPER_FIELD.setAccessible(true);
        }
        return (JukeboxUpgradeWrapper) BPM$UPGRADE_WRAPPER_FIELD.get(this);
    }

    private IStorageWrapper bpm$getStorageWrapper(JukeboxUpgradeWrapper wrapper) throws Exception {
        if (BPM$STORAGE_WRAPPER_FIELD == null) {
            BPM$STORAGE_WRAPPER_FIELD = UpgradeWrapperBase.class.getDeclaredField("storageWrapper");
            BPM$STORAGE_WRAPPER_FIELD.setAccessible(true);
        }
        return (IStorageWrapper) BPM$STORAGE_WRAPPER_FIELD.get(wrapper);
    }

    @SuppressWarnings("unchecked")
    private void bpm$saveUpgrade(JukeboxUpgradeWrapper wrapper, ItemStack upgradeStack) throws Exception {
        if (BPM$SAVE_HANDLER_FIELD == null) {
            BPM$SAVE_HANDLER_FIELD = UpgradeWrapperBase.class.getDeclaredField("upgradeSaveHandler");
            BPM$SAVE_HANDLER_FIELD.setAccessible(true);
        }
        Consumer<ItemStack> save = (Consumer<ItemStack>) BPM$SAVE_HANDLER_FIELD.get(wrapper);
        save.accept(upgradeStack);
    }
}
