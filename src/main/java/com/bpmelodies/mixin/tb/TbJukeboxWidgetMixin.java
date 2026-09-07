package com.bpmelodies.mixin.tb;

import com.bpmelodies.client.gui.MelodyPickerWidget;
import com.bpmelodies.client.gui.TbIconButton;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.network.SetTransportFlagMsg;
import net.neoforged.neoforge.network.PacketDistributor;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.WeakHashMap;

@Mixin(value = JukeboxWidget.class, remap = false)
public abstract class TbJukeboxWidgetMixin {

    @Unique private static final WeakHashMap<JukeboxWidget, MelodyPickerWidget> BPM$PICKERS = new WeakHashMap<>();
    @Unique private static final ResourceLocation BPM$PANEL = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    @Unique
    private MelodyPickerWidget bpm$picker(JukeboxWidget self) {
        int px = self.getPos().x() + (105 - MelodyPickerWidget.W) / 2;
        int py = self.getPos().y() + 63;
        MelodyPickerWidget p = BPM$PICKERS.get(self);
        if (p == null) {
            p = new MelodyPickerWidget(px, py,
                    () -> JukeboxAccess.findJukeboxInOpenMenu(Minecraft.getInstance().player),
                    false);
            BPM$PICKERS.put(self, p);
        } else {
            p.setPosition(px, py);
        }
        return p;
    }

    @Unique
    private void bpm$drawTabPanel(GuiGraphics g, int x, int y) {
        int w = 105, h = 140;
        g.blit(BPM$PANEL, x, y, 4, 4, 0, 0, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x + w - 4, y, 4, 4, 172, 0, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x, y + h - 4, 4, 4, 0, 218, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x + w - 4, y + h - 4, 4, 4, 172, 218, 4, 4, 256, 256);
        g.blit(BPM$PANEL, x + 4, y, w - 8, 4, 80, 0, 1, 4, 256, 256);
        g.blit(BPM$PANEL, x + 4, y + h - 4, w - 8, 4, 80, 218, 1, 4, 256, 256);
        g.blit(BPM$PANEL, x, y + 4, 4, h - 8, 0, 100, 4, 1, 256, 256);
        g.blit(BPM$PANEL, x + w - 4, y + 4, 4, h - 8, 172, 100, 4, 1, 256, 256);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFFC6C6C6);

