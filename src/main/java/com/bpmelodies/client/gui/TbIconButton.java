package com.bpmelodies.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class TbIconButton {
    public enum Icon { PREV, STOP, PLAY, NEXT, SHUFFLE_OFF, SHUFFLE_ON, REPEAT_OFF, REPEAT_ONE, REPEAT_ALL }

    public static final int SIZE = 18;
    private static final ResourceLocation SHUFFLE_OFF_TEX = new ResourceLocation("bpmelodies", "textures/gui/shuffle_off.png");
    private static final ResourceLocation SHUFFLE_ON_TEX = new ResourceLocation("bpmelodies", "textures/gui/shuffle_on.png");
    private static final ResourceLocation REPEAT_OFF_TEX = new ResourceLocation("bpmelodies", "textures/gui/repeat_off.png");
    private static final ResourceLocation REPEAT_ALL_TEX = new ResourceLocation("bpmelodies", "textures/gui/repeat_all.png");
    private static final ResourceLocation REPEAT_ONE_TEX = new ResourceLocation("bpmelodies", "textures/gui/repeat_one.png");

    private TbIconButton() {}

    public static void draw(GuiGraphics g, int bx, int by, int mx, int my, Icon icon, boolean active) {
        int w = SIZE, h = SIZE;
        boolean hover = mx >= bx && mx < bx + w && my >= by && my < by + h;
        int border = 0xFF373737;
        int fill, highlight, shadow, iconColor;
        if (active) {
            fill      = hover ? 0xFF5688D0 : 0xFF3B6BB0;
            highlight = hover ? 0xFF7AA8E0 : 0xFF5F8DCE;
            shadow    = hover ? 0xFF2A5599 : 0xFF234380;
            iconColor = 0xFFFFFFFF;
        } else {
            fill      = hover ? 0xFFD8D8D8 : 0xFFC6C6C6;
            highlight = hover ? 0xFFEDEDED : 0xFFDADADA;
            shadow    = hover ? 0xFFA8A8A8 : 0xFF8B8B8B;
            iconColor = 0xFF202020;
        }
        g.fill(bx, by, bx + w, by + h, border);
        g.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, fill);
        g.fill(bx + 1, by + 1, bx + w - 1, by + 2, highlight);
        g.fill(bx + 1, by + 1, bx + 2, by + h - 1, highlight);
        g.fill(bx + 1, by + h - 2, bx + w - 1, by + h - 1, shadow);
        g.fill(bx + w - 2, by + 1, bx + w - 1, by + h - 1, shadow);
        int cx = bx + 9, cy = by + 9;
        switch (icon) {
            case PLAY -> triangleRight(g, cx, cy, iconColor);
            case STOP -> g.fill(cx - 3, cy - 3, cx + 3, cy + 3, iconColor);
            case PREV -> {
                g.fill(cx - 4, cy - 4, cx - 3, cy + 4, iconColor);
                triangleLeft(g, cx + 1, cy, iconColor);
            }
            case NEXT -> {
                triangleRight(g, cx - 1, cy, iconColor);
                g.fill(cx + 3, cy - 4, cx + 4, cy + 4, iconColor);
            }
            case SHUFFLE_OFF -> blitIcon(g, SHUFFLE_OFF_TEX, bx, by, 0xFFFFFFFF);
            case SHUFFLE_ON  -> blitIcon(g, SHUFFLE_ON_TEX,  bx, by, 0xFFFFFFFF);
            case REPEAT_OFF -> blitIcon(g, REPEAT_OFF_TEX, bx, by, 0xFFFFFFFF);
            case REPEAT_ALL -> blitIcon(g, REPEAT_ALL_TEX, bx, by, 0xFFFFFFFF);
            case REPEAT_ONE -> blitIcon(g, REPEAT_ONE_TEX, bx, by, 0xFFFFFFFF);
        }
    }

    private static void triangleRight(GuiGraphics g, int cx, int cy, int color) {
        for (int i = 0; i < 5; i++) {
            int halfH = 4 - i;
            g.fill(cx - 2 + i, cy - halfH, cx - 1 + i, cy + halfH + 1, color);
        }
    }

    private static void triangleLeft(GuiGraphics g, int cx, int cy, int color) {
        for (int i = 0; i < 5; i++) {
            int halfH = 4 - i;
            g.fill(cx + 2 - i, cy - halfH, cx + 3 - i, cy + halfH + 1, color);
        }
    }

    private static void blitIcon(GuiGraphics g, ResourceLocation tex, int bx, int by, int tintARGB) {
        float a = ((tintARGB >> 24) & 0xFF) / 255.0F;
        float r = ((tintARGB >> 16) & 0xFF) / 255.0F;
        float gr = ((tintARGB >> 8) & 0xFF) / 255.0F;
        float b = (tintARGB & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, gr, b, a);
        g.blit(tex, bx + 1, by + 1, 16, 16, 0, 0, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static void repeatOneOverlay(GuiGraphics g, int cx, int cy, int color) {
        Font f = Minecraft.getInstance().font;
        int tw = f.width("1");
        g.drawString(f, "1", cx - tw / 2, cy - 3, color, false);
    }
}
