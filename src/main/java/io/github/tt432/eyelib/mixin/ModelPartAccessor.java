//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/** @author TT432 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("children")
    Map<String, ModelPart> eyelib$getChildren();

    @Accessor("cubes")
    List<ModelPart.Cube> eyelib$getCubes();
}
//?}
