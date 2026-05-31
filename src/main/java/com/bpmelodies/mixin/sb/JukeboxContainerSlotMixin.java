package com.bpmelodies.mixin.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer$1", remap = false)
public abstract class JukeboxContainerSlotMixin {

    @Inject(method = "setChanged",
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/jukebox/JukeboxUpgradeWrapper;stop", remap = false),
            cancellable = true, remap = true)
    private void bpm$skipStopForInstrument(CallbackInfo ci) {
        ItemStack disc = ((Slot) (Object) this).getItem();
        if (JukeboxAccess.isInstrument(disc)) {
            ci.cancel();
        }
    }
}
