package io.github.tt432.eyelibutil.resource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceLocations {
    public static final ResourceLocation EMPTY = ResourceLocations.of("__blank");

    public static boolean isEmpty(ResourceLocation location) {
        return EMPTY.equals(location);
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation of(String value) {
        return new ResourceLocation(value);
    }
}