package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibutil.codec.DispatchedMapCodec;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Bedrock材质定义的纯数据记录。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public record BrMaterial(Map<String, BrMaterialEntry> materials) {
    public static final Codec<BrMaterial> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            new DispatchedMapCodec<>(Codec.STRING, BrMaterialEntry.CODEC::apply)
                    .fieldOf("materials")
                    .forGetter(BrMaterial::materials)
    ).apply(ins, BrMaterial::new));
}