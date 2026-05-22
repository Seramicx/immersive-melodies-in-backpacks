package com.bpmelodies.mixin.tb;

import com.bpmelodies.client.gui.MelodyPickerWidget;
import com.bpmelodies.client.gui.TbIconButton;
import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.network.ModNetwork;
import com.bpmelodies.common.network.SetTransportFlagMsg;
import com.bpmelodies.common.playback.PlaybackNbt;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.WeakHashMap;

/**
 * Inherited methods (isTabOpened, getUpgrade, getPos) are accessed via cast
 * rather than @Shadow because Mixin's preprocessor can't resolve inherited
 * @Shadow members on third-party classes — and that resolution runs even on
 * the integrated server in single-player, which would crash the world load.
 *
 * Button layout mimics SB's jukebox tab:
 *   Row 1 (y+22): prev(27)  stop(45)  play(63)  next(81)
 *   Row 2 (y+41):           shuffle(45)  repeat(63)
 * TB's native play/stop sprites at x+24 and x+42 get masked by our buttons.
 */
@Mixin(value = JukeboxWidget.class, remap = false)
public abstract class TbJukeboxWidgetMixin {

    @Unique private static final int BTN_SIZE = 18;
    // Expanded tab dimensions when instrument inserted (matches TbTabSizeMixin)
    @Unique private static final int TAB_W = 105;
    @Unique private static final int TAB_H = 140;
    // Row Y aligned with slot frame TOP at y+22 (MC convention: frame 1px outside item position y+23)
    @Unique private static final int ROW1_Y = 22;
    @Unique private static final int ROW2_Y = 41;
    // Button X — slot frame at x+6..x+24, transport row starts immediately after
    @Unique private static final int PREV_X    = 25;
    @Unique private static final int STOP_X    = 43;
    @Unique private static final int PLAY_X    = 61;
    @Unique private static final int NEXT_X    = 79;
    @Unique private static final int SHUFFLE_X = 43;
    @Unique private static final int REPEAT_X  = 61;
    @Unique private static final int PICKER_Y  = 63;

    @Unique private static final WeakHashMap<JukeboxWidget, MelodyPickerWidget> BPM$PICKERS = new WeakHashMap<>();

    @Unique
    private MelodyPickerWidget bpm$picker(JukeboxWidget self) {
        int px = self.getPos().x() + (TAB_W - MelodyPickerWidget.W) / 2;
        int py = self.getPos().y() + PICKER_Y;
        MelodyPickerWidget p = BPM$PICKERS.get(self);
        if (p == null) {
            p = new MelodyPickerWidget(px, py,
                    () -> JukeboxAccess.findJukeboxInOpenMenu(Minecraft.getInstance().player),
                    false /* showImToggle — TB has only 1 slot, toggle is redundant */);
            BPM$PICKERS.put(self, p);
        } else {
            p.setPosition(px, py);
        }
        return p;
    }

