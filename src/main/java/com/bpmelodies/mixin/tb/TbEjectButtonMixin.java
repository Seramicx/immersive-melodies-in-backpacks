package com.bpmelodies.mixin.tb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UpgradeWidgetBase.class, remap = false)
public abstract class TbEjectButtonMixin {

    @Inject(method = "renderRemoveButton", at = @At("HEAD"), cancellable = true)
    private void bpm$relocateEjectRender(GuiGraphics g, double mx, double my, CallbackInfo ci) {
        Integer ex = bpm$ejectX();
        if (ex == null) return;
        JukeboxWidget jw = (JukeboxWidget) (Object) this;
        g.blit(BackpackScreen.ICONS, ex, jw.getPos().y() + 3, 42, 36, 18, 18);
        ci.cancel();
    }

    @Inject(method = "isMouseOverRemoveButton", at = @At("HEAD"), cancellable = true)
    private void bpm$relocateEjectHitbox(double mx, double my, CallbackInfoReturnable<Boolean> cir) {
        Integer ex = bpm$ejectX();
        if (ex == null) return;
        JukeboxWidget jw = (JukeboxWidget) (Object) this;
        int ey = jw.getPos().y() + 3;
        cir.setReturnValue(mx >= ex && mx < ex + 18 && my >= ey && my < ey + 18);
    }

    private Integer bpm$ejectX() {
        Object self = this;
        if (!(self instanceof JukeboxWidget jw)) return null;
        if (!jw.getUpgrade().isTabOpened()) return null;
        boolean instrument = JukeboxAccess.isInstrument(jw.getUpgrade().diskHandler.getStackInSlot(0));
        int tabW = instrument ? 105 : 66;
        return jw.getPos().x() + tabW - 3 - 18;
    }
}
