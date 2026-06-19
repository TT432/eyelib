//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.21.1 中 {@code ModelPart.Polygon} 变为 package-private，
 * 用 targets 字符串目标避免编译期引用限制。
 *
 * @author TT432
 */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Polygon")
public interface ModelPartPolygonAccessor {
    @Accessor("vertices")
    Object[] eyelib$getVertices();

    @Accessor("normal")
    Vector3f eyelib$getNormal();
}
//?}
