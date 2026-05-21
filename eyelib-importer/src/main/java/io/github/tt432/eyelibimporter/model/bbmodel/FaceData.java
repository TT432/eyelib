package io.github.tt432.eyelibimporter.model.bbmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector4f;

/** BBModel 面数据，含 UV 坐标、纹理索引和旋转。
 * @author TT432 */
@org.jspecify.annotations.NullMarked
/** @author TT432 */
public record FaceData(
        Vector4f uv,
        int texture,
        String cullFace,
        int rotation,
        int tint
) {
    public static final Codec<FaceData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BbModelCodecs.FLOATS2VEC4F_CODEC.fieldOf("uv").forGetter(FaceData::uv),
            Codec.INT.optionalFieldOf("texture", -1).forGetter(FaceData::texture),
            Codec.STRING.optionalFieldOf("cullface", "").forGetter(FaceData::cullFace),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(FaceData::rotation),
            Codec.INT.optionalFieldOf("tint", 0).forGetter(FaceData::tint)
    ).apply(ins, FaceData::new));
}