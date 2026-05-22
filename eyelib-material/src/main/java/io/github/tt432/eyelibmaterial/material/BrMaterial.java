package io.github.tt432.eyelibmaterial.material;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运行时Bedrock材质记录。CODEC委托给shared.BrMaterial.CODEC进行序列化。
 *
 * @author TT432
 */
@NullMarked
public record BrMaterial(
        Map<String, BrMaterialEntry> materials
) {
    /**
     * CODEC delegates to the shared pure-data CODEC for serialization,
     * then converts to/from the runtime type via xmap.
     */
    public static final Codec<BrMaterial> CODEC = io.github.tt432.eyelibmaterial.shared.BrMaterial.CODEC.xmap(
            BrMaterial::fromShared,
            BrMaterial::toShared
    );

    public static BrMaterial fromShared(io.github.tt432.eyelibmaterial.shared.BrMaterial shared) {
        var materials = shared.materials().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BrMaterialEntry.fromShared(e.getValue())
                ));
        return new BrMaterial(materials);
    }

    io.github.tt432.eyelibmaterial.shared.BrMaterial toShared() {
        var sharedMaterials = materials.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toShared()
                ));
        return new io.github.tt432.eyelibmaterial.shared.BrMaterial(sharedMaterials);
    }
}