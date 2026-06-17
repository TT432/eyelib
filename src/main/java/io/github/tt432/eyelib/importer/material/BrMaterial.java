package io.github.tt432.eyelib.importer.material;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.stream.Collectors;

/** CODEC 委托给共享纯数据类型的 import 层材料。
 * @author TT432 */
@NullMarked
public record BrMaterial(
        Map<String, BrMaterialEntry> materials
) {

    public static final Codec<BrMaterial> CODEC = io.github.tt432.eyelib.material.shared.BrMaterial.CODEC.xmap(
            BrMaterial::fromShared,
            BrMaterial::toShared
    );

    static BrMaterial fromShared(io.github.tt432.eyelib.material.shared.BrMaterial shared) {
        var mats = shared.materials().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BrMaterialEntry.fromShared(e.getValue())
                ));
        return new BrMaterial(mats);
    }

    io.github.tt432.eyelib.material.shared.BrMaterial toShared() {
        var sharedMats = materials.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BrMaterialEntry.toShared(e.getValue())
                ));
        return new io.github.tt432.eyelib.material.shared.BrMaterial(null, sharedMats);
    }
}