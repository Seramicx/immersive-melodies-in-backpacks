package com.bpmelodies.mixin.tb;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.tiviacz.travelersbackpack.inventory.menu.slot.UpgradeSlotItemHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets the anonymous {@code UpgradeSlotItemHandler} subclass declared inside
 * {@code JukeboxUpgrade.getUpgradeSlots(...)} (compiles to {@code JukeboxUpgrade$1}).
 * For Instrument stacks, bypasses TB's IS_PLAYING flag check.
 */
@Mixin(targets = "com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxUpgrade$1")
public class TbDiskSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void bpm$allowInstrumentPlace(ItemStack pStack, CallbackInfoReturnable<Boolean> cir) {
        if (JukeboxAccess.isInstrument(pStack)) {
            UpgradeSlotItemHandler<?> self = (UpgradeSlotItemHandler<?>) (Object) this;
            cir.setReturnValue(self.getUpgradeParent().isTabOpened());
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void bpm$allowInstrumentPickup(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (JukeboxAccess.isInstrument(self.getItem())) cir.setReturnValue(true);
    }
}
