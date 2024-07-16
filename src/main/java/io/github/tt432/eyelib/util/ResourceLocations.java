package io.github.tt432.eyelib.util;

import io.github.tt432.eyelib.Eyelib;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceLocations {
    public static final ResourceLocation EMPTY = ResourceLocations.of("__blank");

    public static boolean isEmpty(ResourceLocation location) {
        return EMPTY.equals(location);
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation of(String value) {
        return ResourceLocation.parse(value);
    }

    public static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, path);
    }
}
