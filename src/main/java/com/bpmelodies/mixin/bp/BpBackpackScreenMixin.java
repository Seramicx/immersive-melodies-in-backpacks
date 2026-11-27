package com.bpmelodies.mixin.bp;

import com.bpmelodies.client.gui.MelodyPickerWidget;
import com.bpmelodies.client.gui.TbIconButton;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.network.SetTransportFlagMsg;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.mrcrayfish.backpacked.client.gui.screen.inventory.UnlockableContainerScreen;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(value = UnlockableContainerScreen.class, remap = false)
public abstract class BpBackpackScreenMixin {

    @Unique private static final ResourceLocation BPM$PANEL = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    @Unique private static final int BPM$W = 84;
    // clears the tab strip Backpacked draws down the right edge of the window
    @Unique private static final int BPM$GAP = 20;

    @Unique private MelodyPickerWidget bpm$picker;

    @Unique
    private Optional<JukeboxAccess> bpm$access() {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!(screen.getMenu() instanceof BackpackContainerMenu)) return Optional.empty();
        return JukeboxAccess.findJukeboxInOpenMenu(Minecraft.getInstance().player);
    }

    @Unique
    private MelodyPickerWidget bpm$picker(int x, int y) {
        if (bpm$picker == null) {
            bpm$picker = new MelodyPickerWidget(x, y,
                    () -> JukeboxAccess.findJukeboxInOpenMenu(Minecraft.getInstance().player),
                    true);
        } else {
            bpm$picker.setPosition(x, y);
        }
        return bpm$picker;
    }

    @Inject(method = "render", at = @At("RETURN"), remap = true)
    private void bpm$renderPanel(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Optional<JukeboxAccess> found = bpm$access();
        if (found.isEmpty()) return;
        JukeboxAccess access = found.get();
        ItemStack instrument = access.firstInstrumentStack();
        int discSlot = access.findDiscSlot();
        if (instrument == null && discSlot < 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int x = screen.getGuiLeft() + screen.getXSize() + BPM$GAP;
        int y = screen.getGuiTop();
        MelodyPickerWidget p = instrument == null ? null : bpm$picker(x + (BPM$W - MelodyPickerWidget.W) / 2, y + 46);
        bpm$drawPanel(g, x, y, p == null ? 49 : 46 + p.getEffectiveHeight() + 6);

        g.renderItem(instrument != null ? instrument : access.discInventory().getStackInSlot(discSlot), x + 6, y + 6);

        TbIconButton.draw(g, x + 24, y + 6, mouseX, mouseY, TbIconButton.Icon.PREV, false);
        TbIconButton.draw(g, x + 42, y + 6, mouseX, mouseY, TbIconButton.Icon.STOP, false);
        TbIconButton.draw(g, x + 60, y + 6, mouseX, mouseY, TbIconButton.Icon.PLAY, false);
        TbIconButton.draw(g, x + 24, y + 25, mouseX, mouseY, TbIconButton.Icon.NEXT, false);

        ItemStack up = access.upgradeStack();
        boolean shuffle = PlaybackNbt.getShuffle(up);
        TbIconButton.Icon repIcon = switch (PlaybackNbt.getRepeat(up)) {
            case NO -> TbIconButton.Icon.REPEAT_OFF;
            case ONE -> TbIconButton.Icon.REPEAT_ONE;
            case ALL -> TbIconButton.Icon.REPEAT_ALL;
        };
        TbIconButton.draw(g, x + 42, y + 25, mouseX, mouseY,
                shuffle ? TbIconButton.Icon.SHUFFLE_ON : TbIconButton.Icon.SHUFFLE_OFF, false);
        TbIconButton.draw(g, x + 60, y + 25, mouseX, mouseY, repIcon, false);

        if (p != null) p.render(g, mouseX, mouseY, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void bpm$intercept(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Optional<JukeboxAccess> found = bpm$access();
        if (found.isEmpty()) return;
        JukeboxAccess access = found.get();
        boolean hasInstrument = access.firstInstrumentStack() != null;
        if (!hasInstrument && access.findDiscSlot() < 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int x = screen.getGuiLeft() + screen.getXSize() + BPM$GAP;
        int y = screen.getGuiTop();

        if (hasInstrument) {
            MelodyPickerWidget p = bpm$picker(x + (BPM$W - MelodyPickerWidget.W) / 2, y + 46);
            if (p.isMouseOver(mouseX, mouseY)) {
                p.mouseClicked(mouseX, mouseY, button);
                cir.setReturnValue(true);
                return;
            }
        }

        SetTransportFlagMsg.Op op = null;
        if (bpm$hit(mouseX, mouseY, x + 24, y + 6)) op = SetTransportFlagMsg.Op.PREV;
        else if (bpm$hit(mouseX, mouseY, x + 42, y + 6)) op = SetTransportFlagMsg.Op.STOP;
        else if (bpm$hit(mouseX, mouseY, x + 60, y + 6)) op = SetTransportFlagMsg.Op.PLAY;
        else if (bpm$hit(mouseX, mouseY, x + 24, y + 25)) op = SetTransportFlagMsg.Op.NEXT;
        else if (bpm$hit(mouseX, mouseY, x + 42, y + 25)) op = SetTransportFlagMsg.Op.TOGGLE_SHUFFLE;
        else if (bpm$hit(mouseX, mouseY, x + 60, y + 25)) op = SetTransportFlagMsg.Op.CYCLE_REPEAT;

        if (op != null) {
            PacketDistributor.sendToServer(new SetTransportFlagMsg(op, 0));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void bpm$drawPanel(GuiGraphics g, int x, int y, int h) {
        int w = BPM$W;
        g.blit(BPM$PANEL, x, y, 4, 4, 0, 0, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x + w - 4, y, 4, 4, 172, 0, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x, y + h - 4, 4, 4, 0, 218, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x + w - 4, y + h - 4, 4, 4, 172, 218, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x + 4, y, w - 8, 4, 80, 0, 1, 4, 256, 256);
        g.blit(BPM$PANEL, x + 4, y + h - 4, w - 8, 4, 80, 218, 1, 4, 256, 256);
        g.blit(BPM$PANEL, x, y + 4, 4, h - 8, 0, 100, 4, 1, 256, 256);
        g.blit(BPM$PANEL, x + w - 4, y + 4, 4, h - 8, 172, 100, 4, 1, 256, 256);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFFC6C6C6);
    }

    @Unique
    private static boolean bpm$hit(double mx, double my, int bx, int by) {
        return mx >= bx && mx < bx + 18 && my >= by && my < by + 18;
    }
}
