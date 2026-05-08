package io.github.tt432.eyelibimporter.material;

import com.mojang.serialization.Codec;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Importer Bedrock material — delegates CODEC to shared pure-data types.
 */
public record BrMaterial(
        Map<String, BrMaterialEntry> materials
) {

    /**
     * CODEC delegates to the shared pure-data CODEC for serialization,
     * then converts to/from the importer type via xmap.
     */
    public static final Codec<BrMaterial> CODEC = io.github.tt432.eyelibmaterial.shared.BrMaterial.CODEC.xmap(
            BrMaterial::fromShared,
            BrMaterial::toShared
    );

    static BrMaterial fromShared(io.github.tt432.eyelibmaterial.shared.BrMaterial shared) {
        var mats = shared.materials().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BrMaterialEntry.fromShared(e.getValue())
                ));
        return new BrMaterial(mats);
    }

    io.github.tt432.eyelibmaterial.shared.BrMaterial toShared() {
        var sharedMats = materials.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BrMaterialEntry.toShared(e.getValue())
                ));
        return new io.github.tt432.eyelibmaterial.shared.BrMaterial(sharedMats);
    }
}
