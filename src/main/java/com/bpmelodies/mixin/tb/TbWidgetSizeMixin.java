package com.bpmelodies.mixin.tb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UpgradeWidgetBase.class, remap = false)
public abstract class TbWidgetSizeMixin {

    @Inject(method = "getWidgetSizeAndPos", at = @At("RETURN"), cancellable = true)
    private void bpm$extendBoundsForPicker(CallbackInfoReturnable<int[]> cir) {
        Object self = this;
        if (!(self instanceof JukeboxWidget jw)) return;
        if (!jw.getUpgrade().isTabOpened()) return;
        if (!JukeboxAccess.isInstrument(jw.getUpgrade().diskHandler.getStackInSlot(0))) return;
        int[] orig = cir.getReturnValue();
        if (orig == null || orig.length < 4) return;
        cir.setReturnValue(new int[]{orig[0], orig[1], 105, 134});
    }
}
