package io.github.tt432.eyelibimporter.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;

import java.util.Map;

public record BrMaterial(
        Map<String, BrMaterialEntry> materials
) {
    public static final Codec<BrMaterial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ImporterCodecUtil.dispatchedMap(BrMaterialEntry.CODEC::apply)
                    .fieldOf("materials")
                    .forGetter(BrMaterial::materials)
    ).apply(instance, BrMaterial::new));
}
