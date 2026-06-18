package io.github.tt432.eyelib.track;

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
        //? if <1.20.6 {
        return new ResourceLocation(MOD_ID, path);
        //?} else {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?}
    }
}
