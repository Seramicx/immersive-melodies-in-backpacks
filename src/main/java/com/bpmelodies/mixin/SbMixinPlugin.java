package com.bpmelodies.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SbMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled = false;

    @Override public void onLoad(String mixinPackage) {
        enabled = LoadingModList.get().getModFileById("sophisticatedcore") != null;
    }

    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String s, org.objectweb.asm.tree.ClassNode c, String n, IMixinInfo i) {}
    @Override public void postApply(String s, org.objectweb.asm.tree.ClassNode c, String n, IMixinInfo i) {}
}
