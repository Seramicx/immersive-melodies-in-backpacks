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

/**
 * Outer holder for the two inner-class mixins. The actual attach logic lives
 * in {@code com.bpmelodies.client.gui.SbTabPickerAttacher} (non-mixin package),
 * because Mixin's sealed-package rule forbids injected bytecode from
 * referencing classes that live in a defined mixin package.
 */
public final class JukeboxUpgradeTabMixin {

    private JukeboxUpgradeTabMixin() {}

    @Mixin(value = JukeboxUpgradeTab.Basic.class, remap = false)
    public static abstract class BasicMixin {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void bpm$attach(JukeboxUpgradeContainer container, Position position, StorageScreenBase<?> screen, CallbackInfo ci) {
            SbTabPickerAttacher.attach((JukeboxUpgradeTab.Basic) (Object) this);
        }
    }

    @Mixin(value = JukeboxUpgradeTab.Advanced.class, remap = false)
    public static abstract class AdvancedMixin {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void bpm$attach(JukeboxUpgradeContainer container, Position position, StorageScreenBase<?> screen, int slotsInRow, CallbackInfo ci) {
            SbTabPickerAttacher.attach((JukeboxUpgradeTab.Advanced) (Object) this);
        }
    }
}
