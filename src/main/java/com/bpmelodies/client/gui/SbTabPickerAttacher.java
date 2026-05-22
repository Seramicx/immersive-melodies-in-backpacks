package com.bpmelodies.client.gui;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeTab;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

public final class SbTabPickerAttacher {
    private SbTabPickerAttacher() {}

    private static Method addHideableChildMethod;
    private static Field openTabDimensionField;
    private static Method getContainerMethod;
    private static final WeakHashMap<JukeboxUpgradeTab, int[]> SIZES = new WeakHashMap<>();
    // [widthWithout, heightWithout, widthWith, heightToggleOnly, heightFull]
    //  - heightToggleOnly = tab with instrument + IM off (just the toggle visible)
    //  - heightFull       = tab with instrument + IM on  (full picker)

    public static void attach(JukeboxUpgradeTab tab) {
        try {
            Class<?> base = Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabBase");
            if (addHideableChildMethod == null) {
                addHideableChildMethod = base.getDeclaredMethod("addHideableChild", WidgetBase.class);
                addHideableChildMethod.setAccessible(true);
            }
            if (openTabDimensionField == null) {
                openTabDimensionField = base.getDeclaredField("openTabDimension");
                openTabDimensionField.setAccessible(true);
            }
            Dimension before = (Dimension) openTabDimensionField.get(tab);
            int wWithout = before.width();
            int hWithout = before.height();

            int yOffset = Math.max(60, hWithout) + 4;
            MelodyPickerSbWidget picker = new MelodyPickerSbWidget(
                    new Position(tab.getX() + 3, tab.getY() + yOffset),
                    () -> JukeboxAccess.findJukeboxInOpenMenu(Minecraft.getInstance().player));
            addHideableChildMethod.invoke(tab, picker);

            Dimension after = (Dimension) openTabDimensionField.get(tab);
            // hToggleOnly = tab y → picker y + toggle height + 6 bottom border
            int hToggleOnly = yOffset + MelodyPickerWidget.TOGGLE_H + 6;
            int hFull = after.height();
            int wWith = after.width();
            SIZES.put(tab, new int[]{wWithout, hWithout, wWith, hToggleOnly, hFull});
            BpMelodiesMod.LOGGER.info("[attacher] attached picker to tab id={} cls={} pos=({},{}) sizes empty=[{},{}] toggleOnly=[{},{}] full=[{},{}]",
                    System.identityHashCode(tab), tab.getClass().getSimpleName(),
                    tab.getX(), tab.getY(), wWithout, hWithout, wWith, hToggleOnly, wWith, hFull);
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to attach MelodyPicker to SB jukebox tab", t);
        }
    }

    /** Per-frame dynamic resize based on slot contents. Reads via slots
     *  (not wrapper.discInventory) because the client-side wrapper can be
     *  desynced from the visible slot list. */
    public static void onRender(JukeboxUpgradeTab tab) {
        int[] sizes = SIZES.get(tab);
        if (sizes == null) return;
        JukeboxUpgradeContainer container = getContainer(tab);
        if (container == null || !container.isOpen()) return;
        boolean hasInst = false;
        for (net.minecraft.world.inventory.Slot s : container.getSlots()) {
            if (JukeboxAccess.isInstrument(s.getItem())) { hasInst = true; break; }
        }
        boolean imEnabled = com.bpmelodies.common.playback.PlaybackNbt.isImEnabled(container.getUpgradeWrapper().getUpgradeStack());

        int wantW, wantH;
        if (!hasInst) {
            wantW = sizes[0]; wantH = sizes[1];                // baseline
        } else if (!imEnabled) {
            wantW = sizes[2]; wantH = sizes[3];                // instrument + IM off → toggle only
        } else {
            wantW = sizes[2]; wantH = sizes[4];                // instrument + IM on → full
        }
        if (tab.getWidth() != wantW) tab.setWidth(wantW);
        if (tab.getHeight() != wantH) tab.setHeight(wantH);
    }

    static JukeboxUpgradeContainer getContainer(JukeboxUpgradeTab tab) {
        try {
            if (getContainerMethod == null) {
                Class<?> c = Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab");
                getContainerMethod = c.getDeclaredMethod("getContainer");
                getContainerMethod.setAccessible(true);
            }
            return (JukeboxUpgradeContainer) getContainerMethod.invoke(tab);
        } catch (Throwable t) { return null; }
    }
}
