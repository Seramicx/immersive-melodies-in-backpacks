package com.bpmelodies.mixin.sb;

import com.bpmelodies.common.handler.JukeboxAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SB's JukeboxUpgradeContainer's inner Slot has:
 *   public void setChanged() {
 *       super.setChanged();
 *       if (wrapper.isPlaying() && getSlotIndex() == wrapper.getDiscSlotActive()) {
 *           wrapper.stop(player);
 *       }
 *   }
 * Intent: if the user swaps the disc, stop playback. Side effect: when WE mutate
 * the instrument disc's NBT (selected_melody) mid-playback, this fires
 * wrapper.stop and kills our session. Skip the stop entirely when the disc is
 * an instrument — we manage transitions ourselves.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer$1")
public abstract class JukeboxContainerSlotMixin extends SlotItemHandler {
    public JukeboxContainerSlotMixin(net.minecraftforge.items.IItemHandler handler, int slot, int x, int y) {
        super(handler, slot, x, y);
    }

    // setChanged() is MC-defined → mixin remaps to SRG (m_6654_).
    // The INVOKE target references SC's wrapper.stop — no remap needed for that, but the
    // descriptor's LivingEntity class is mojmap at runtime so literal works.
    @Inject(method = "setChanged", at = @At(
            value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/jukebox/JukeboxUpgradeWrapper;stop(Lnet/minecraft/world/entity/LivingEntity;)V",
            remap = false),
            cancellable = true)
    private void bpm$skipStopForInstrument(CallbackInfo ci) {
        ItemStack disc = getItem();
        if (JukeboxAccess.isInstrument(disc)) {
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[slot-mixin] skipping wrapper.stop on instrument NBT change slot={}", getSlotIndex());
            ci.cancel();
        }
    }
}