        int sx = x + 6, sy = y + 22;
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
        g.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
        g.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
        g.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
        g.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);
        g.fill(sx, sy + 17, sx + 1, sy + 18, 0xFF8B8B8B);
        g.fill(sx + 17, sy, sx + 18, sy + 1, 0xFF8B8B8B);
    }

    @Unique
    private boolean bpm$shouldShow(JukeboxWidget self) {
        if (!self.getUpgrade().isTabOpened()) return false;
        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        return JukeboxAccess.isInstrument(inSlot);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void bpm$replaceBg(GuiGraphics g, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        JukeboxWidget self = (JukeboxWidget) (Object) this;
        if (!self.getUpgrade().isTabOpened()) return;
        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        boolean instrument = JukeboxAccess.isInstrument(inSlot);
        bpm$syncWidgetSize(self, instrument ? 105 : 66, instrument ? 140 : 46);
        if (!instrument) return;
        bpm$drawTabPanel(g, self.getPos().x(), self.getPos().y());
        g.renderItem(self.getUpgrade().getUpgradeManager().getUpgradesHandler().getStackInSlot(self.getUpgrade().getDataHolderSlot()),
                self.getPos().x() + 4, self.getPos().y() + 4);
        ci.cancel();
    }

    @Unique private static java.lang.reflect.Field BPM$WIDTH_FIELD;
    @Unique private static java.lang.reflect.Field BPM$HEIGHT_FIELD;

    @Unique
    private void bpm$syncWidgetSize(JukeboxWidget self, int w, int h) {
        try {
            if (BPM$WIDTH_FIELD == null) {
                BPM$WIDTH_FIELD = com.tiviacz.travelersbackpack.client.screens.widgets.WidgetBase.class.getDeclaredField("width");
                BPM$WIDTH_FIELD.setAccessible(true);
                BPM$HEIGHT_FIELD = com.tiviacz.travelersbackpack.client.screens.widgets.WidgetBase.class.getDeclaredField("height");
                BPM$HEIGHT_FIELD.setAccessible(true);
            }
            BPM$WIDTH_FIELD.setInt(self, w);
            BPM$HEIGHT_FIELD.setInt(self, h);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "render", at = @At("RETURN"), remap = true)
    private void bpm$renderPicker(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        JukeboxWidget self = (JukeboxWidget) (Object) this;
        if (!self.getUpgrade().isTabOpened()) return;

        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        boolean instrument = JukeboxAccess.isInstrument(inSlot);
        if (!instrument) return;

        int x = self.getPos().x();
        int y = self.getPos().y();

        ItemStack up = self.getUpgrade().getDataHolderStack();
        boolean shuffle = PlaybackNbt.getShuffle(up);
        PlaybackNbt.RepeatMode rep = PlaybackNbt.getRepeat(up);

        TbIconButton.draw(g, x + 25, y + 22, mouseX, mouseY, TbIconButton.Icon.PREV,    false);
        TbIconButton.draw(g, x + 43, y + 22, mouseX, mouseY, TbIconButton.Icon.STOP,    false);
        TbIconButton.draw(g, x + 61, y + 22, mouseX, mouseY, TbIconButton.Icon.PLAY,    false);
        TbIconButton.draw(g, x + 79, y + 22, mouseX, mouseY, TbIconButton.Icon.NEXT,    false);
        TbIconButton.Icon shuffleIcon = shuffle ? TbIconButton.Icon.SHUFFLE_ON : TbIconButton.Icon.SHUFFLE_OFF;
        TbIconButton.draw(g, x + 43, y + 41, mouseX, mouseY, shuffleIcon, false);
        TbIconButton.Icon repIcon = switch (rep) {
            case NO -> TbIconButton.Icon.REPEAT_OFF;
            case ONE -> TbIconButton.Icon.REPEAT_ONE;
            case ALL -> TbIconButton.Icon.REPEAT_ALL;
        };
        TbIconButton.draw(g, x + 61, y + 41, mouseX, mouseY, repIcon, false);

        bpm$picker(self).render(g, mouseX, mouseY, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void bpm$intercept(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        JukeboxWidget self = (JukeboxWidget) (Object) this;
        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        boolean instrument = JukeboxAccess.isInstrument(inSlot);

        if (instrument && self.getUpgrade().isTabOpened()) {
            MelodyPickerWidget p = bpm$picker(self);
            if (p.isMouseOver(mouseX, mouseY)) {
                if (p.mouseClicked(mouseX, mouseY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        if (!instrument || !self.getUpgrade().isTabOpened()) return;
        int x = self.getPos().x();
        int y = self.getPos().y();

        SetTransportFlagMsg.Op op = null;
        if (bpm$hit(mouseX, mouseY, x + 25, y + 22)) op = SetTransportFlagMsg.Op.PREV;
        else if (bpm$hit(mouseX, mouseY, x + 43, y + 22)) op = SetTransportFlagMsg.Op.STOP;
        else if (bpm$hit(mouseX, mouseY, x + 61, y + 22)) op = SetTransportFlagMsg.Op.PLAY;
        else if (bpm$hit(mouseX, mouseY, x + 79, y + 22)) op = SetTransportFlagMsg.Op.NEXT;
        else if (bpm$hit(mouseX, mouseY, x + 43, y + 41)) op = SetTransportFlagMsg.Op.TOGGLE_SHUFFLE;
        else if (bpm$hit(mouseX, mouseY, x + 61, y + 41)) op = SetTransportFlagMsg.Op.CYCLE_REPEAT;

        if (op != null) {
            PacketDistributor.sendToServer(new SetTransportFlagMsg(op, 0));
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean bpm$hit(double mx, double my, int bx, int by) {
        return mx >= bx && mx < bx + 18 && my >= by && my < by + 18;
    }
}
