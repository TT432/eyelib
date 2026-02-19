package io.github.tt432.eyelib.client.model.bbmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import org.joml.Vector4f;

/**
 *
 * @param uv
 * @param texture  null if -1
 * @param cullFace
 * @param rotation
 * @param tint
 */
public record FaceData(
        Vector4f uv,
        int texture,
        String cullFace,
        int rotation,
        int tint
) {
    public static final Codec<FaceData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            EyelibCodec.FLOATS2VEC4F_CODEC.fieldOf("uv").forGetter(FaceData::uv),
            Codec.INT.optionalFieldOf("texture", -1).forGetter(FaceData::texture),
            Codec.STRING.optionalFieldOf("cullface", "").forGetter(FaceData::cullFace),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(FaceData::rotation),
            Codec.INT.optionalFieldOf("tint", 0).forGetter(FaceData::tint)
    ).apply(ins, FaceData::new));
}
