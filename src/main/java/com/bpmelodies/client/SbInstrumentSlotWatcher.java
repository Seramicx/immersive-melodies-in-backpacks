package com.bpmelodies.client;

import com.bpmelodies.BpMelodiesMod;
import com.bpmelodies.common.handler.JukeboxAccess;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public final class SbInstrumentSlotWatcher {
    private SbInstrumentSlotWatcher() {}

    private static WeakReference<StorageContainerMenuBase<?>> lastMenu = new WeakReference<>(null);
    private static Boolean lastHadInstrument = null;
    private static Method onUpgradesChangedMethod;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            tickInner();
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.error("[watcher] tick threw", t);
        }
    }

    private static void tickInner() {
        if (!BpMelodiesMod.SB_PRESENT) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
            lastMenu = new WeakReference<>(null);
            lastHadInstrument = null;
            return;
        }
        JukeboxUpgradeContainer juke = null;
        var allContainers = menu.getUpgradeContainers();
        for (UpgradeContainerBase<?, ?> c : allContainers.values()) {
            if (c instanceof JukeboxUpgradeContainer j) { juke = j; break; }
        }
        if (juke == null) {
            lastHadInstrument = null;
            return;
        }
        boolean has = false;
        for (var s : juke.getSlots()) {
            if (JukeboxAccess.isInstrument(s.getItem())) { has = true; break; }
        }
        StorageContainerMenuBase<?> prev = lastMenu.get();
        boolean menuChanged = (menu != prev);
        boolean stateFlipped = (lastHadInstrument == null || has != lastHadInstrument);

        if (menuChanged) {
            lastMenu = new WeakReference<>(menu);
            lastHadInstrument = has;
            return;
        }
        if (stateFlipped) {
            lastHadInstrument = has;
            triggerUpgradesChanged(menu);
        }
    }

    private static void triggerUpgradesChanged(StorageContainerMenuBase<?> menu) {
        try {
            if (onUpgradesChangedMethod == null) {
                onUpgradesChangedMethod = StorageContainerMenuBase.class.getDeclaredMethod("onUpgradesChanged");
                onUpgradesChangedMethod.setAccessible(true);
            }
            onUpgradesChangedMethod.invoke(menu);
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to trigger upgrades-changed reload", t);
        }
    }
}
