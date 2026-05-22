package com.bpmelodies.mixin.sb;

import com.bpmelodies.client.gui.SbTabPickerAttacher;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class JukeboxUpgradeTabMixin {

    private JukeboxUpgradeTabMixin() {}

    @Mixin(value = JukeboxUpgradeTab.Basic.class, remap = false)
    public static abstract class BasicMixin {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void bpm$attach(JukeboxUpgradeContainer container, Position position, StorageScreenBase<?> screen, CallbackInfo ci) {
            SbTabPickerAttacher.attach((JukeboxUpgradeTab.Basic) (Object) this, true);
        }
    }

    @Mixin(value = JukeboxUpgradeTab.Advanced.class, remap = false)
    public static abstract class AdvancedMixin {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void bpm$attach(JukeboxUpgradeContainer container, Position position, StorageScreenBase<?> screen, int slotsInRow, CallbackInfo ci) {
            SbTabPickerAttacher.attach((JukeboxUpgradeTab.Advanced) (Object) this, false);
        }
    }
}
