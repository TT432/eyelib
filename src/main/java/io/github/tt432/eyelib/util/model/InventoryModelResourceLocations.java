package io.github.tt432.eyelibutil.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * 提供带"inventory"变体的 ModelResourceLocation 工厂方法。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryModelResourceLocations {
    public static ModelResourceLocation inventory(ResourceLocation id) {
        return new ModelResourceLocation(id, "inventory");
    }
}