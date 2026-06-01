package io.github.tt432.eyelib.client.model;

import net.minecraft.client.model.geom.ModelPart;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
@NullMarked
public interface RootModelPartModel {
    @Nullable ModelPart getRootPart();
}