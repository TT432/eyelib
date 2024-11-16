package io.github.tt432.eyelib.client.model.bake;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@UtilityClass
public class BakedModels {
    private final Map<ResourceLocation, Map<ResourceLocation, BakedModel>> cache = new HashMap<>();
}
