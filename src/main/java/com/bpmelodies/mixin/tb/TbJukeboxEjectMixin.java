package com.bpmelodies.mixin.tb;

import com.bpmelodies.common.playback.ServerPlaybackTracker;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxUpgrade;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JukeboxUpgrade.class, remap = false)
public class TbJukeboxEjectMixin {
    @Inject(method = "onUpgradeRemoved(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void bpm$stopOnEject(ItemStack removedStack, CallbackInfo ci) {
        ServerPlaybackTracker.stopSessionsForTbUpgrade(this);
    }
}
