package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.resources.ResourceLocation;
/**
 * PortResourceLocation ↔ MC ResourceLocation 的双向桥接。
 *
 * @author TT432
 */
public final class ResourceLocationBridge {

    private ResourceLocationBridge() {}

    public static ResourceLocation toMc(PortResourceLocation port) {
        //? if <1.20.6 {
        return new ResourceLocation(port.namespace(), port.path());
        //?} else {
        return ResourceLocation.fromNamespaceAndPath(port.namespace(), port.path());
        //?}
    }

    public static PortResourceLocation fromMc(ResourceLocation mc) {
        return PortResourceLocation.of(mc.getNamespace(), mc.getPath());
    }

    public static ResourceLocation parseMc(String value) {
        //? if <1.20.6 {
        return new ResourceLocation(value);
        //?} else {
        return ResourceLocation.parse(value);

        //?}
    }

    public static ResourceLocation fromParts(String namespace, String path) {
        //? if <1.20.6 {
        return new ResourceLocation(namespace, path);
        //?} else {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);

        //?}
    }
}
