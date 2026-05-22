package com.bpmelodies.mixin.tb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxUpgrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UpgradeBase.class, remap = false)
public abstract class TbTabSizeMixin {
    @Inject(method = "getTabSize", at = @At("RETURN"), cancellable = true)
    private void bpm$enlargeForJukeboxWithInstrument(CallbackInfoReturnable<Point> cir) {
        Object self = this;
        if (!(self instanceof JukeboxUpgrade ju)) return;
        if (!ju.isTabOpened()) return;
        if (!JukeboxAccess.isInstrument(ju.diskHandler.getStackInSlot(0))) return;
        cir.setReturnValue(new Point(105, 140));
    }
}
