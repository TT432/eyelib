package io.github.tt432.eyelib.util.resource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * 提供 ResourceLocation 的创建与空值判断工具方法。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceLocations {
    public static final ResourceLocation EMPTY = ResourceLocations.of("__blank");

    public static boolean isEmpty(ResourceLocation location) {
        return EMPTY.equals(location);
    }

    public static ResourceLocation of(String namespace, String path) {
        //? if <1.20.6 {
        return new ResourceLocation(namespace, path);
        //?} else {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?}
    }

    public static ResourceLocation of(String value) {
        //? if <1.20.6 {
        return new ResourceLocation(value);
        //?} else {
        return ResourceLocation.parse(value);
        //?}
    }
}
