package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelib.util.client.model.InventoryModelResourceLocations;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class ModelResourceLocationHelper {
    public static ModelResourceLocation inventory(ResourceLocation id) {
        return InventoryModelResourceLocations.inventory(id);
    }
}
