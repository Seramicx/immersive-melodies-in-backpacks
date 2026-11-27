package com.bpmelodies.compat.jei;

import com.bpmelodies.client.PanelArea;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;

// keeps JEI's ingredient list off the jukebox panel drawn beside Backpacked screens
public class BpJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("bpmelodies", "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                Rect2i area = PanelArea.forScreen(Minecraft.getInstance().screen);
                return area == null ? List.of() : List.of(area);
            }
        });
    }
}
