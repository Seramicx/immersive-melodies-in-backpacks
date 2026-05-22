package com.bpmelodies.client.gui;

import com.bpmelodies.common.handler.JukeboxAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.Optional;
import java.util.function.Supplier;

public class MelodyPickerSbWidget extends WidgetBase {
    private final MelodyPickerWidget inner;

    public MelodyPickerSbWidget(Position position, Supplier<Optional<JukeboxAccess>> accessProvider) {
        this(position, accessProvider, true);
    }

    public MelodyPickerSbWidget(Position position, Supplier<Optional<JukeboxAccess>> accessProvider, boolean showImToggle) {
        super(position, new Dimension(MelodyPickerWidget.W, showImToggle ? MelodyPickerWidget.H : MelodyPickerWidget.H_NO_TOGGLE));
        this.inner = new MelodyPickerWidget(position.x(), position.y(), accessProvider::get, showImToggle);
    }

    @Override
    public void setPosition(Position position) {
        super.setPosition(position);
        inner.setPosition(position.x(), position.y());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) { }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        inner.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return inner.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return inner.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return inner.charTyped(codePoint, modifiers);
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) { }
}
