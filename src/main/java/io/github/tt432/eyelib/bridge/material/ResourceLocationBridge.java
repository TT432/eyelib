package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;

/**
 * PortResourceLocation ↔ MC ResourceLocation 的双向桥接。
 *
 * @author TT432
 */
@NullMarked
public final class ResourceLocationBridge {

    private ResourceLocationBridge() {}

    public static ResourceLocation toMc(PortResourceLocation port) {
        return new ResourceLocation(port.namespace(), port.path());
    }

    public static PortResourceLocation fromMc(ResourceLocation mc) {
        return PortResourceLocation.of(mc.getNamespace(), mc.getPath());
    }
}
