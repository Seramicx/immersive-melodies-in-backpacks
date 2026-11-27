package com.bpmelodies.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

public final class PanelArea {

    @Nullable private static Screen owner;
    @Nullable private static Rect2i area;

    private PanelArea() {}

    public static void set(Screen screen, int x, int y, int w, int h) {
        owner = screen;
        area = new Rect2i(x, y, w, h);
    }

    public static void clear(Screen screen) {
        if (owner == screen) area = null;
    }

    @Nullable
    public static Rect2i forScreen(@Nullable Screen screen) {
        return screen != null && screen == owner ? area : null;
    }
}
