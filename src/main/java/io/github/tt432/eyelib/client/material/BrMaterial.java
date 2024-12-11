package io.github.tt432.eyelib.client.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * @author TT432
 */
public record BrMaterial(
        Map<String, BrMaterialEntry> materials
) {
    public static final Codec<BrMaterial> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.dispatchedMap(Codec.STRING, BrMaterialEntry.CODEC::apply).fieldOf("materials").forGetter(BrMaterial::materials)
    ).apply(ins, BrMaterial::new));
}
