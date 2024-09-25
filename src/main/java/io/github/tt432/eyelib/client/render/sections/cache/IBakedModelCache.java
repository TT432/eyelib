package io.github.tt432.eyelib.client.render.sections.cache;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.BakedModel;

/**
 * @author Argon4W
 */
public interface IBakedModelCache {
    BakedModel getTransformedModel(Transformation transformation);
    int size();
}
