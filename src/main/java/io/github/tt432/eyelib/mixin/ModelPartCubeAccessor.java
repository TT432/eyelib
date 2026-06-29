//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** @author TT432 */
@Mixin(ModelPart.Cube.class)
public interface ModelPartCubeAccessor {
    @Accessor("polygons")
    ModelPart.Polygon[] eyelib$getPolygons();
}
//?}
