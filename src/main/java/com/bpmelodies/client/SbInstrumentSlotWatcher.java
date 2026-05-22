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
    private static int tickCount = 0;
    private static String lastEmitted = "";

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCount++;
        try {
            tickInner();
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.error("[watcher] tick #{} threw", tickCount, t);
        }
    }

    private static void tickInner() {
        if (!BpMelodiesMod.SB_PRESENT) { emit("SB_ABSENT"); return; }
        var player = Minecraft.getInstance().player;
        if (player == null) { emit("NO_PLAYER"); return; }
        if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
            emit("MENU=" + player.containerMenu.getClass().getSimpleName() + " (not SB)");
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
            emit("MENU=" + menu.getClass().getSimpleName() + " containers=" + allContainers.size() + " no-juke");
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

        emit(String.format("menuId=%d prevId=%s menuChanged=%s has=%s lastHad=%s flipped=%s jukeOpen=%s",
                System.identityHashCode(menu),
                prev == null ? "null" : String.valueOf(System.identityHashCode(prev)),
                menuChanged, has, lastHadInstrument, stateFlipped, juke.isOpen()));

        if (menuChanged) {
            BpMelodiesMod.LOGGER.info("[watcher] >>> menu-changed branch entering");
            lastMenu = new WeakReference<>(menu);
            lastHadInstrument = has;
            return;
        }
        if (stateFlipped) {
            BpMelodiesMod.LOGGER.info("[watcher] >>> state-flipped branch entering: {} -> {}", lastHadInstrument, has);
            lastHadInstrument = has;
            triggerUpgradesChanged(menu);
        }
    }

    private static void emit(String msg) {
        if (!msg.equals(lastEmitted)) {
            lastEmitted = msg;
            BpMelodiesMod.LOGGER.info("[watcher] tick #{} {}", tickCount, msg);
        }
    }

    private static void triggerUpgradesChanged(StorageContainerMenuBase<?> menu) {
        try {
            if (onUpgradesChangedMethod == null) {
                onUpgradesChangedMethod = StorageContainerMenuBase.class.getDeclaredMethod("onUpgradesChanged");
                onUpgradesChangedMethod.setAccessible(true);
            }
            onUpgradesChangedMethod.invoke(menu);
            BpMelodiesMod.LOGGER.info("[watcher] onUpgradesChanged invoked successfully");
        } catch (Throwable t) {
            BpMelodiesMod.LOGGER.warn("[bpmelodies] Failed to trigger upgrades-changed reload", t);
        }
    }
}
