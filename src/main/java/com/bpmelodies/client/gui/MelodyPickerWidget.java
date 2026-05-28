package com.bpmelodies.client.gui;

import com.bpmelodies.common.handler.JukeboxAccess;
import com.bpmelodies.common.network.RequestMelodyListMsg;
import com.bpmelodies.common.network.SetSelectedMelodyMsg;
import com.bpmelodies.common.network.ToggleImModeMsg;
import net.neoforged.neoforge.network.PacketDistributor;
import com.bpmelodies.common.playback.PlaybackNbt;
import immersive_melodies.resources.ClientMelodyManager;
import immersive_melodies.resources.MelodyDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MelodyPickerWidget {
    public static final int W = 72;
    public static final int ROW_H = 11;
    public static final int PAGE_SIZE = 5;
    public static final int BTN_H = 11;
    public static final int PAGINATION_H = BTN_H;
    public static final int TOGGLE_H = 15;
    public static final int H = TOGGLE_H + 1 + PAGE_SIZE * ROW_H + PAGINATION_H + 2;
    public static final int H_NO_TOGGLE = PAGE_SIZE * ROW_H + PAGINATION_H + 2;

    public interface JukeboxAccessProvider {
        Optional<JukeboxAccess> get();
    }

    private int x;
    private int y;
    private final JukeboxAccessProvider provider;
    private final EditBox pageBox;
    private final boolean showImToggle;
    private int currentPage = 1;
    private long lastRequestMs = 0L;

    public MelodyPickerWidget(int x, int y, JukeboxAccessProvider provider) {
        this(x, y, provider, true);
    }

    public MelodyPickerWidget(int x, int y, JukeboxAccessProvider provider, boolean showImToggle) {
        this.x = x;
        this.y = y;
        this.provider = provider;
        this.showImToggle = showImToggle;
        Font font = Minecraft.getInstance().font;
        this.pageBox = new EditBox(font, x + 25, y + listYOffset() + PAGE_SIZE * ROW_H + 2, 22, BTN_H + 1, Component.literal("p"));
        this.pageBox.setMaxLength(3);
        this.pageBox.setBordered(true);
        this.pageBox.setVisible(true);
        this.pageBox.setValue(String.valueOf(currentPage));
        this.pageBox.moveCursorToStart(false);
    }

    private int listYOffset() { return showImToggle ? (TOGGLE_H + 1) : 0; }

    public int getEffectiveHeight() { return showImToggle ? H : H_NO_TOGGLE; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return W; }
    public int getHeight() { return H; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        pageBox.setX(x + 25);
        pageBox.setY(y + listYOffset() + PAGE_SIZE * ROW_H + 2);
    }

    private List<Map.Entry<ResourceLocation, MelodyDescriptor>> sortedMelodies() {
        Map<ResourceLocation, MelodyDescriptor> all = ClientMelodyManager.getMelodiesList();
        List<Map.Entry<ResourceLocation, MelodyDescriptor>> list = new ArrayList<>(all.entrySet());
        String playerName = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getName().getString();
        list.sort((a, b) -> {
            int sa = imSortIndex(a.getKey(), playerName);
            int sb = imSortIndex(b.getKey(), playerName);
            if (sa != sb) return sb - sa;
            return a.getKey().toString().compareTo(b.getKey().toString());
        });
        return list;
    }

    private static int imSortIndex(ResourceLocation rl, @javax.annotation.Nullable String playerName) {
        if ("player".equals(rl.getNamespace())) {
            if (playerName != null && rl.getPath().startsWith(playerName + "/")) return 2;
            return 0;
        }
        return 1;
    }

    private int totalPages(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private ResourceLocation currentSelection() {
        return provider.get()
                .map(a -> {
                    ResourceLocation live = com.bpmelodies.client.playback.ClientPlaybackTracker.getCurrentMelody(a.storageUuid());
                    if (live != null) return live;
                    int s = a.findInstrumentSlot();
                    if (s < 0) return (ResourceLocation) null;
                    return PlaybackNbt.getSelectedMelody(a.visibleSlotStacks().get(s));
                })
                .orElse(null);
    }

    private boolean hasInstrument() {
        return provider.get().map(a -> a.findInstrumentSlot() >= 0).orElse(false);
    }

    private boolean isImEnabled() {
        return provider.get().map(JukeboxAccess::isImEnabled).orElse(true);
    }

    public boolean isMouseOver(double mx, double my) {
        if (!hasInstrument()) return false;
        return mx >= x && mx < x + W && my >= y && my < y + getEffectiveHeight();
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!hasInstrument()) return;

        Font font = Minecraft.getInstance().font;
        boolean enabled = isImEnabled();

        if (showImToggle) {
            drawToggle(g, mouseX, mouseY, enabled);
            if (!enabled) return;
        }

        if (ClientMelodyManager.getMelodiesList().isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastRequestMs > 1000L) {
                lastRequestMs = now;
                PacketDistributor.sendToServer(new RequestMelodyListMsg());
            }
        }

        int listY = y + listYOffset();
        g.fill(x, listY, x + W, listY + PAGE_SIZE * ROW_H, 0xFF373737);

        List<Map.Entry<ResourceLocation, MelodyDescriptor>> all = sortedMelodies();
        int total = all.size();
        int totalPages = totalPages(total);
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        ResourceLocation selected = currentSelection();
        int from = (currentPage - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = from + i;
            int rowY = listY + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + W && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (idx < total) {
                boolean isSel = all.get(idx).getKey().equals(selected);
                int bg = isSel ? 0xFF3B6BB0 : (hover ? 0xFF505050 : 0);
                if (bg != 0) g.fill(x, rowY, x + W, rowY + ROW_H, bg);
                String name = all.get(idx).getValue().getName();
                String text = (isSel ? "▶" : " ") + name;
                if (font.width(text) > W - 4) {
                    while (font.width(text + "…") > W - 4 && text.length() > 2) {
                        text = text.substring(0, text.length() - 1);
                    }
                    text += "…";
                }
                g.drawString(font, text, x + 2, rowY + 2, 0xFFFFFF, false);
            }
        }
        if (total == 0) {
            String hint = "(no songs)";
            int tx = x + (W - font.width(hint)) / 2;
            g.drawString(font, hint, tx, listY + 2 * ROW_H + 2, 0xAAAAAA, false);
        }

        int pagY = listY + PAGE_SIZE * ROW_H + 2;
        drawPageBtn(g, mouseX, mouseY, x + 0,  pagY, 12, "«", currentPage > 1);
        drawPageBtn(g, mouseX, mouseY, x + 12, pagY, 12, "‹", currentPage > 1);
        pageBox.render(g, mouseX, mouseY, partialTick);
        drawPageBtn(g, mouseX, mouseY, x + 48, pagY, 12, "›", currentPage < totalPages);
        drawPageBtn(g, mouseX, mouseY, x + 60, pagY, 12, "»", currentPage < totalPages);
    }

    private void drawToggle(GuiGraphics g, int mx, int my, boolean enabled) {
        boolean hover = mx >= x && mx < x + W && my >= y && my < y + TOGGLE_H;
        int border = 0xFF373737;
        int fill, highlight, shadow, textColor;
        if (enabled) {
            fill      = hover ? 0xFF5688D0 : 0xFF3B6BB0;
            highlight = hover ? 0xFF7AA8E0 : 0xFF5F8DCE;
            shadow    = hover ? 0xFF2A5599 : 0xFF234380;
            textColor = 0xFFFFFFFF;
        } else {
            fill      = hover ? 0xFFD8D8D8 : 0xFFC6C6C6;
            highlight = hover ? 0xFFEDEDED : 0xFFDADADA;
            shadow    = hover ? 0xFFA8A8A8 : 0xFF8B8B8B;
            textColor = 0xFF202020;
        }
        g.fill(x, y, x + W, y + TOGGLE_H, border);
        g.fill(x + 1, y + 1, x + W - 1, y + TOGGLE_H - 1, fill);
        g.fill(x + 1, y + 1, x + W - 1, y + 2, highlight);
        g.fill(x + 1, y + 1, x + 2, y + TOGGLE_H - 1, highlight);
        g.fill(x + 1, y + TOGGLE_H - 2, x + W - 1, y + TOGGLE_H - 1, shadow);
        g.fill(x + W - 2, y + 1, x + W - 1, y + TOGGLE_H - 1, shadow);
        String label = enabled ? "IM ON" : "IM OFF";
        Font f = Minecraft.getInstance().font;
        g.drawString(f, label, x + (W - f.width(label)) / 2, y + 3, textColor, false);
    }

    private void drawPageBtn(GuiGraphics g, int mx, int my, int bx, int by, int w, String label, boolean enabled) {
        int h = BTN_H;
        boolean hover = enabled && mx >= bx && mx < bx + w && my >= by && my < by + h;
        int border = 0xFF373737;
        int fill = enabled ? (hover ? 0xFFD8D8D8 : 0xFFC6C6C6) : 0xFF8B8B8B;
        int highlight = enabled ? 0xFFEDEDED : 0xFFA0A0A0;
        int shadow = enabled ? 0xFF8B8B8B : 0xFF555555;
        int textColor = enabled ? 0xFF202020 : 0xFF555555;
        g.fill(bx, by, bx + w, by + h, border);
        g.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, fill);
        g.fill(bx + 1, by + 1, bx + w - 1, by + 2, highlight);
        g.fill(bx + 1, by + 1, bx + 2, by + h - 1, highlight);
        g.fill(bx + 1, by + h - 2, bx + w - 1, by + h - 1, shadow);
        g.fill(bx + w - 2, by + 1, bx + w - 1, by + h - 1, shadow);
        Font f = Minecraft.getInstance().font;
        int xOff = (w - f.width(label)) / 2;
        if ("›".equals(label) || "»".equals(label) || "«".equals(label)) xOff += 1;
        g.drawString(f, label, bx + xOff, by + 2, textColor, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hasInstrument()) return false;
        if (button != 0) return false;

        if (showImToggle) {
            if (mouseX >= x && mouseX < x + W && mouseY >= y && mouseY < y + TOGGLE_H) {
                boolean newEnabled = !isImEnabled();
                PacketDistributor.sendToServer(new ToggleImModeMsg(newEnabled));
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (!isImEnabled()) return false;
        }

        if (pageBox.isFocused() && pageBox.mouseClicked(mouseX, mouseY, button)) return true;
        if (pageBox.isMouseOver(mouseX, mouseY)) {
            pageBox.setFocused(true);
            return true;
        } else {
            commitPageBox();
            pageBox.setFocused(false);
        }

        List<Map.Entry<ResourceLocation, MelodyDescriptor>> all = sortedMelodies();
        int total = all.size();
        int totalPages = totalPages(total);

        int listY = y + listYOffset();
        int from = (currentPage - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = from + i;
            if (idx >= total) break;
            int rowY = listY + i * ROW_H;
            if (mouseX >= x && mouseX < x + W && mouseY >= rowY && mouseY < rowY + ROW_H) {
                PacketDistributor.sendToServer(new SetSelectedMelodyMsg(all.get(idx).getKey()));
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        int pagY = listY + PAGE_SIZE * ROW_H + 2;
        if (mouseY >= pagY && mouseY < pagY + BTN_H) {
            if (mouseX >= x + 0  && mouseX < x + 12 && currentPage > 1)          { currentPage = 1; pageBox.setValue(String.valueOf(currentPage)); pageBox.moveCursorToStart(false); return true; }
            if (mouseX >= x + 12 && mouseX < x + 24 && currentPage > 1)          { currentPage--;   pageBox.setValue(String.valueOf(currentPage)); pageBox.moveCursorToStart(false); return true; }
            if (mouseX >= x + 48 && mouseX < x + 60 && currentPage < totalPages) { currentPage++;   pageBox.setValue(String.valueOf(currentPage)); pageBox.moveCursorToStart(false); return true; }
            if (mouseX >= x + 60 && mouseX < x + 72 && currentPage < totalPages) { currentPage = totalPages; pageBox.setValue(String.valueOf(currentPage)); pageBox.moveCursorToStart(false); return true; }
        }

        return false;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (pageBox.isFocused()) {
            if (key == 257) { commitPageBox(); pageBox.setFocused(false); return true; }
            return pageBox.keyPressed(key, scan, mods);
        }
        return false;
    }

    public boolean charTyped(char c, int mods) {
        if (pageBox.isFocused()) return pageBox.charTyped(c, mods);
        return false;
    }

    private void commitPageBox() {
        try {
            int v = Integer.parseInt(pageBox.getValue().trim());
            int total = totalPages(sortedMelodies().size());
            currentPage = Math.max(1, Math.min(total, v));
        } catch (NumberFormatException ignored) {}
        pageBox.setValue(String.valueOf(currentPage));
        pageBox.moveCursorToStart(false);
    }
}
