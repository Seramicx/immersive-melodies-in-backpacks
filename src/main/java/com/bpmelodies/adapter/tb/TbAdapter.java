package com.bpmelodies.adapter.tb;

public final class TbAdapter {
    private TbAdapter() {}

    public static void init() {
        // All TB integration runs through mixins (gated by TbMixinPlugin)
        // and the TbJukeboxAccess resolver (called from JukeboxAccess.findJukeboxInOpenMenu).
        // No additional registration needed at boot.
    }
}
