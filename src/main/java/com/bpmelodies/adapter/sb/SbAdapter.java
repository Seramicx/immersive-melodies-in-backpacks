package com.bpmelodies.adapter.sb;

import com.bpmelodies.common.handler.ImInstrumentDiscHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.DiscHandlerRegistry;

public final class SbAdapter {
    private SbAdapter() {}

    public static void init() {
        DiscHandlerRegistry.registerHandler(new ImInstrumentDiscHandler());
    }
}
