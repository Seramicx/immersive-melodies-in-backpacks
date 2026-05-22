package com.bpmelodies.common.playback;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class PlaybackNbt {
    public static final String SELECTED_MELODY = "bpmelodies:selected_melody";
    public static final String SHUFFLE = "bpmelodies:shuffle";
    public static final String REPEAT = "bpmelodies:repeat";
    public static final String IM_ENABLED = "bpmelodies:im_enabled";

    /** IM-mode toggle on the jukebox upgrade stack. Default ON. When OFF the
     *  picker hides, slot-change auto-reload is suppressed, and the disc
     *  handler short-circuits so SB treats the instrument as inert. */
    public static boolean isImEnabled(ItemStack upgradeStack) {
        if (upgradeStack.isEmpty()) return true;
        CompoundTag tag = upgradeStack.getTag();
        if (tag == null || !tag.contains(IM_ENABLED)) return true;
        return tag.getBoolean(IM_ENABLED);
    }

    public static void setImEnabled(ItemStack upgradeStack, boolean enabled) {
        if (upgradeStack.isEmpty()) return;
        upgradeStack.getOrCreateTag().putBoolean(IM_ENABLED, enabled);
    }

    private PlaybackNbt() {}

    @Nullable
    public static ResourceLocation getSelectedMelody(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SELECTED_MELODY)) return null;
        try {
            return new ResourceLocation(tag.getString(SELECTED_MELODY));
        } catch (Exception e) {
            return null;
        }
    }

    public static void setSelectedMelody(ItemStack stack, @Nullable ResourceLocation rl) {
        if (stack.isEmpty()) return;
        if (rl == null) {
            if (stack.getTag() != null) stack.getTag().remove(SELECTED_MELODY);
        } else {
            stack.getOrCreateTag().putString(SELECTED_MELODY, rl.toString());
        }
    }

    public enum RepeatMode {
        NO, ONE, ALL;

        public RepeatMode next() {
            return switch (this) {
                case NO -> ALL;
                case ALL -> ONE;
                case ONE -> NO;
            };
        }
    }

    public static boolean getShuffle(ItemStack upgradeStack) {
        if (upgradeStack.isEmpty()) return false;
        CompoundTag tag = upgradeStack.getTag();
        return tag != null && tag.getBoolean(SHUFFLE);
    }

    public static void setShuffle(ItemStack upgradeStack, boolean shuffle) {
        if (upgradeStack.isEmpty()) return;
        upgradeStack.getOrCreateTag().putBoolean(SHUFFLE, shuffle);
    }

    public static RepeatMode getRepeat(ItemStack upgradeStack) {
        if (upgradeStack.isEmpty()) return RepeatMode.NO;
        CompoundTag tag = upgradeStack.getTag();
        if (tag == null || !tag.contains(REPEAT)) return RepeatMode.NO;
        try {
            return RepeatMode.valueOf(tag.getString(REPEAT));
        } catch (IllegalArgumentException e) {
            return RepeatMode.NO;
        }
    }

    public static void setRepeat(ItemStack upgradeStack, RepeatMode mode) {
        if (upgradeStack.isEmpty()) return;
        upgradeStack.getOrCreateTag().putString(REPEAT, mode.name());
    }
}
