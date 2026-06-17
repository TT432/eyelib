package io.github.tt432.eyelibtrack;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * eyelib-track 模块常量。
 *
 * @author TT432
 */
public final class EyelibTrack {
    public static final String MOD_ID = "eyelibtrack";
    public static final String TRACK_ID_KEY = "eyelib_track_id";

    private EyelibTrack() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}