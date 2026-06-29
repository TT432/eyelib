//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author TT432
 */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Polygon")
public interface ModelPartPolygonAccessor {
    @Accessor("vertices")
    ModelPart.Vertex[] eyelib$getVertices();

    @Accessor("normal")
    Vector3f eyelib$getNormal();
}
//?}
