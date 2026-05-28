package com.bpmelodies.mixin.tb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.playback.ServerPlaybackTracker;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxUpgrade$2", remap = false)
public class TbDiskHandlerMixin {
    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void bpm$allowInstrument(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (JukeboxAccess.isInstrument(stack)) cir.setReturnValue(true);
    }

    @Inject(method = "onContentsChanged", at = @At("HEAD"))
    private void bpm$stopOnInstrumentRemoved(int slot, CallbackInfo ci) {
        if (slot != 0) return;
        ItemStackHandler self = (ItemStackHandler)(Object)this;
        if (!self.getStackInSlot(slot).isEmpty()) return;
        ServerPlaybackTracker.stopSessionsForTbHandler(self);
    }
}
