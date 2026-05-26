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
    private static final WeakHashMap<JukeboxUpgradeTab, Boolean> HIDE_TOGGLE = new WeakHashMap<>();
    public static void attach(JukeboxUpgradeTab tab, boolean hideToggle) {
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
                    () -> JukeboxAccess.findJukeboxInOpenMenu(Minecraft.getInstance().player),
                    !hideToggle);
            addHideableChildMethod.invoke(tab, picker);

            Dimension after = (Dimension) openTabDimensionField.get(tab);
            int hToggleOnly = hideToggle ? 0 : (yOffset + MelodyPickerWidget.TOGGLE_H + 6);
            int hFull = after.height();
            int wWith = after.width();
            SIZES.put(tab, new int[]{wWithout, hWithout, wWith, hToggleOnly, hFull});
            HIDE_TOGGLE.put(tab, hideToggle);
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to attach MelodyPicker to SB jukebox tab", t);
        }
    }

    public static void onRender(JukeboxUpgradeTab tab) {
        int[] sizes = SIZES.get(tab);
        if (sizes == null) return;
        JukeboxUpgradeContainer container = getContainer(tab);
        if (container == null || !container.isOpen()) return;
        boolean hasInst = false;
        for (net.minecraft.world.inventory.Slot s : container.getSlots()) {
            if (JukeboxAccess.isInstrument(s.getItem())) { hasInst = true; break; }
        }
        boolean hideToggle = Boolean.TRUE.equals(HIDE_TOGGLE.get(tab));

        int wantW, wantH;
        if (!hasInst) {
            wantW = sizes[0]; wantH = sizes[1];
        } else if (hideToggle) {
            wantW = sizes[2]; wantH = sizes[4];
        } else {
            boolean imEnabled = com.bpmelodies.common.playback.PlaybackNbt.isImEnabled(container.getUpgradeWrapper().getUpgradeStack());
            if (!imEnabled) { wantW = sizes[2]; wantH = sizes[3]; }
            else            { wantW = sizes[2]; wantH = sizes[4]; }
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
