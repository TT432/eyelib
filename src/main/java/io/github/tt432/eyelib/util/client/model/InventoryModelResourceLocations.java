package io.github.tt432.eyelib.util.client.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryModelResourceLocations {
    public static ModelResourceLocation inventory(ResourceLocation id) {
        return new ModelResourceLocation(id, "inventory");
    }
}
