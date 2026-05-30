package com.bpmelodies.mixin.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper$1", remap = false)
public abstract class JukeboxDiscInventoryMixin {

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void bpm$allowInstrument(int slot, ItemVariant resource, int count, CallbackInfoReturnable<Boolean> cir) {
        if (resource.getItem() instanceof immersive_melodies.item.InstrumentItem) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onContentsChanged", at = @At("RETURN"))
    private void bpm$autoToggleIm(int slot, CallbackInfo ci) {
        try {
            JukeboxUpgradeWrapper wrapper = bpm$wrapper();
            ItemStackHandler self = (ItemStackHandler) (Object) this;
            ItemStack upgradeStack = ((UpgradeWrapperBase<?, ?>) (Object) wrapper).getUpgradeStack();
            boolean wantOn = JukeboxAccess.isInstrument(self.getStackInSlot(0));
            boolean isOn = PlaybackNbt.isImEnabled(upgradeStack);
            if (wantOn == isOn) return;
            PlaybackNbt.setImEnabled(upgradeStack, wantOn);
            bpm$save(wrapper, upgradeStack);
            if (!wantOn) {
                IStorageWrapper sw = bpm$storageWrapper(wrapper);
                if (sw != null) sw.getContentsUuid().ifPresent(ServerPlaybackTracker::stop);
            }
        } catch (Throwable t) {
            com.bpmelodies.BpMelodiesMod.LOGGER.warn("[disc-inv-mixin] auto-IM failed", t);
        }
    }

    private static Field BPM$THIS0;
    private static Field BPM$SAVE_HANDLER;
    private static Field BPM$STORAGE_WRAPPER;

    private JukeboxUpgradeWrapper bpm$wrapper() throws Exception {
        if (BPM$THIS0 == null) {
            BPM$THIS0 = this.getClass().getDeclaredField("this$0");
            BPM$THIS0.setAccessible(true);
        }
        return (JukeboxUpgradeWrapper) BPM$THIS0.get(this);
    }

    @SuppressWarnings("unchecked")
    private void bpm$save(JukeboxUpgradeWrapper wrapper, ItemStack upgradeStack) throws Exception {
        if (BPM$SAVE_HANDLER == null) {
            BPM$SAVE_HANDLER = UpgradeWrapperBase.class.getDeclaredField("upgradeSaveHandler");
            BPM$SAVE_HANDLER.setAccessible(true);
        }
        ((Consumer<ItemStack>) BPM$SAVE_HANDLER.get(wrapper)).accept(upgradeStack);
    }

    private IStorageWrapper bpm$storageWrapper(JukeboxUpgradeWrapper wrapper) throws Exception {
        if (BPM$STORAGE_WRAPPER == null) {
            BPM$STORAGE_WRAPPER = UpgradeWrapperBase.class.getDeclaredField("storageWrapper");
            BPM$STORAGE_WRAPPER.setAccessible(true);
        }
        return (IStorageWrapper) BPM$STORAGE_WRAPPER.get(wrapper);
    }
}
