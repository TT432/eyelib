package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public final class MaterialAssetRegistry {
    public static void replaceMaterials(Map<?, BrMaterial> materials) {
        LinkedHashMap<String, BrMaterialEntry> flattened = new LinkedHashMap<>();
        for (BrMaterial value : materials.values()) {
            value.materials().forEach(flattened::put);
        }
        MaterialManager.writePort().replaceAll(flattened);
    }

    public static void publishMaterial(BrMaterial material) {
        material.materials().forEach(MaterialManager.writePort()::put);
    }
}
