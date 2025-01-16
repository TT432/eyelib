package io.github.tt432.eyelib.client.model.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record BrTextureMesh(
        String texture,
        Vector3f position,
        Vector3f rotation,
        Vector3f local_pivot,
        Vector3f scale
) {
    public static final Codec<BrTextureMesh> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("texture").forGetter(o -> o.texture),
            ExtraCodecs.VECTOR3F.optionalFieldOf("position", new Vector3f()).forGetter(o -> o.position),
            ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(o -> o.rotation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("local_pivot", new Vector3f()).forGetter(o -> o.local_pivot),
            ExtraCodecs.VECTOR3F.optionalFieldOf("scale", new Vector3f(1)).forGetter(o -> o.scale)
    ).apply(ins, BrTextureMesh::new));
}
