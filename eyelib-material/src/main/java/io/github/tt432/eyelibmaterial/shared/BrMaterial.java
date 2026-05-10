package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibutil.codec.DispatchedMapCodec;

import java.util.Map;

/**
 * Pure data record for Bedrock material definitions.
 * <p>
 * No GL/LWJGL/MC dependencies — suitable for platform-free serialization.
 *
 * @author TT432
 */
public record BrMaterial(Map<String, BrMaterialEntry> materials) {
    public static final Codec<BrMaterial> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            new DispatchedMapCodec<>(Codec.STRING, BrMaterialEntry.CODEC::apply)
                    .fieldOf("materials")
                    .forGetter(BrMaterial::materials)
    ).apply(ins, BrMaterial::new));
}
