package com.bpmelodies.mixin.sb;

import com.bpmelodies.client.gui.SbTabPickerAttacher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JukeboxUpgradeTab.class, remap = false)
public abstract class JukeboxUpgradeTabRenderMixin {
    @Inject(method = "renderBg", at = @At("HEAD"))
    private void bpm$dynamicResize(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci) {
        SbTabPickerAttacher.onRender((JukeboxUpgradeTab) (Object) this);
    }
}
