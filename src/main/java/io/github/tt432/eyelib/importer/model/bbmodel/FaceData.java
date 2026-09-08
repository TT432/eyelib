package io.github.tt432.eyelib.importer.model.bbmodel;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** BBModel 面数据，含 UV 坐标、纹理引用（int 索引或 uuid 字符串）和旋转。
 * box_uv cube 的面可不写 uv（由 uv_offset 展开）；现代 bbmodel 的 texture 可为纹理 uuid 字符串。
 * @author TT432 */
@org.jspecify.annotations.NullMarked
public record FaceData(
        @Nullable Vector4f uv,
        int texture,
        String textureUuid,
        String cullFace,
        int rotation,
        int tint
) {
    public static final Codec<FaceData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BbModelCodecs.FLOATS2VEC4F_CODEC.optionalFieldOf("uv").forGetter(f -> Optional.ofNullable(f.uv())),
            Codec.either(Codec.INT, Codec.STRING)
                    .optionalFieldOf("texture", Either.left(-1))
                    .forGetter(f -> f.textureUuid().isEmpty()
                            ? Either.left(f.texture())
                            : Either.right(f.textureUuid())),
            Codec.STRING.optionalFieldOf("cullface", "").forGetter(FaceData::cullFace),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(FaceData::rotation),
            Codec.INT.optionalFieldOf("tint", 0).forGetter(FaceData::tint)
    ).apply(ins, (uv, texture, cullFace, rotation, tint) -> new FaceData(
            uv.orElse(null),
            texture.map(i -> i, u -> -1),
            texture.map(i -> "", u -> u),
            cullFace,
            rotation,
            tint
    )));
}
