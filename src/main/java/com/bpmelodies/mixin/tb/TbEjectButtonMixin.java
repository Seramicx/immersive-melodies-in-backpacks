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

/**
 * The eject (red arrow) button on TB widgets is positioned via {@code removeElement}
 * which is computed at construction-time from the initial {@link UpgradeWidgetBase}'s
 * tab size. When we dynamically expand the jukebox tab via {@link TbTabSizeMixin},
 * the cached position is stale — the eject button stays at the OLD position and
 * overlaps with our transport buttons.
 *
 * This mixin re-routes both the render and hit-test to the right corner of the
 * EXPANDED tab whenever a jukebox tab is open with an instrument inserted.
 */
@Mixin(value = UpgradeWidgetBase.class, remap = false)
public abstract class TbEjectButtonMixin {
    private static final int TAB_W_EXPANDED = 105;
    private static final int TAB_W_NATIVE = 66;
    private static final int EJECT_Y_OFFSET = 3;

    @Inject(method = "renderRemoveButton", at = @At("HEAD"), cancellable = true)
    private void bpm$relocateEjectRender(GuiGraphics g, double mx, double my, CallbackInfo ci) {
        Integer ex = bpm$ejectX();
        if (ex == null) return;
        JukeboxWidget jw = (JukeboxWidget) (Object) this;
        g.blit(BackpackScreen.ICONS, ex, jw.getPos().y() + EJECT_Y_OFFSET, 42, 36, 18, 18);
        ci.cancel();
    }

    @Inject(method = "isMouseOverRemoveButton", at = @At("HEAD"), cancellable = true)
    private void bpm$relocateEjectHitbox(double mx, double my, CallbackInfoReturnable<Boolean> cir) {
        Integer ex = bpm$ejectX();
        if (ex == null) return;
        JukeboxWidget jw = (JukeboxWidget) (Object) this;
        int ey = jw.getPos().y() + EJECT_Y_OFFSET;
        cir.setReturnValue(mx >= ex && mx < ex + 18 && my >= ey && my < ey + 18);
    }

    /** Returns the eject button's CURRENT x position based on whether instrument is in slot.
     *  Always relocates (even no-instrument case) because TB's cached removeElement may be
     *  stale from when the widget was last constructed with a different tab size. */
    private Integer bpm$ejectX() {
        Object self = this;
        if (!(self instanceof JukeboxWidget jw)) return null;
        if (!jw.getUpgrade().isTabOpened()) return null;
        boolean instrument = JukeboxAccess.isInstrument(jw.getUpgrade().diskHandler.getStackInSlot(0));
        int tabW = instrument ? TAB_W_EXPANDED : TAB_W_NATIVE;
        return jw.getPos().x() + tabW - 3 - 18;
    }
}