    /** Render the unified expanded tab panel (replaces TB's stretched 66×46 sprite). */
    @Unique
    private void bpm$drawTabPanel(GuiGraphics g, int x, int y) {
        int x1 = x, y1 = y, x2 = x + TAB_W, y2 = y + TAB_H;
        g.fill(x1, y1, x2, y2, 0xFF373737);                  // outer border
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFFC6C6C6);  // fill
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0xFFFFFFFF);  // top highlight
        g.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, 0xFFFFFFFF);  // left highlight
        g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, 0xFF555555);  // bottom shadow
        g.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, 0xFF555555);  // right shadow

        // Slot frame — vanilla MC style: no outer border, just 1px inset bevel.
        // Top-left dark + bottom-right light = "recessed into panel" appearance.
        int sx = x + 6, sy = y + 22;
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);             // medium gray fill
        g.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);              // top shadow
        g.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);              // left shadow
        g.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);        // bottom highlight
        g.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);        // right highlight
    }

    @Unique
    private boolean bpm$shouldShow(JukeboxWidget self) {
        if (!self.getUpgrade().isTabOpened()) return false;
        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        return JukeboxAccess.isInstrument(inSlot);
    }

    /** Replace TB's stretched bg (because we expanded getTabSize) with our own panel,
     *  drawn BEFORE TB's foreground icon/eject so they still render on top.
     *  Also resets TB's cached width/height when no instrument so TB's native blit
     *  doesn't read past the sprite (which causes the tan-bleed rectangle bug). */
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void bpm$replaceBg(GuiGraphics g, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        JukeboxWidget self = (JukeboxWidget) (Object) this;
        if (!self.getUpgrade().isTabOpened()) return;
        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        boolean instrument = JukeboxAccess.isInstrument(inSlot);
        // Always sync TB's cached width/height with current desired size (since getTabSize
        // only runs at construction, switching instrument in/out leaves stale dimensions).
        bpm$syncWidgetSize(self, instrument ? TAB_W : 66, instrument ? TAB_H : 46);
        if (!instrument) return;
        // Draw our unified panel and the upgrade icon ourselves, then cancel TB's bg blit
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
        // ENTIRE custom layout (buttons + picker) only shows when an IM instrument is in slot
        if (!instrument) return;

        int x = self.getPos().x();
        int y = self.getPos().y();

        ItemStack up = self.getUpgrade().getDataHolderStack();
        boolean shuffle = PlaybackNbt.getShuffle(up);
        PlaybackNbt.RepeatMode rep = PlaybackNbt.getRepeat(up);

        TbIconButton.draw(g, x + PREV_X,    y + ROW1_Y, mouseX, mouseY, TbIconButton.Icon.PREV,    false);
        TbIconButton.draw(g, x + STOP_X,    y + ROW1_Y, mouseX, mouseY, TbIconButton.Icon.STOP,    false);
        TbIconButton.draw(g, x + PLAY_X,    y + ROW1_Y, mouseX, mouseY, TbIconButton.Icon.PLAY,    false);
        TbIconButton.draw(g, x + NEXT_X,    y + ROW1_Y, mouseX, mouseY, TbIconButton.Icon.NEXT,    false);
        // Shuffle: two textures (on/off) — button bg stays neutral
        TbIconButton.Icon shuffleIcon = shuffle ? TbIconButton.Icon.SHUFFLE_ON : TbIconButton.Icon.SHUFFLE_OFF;
        TbIconButton.draw(g, x + SHUFFLE_X, y + ROW2_Y, mouseX, mouseY, shuffleIcon, false);
        TbIconButton.Icon repIcon = switch (rep) {
            case NO -> TbIconButton.Icon.REPEAT_OFF;
            case ONE -> TbIconButton.Icon.REPEAT_ONE;
            case ALL -> TbIconButton.Icon.REPEAT_ALL;
        };
        // Repeat: button bg stays neutral — the 3 icon textures (off/all/one) carry the state visually
        TbIconButton.draw(g, x + REPEAT_X, y + ROW2_Y, mouseX, mouseY, repIcon, false);

        bpm$picker(self).render(g, mouseX, mouseY, partialTick);
    }


    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void bpm$intercept(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        JukeboxWidget self = (JukeboxWidget) (Object) this;
        ItemStack inSlot = self.getUpgrade().diskHandler.getStackInSlot(0);
        boolean instrument = JukeboxAccess.isInstrument(inSlot);

        // Picker click
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
        if (bpm$hit(mouseX, mouseY, x + PREV_X,    y + ROW1_Y)) op = SetTransportFlagMsg.Op.PREV;
        else if (bpm$hit(mouseX, mouseY, x + STOP_X, y + ROW1_Y)) op = SetTransportFlagMsg.Op.STOP;
        else if (bpm$hit(mouseX, mouseY, x + PLAY_X, y + ROW1_Y)) op = SetTransportFlagMsg.Op.PLAY;
        else if (bpm$hit(mouseX, mouseY, x + NEXT_X, y + ROW1_Y)) op = SetTransportFlagMsg.Op.NEXT;
        else if (bpm$hit(mouseX, mouseY, x + SHUFFLE_X, y + ROW2_Y)) op = SetTransportFlagMsg.Op.TOGGLE_SHUFFLE;
        else if (bpm$hit(mouseX, mouseY, x + REPEAT_X,  y + ROW2_Y)) op = SetTransportFlagMsg.Op.CYCLE_REPEAT;

        if (op != null) {
            com.bpmelodies.BpMelodiesMod.LOGGER.info("[tb-click] button={} at mouse=({},{}) widget=({},{})", op, mouseX, mouseY, x, y);
            ModNetwork.CHANNEL.sendToServer(new SetTransportFlagMsg(op, 0));
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean bpm$hit(double mx, double my, int bx, int by) {
        return mx >= bx && mx < bx + BTN_SIZE && my >= by && my < by + BTN_SIZE;
    }
}
