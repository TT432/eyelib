//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** @author TT432 */
@Mixin(ModelPart.Cube.class)
public interface ModelPartCubeAccessor {
    /**
     * 1.21.1 中 polygons 字段类型 {@code ModelPart.Polygon[]} 的元素类变为 package-private，
     * 因此返回 {@code Object[]}（数组协变安全），由调用方逐元素 cast。
     */
    @Accessor("polygons")
    Object[] eyelib$getPolygons();
}
//?}
