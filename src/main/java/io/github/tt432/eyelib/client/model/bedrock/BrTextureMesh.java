package io.github.tt432.eyelib.client.model.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AllArgsConstructor;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

/**
 * TODO
 *
 * @author TT432
 */
@AllArgsConstructor
public class BrTextureMesh {
    public static final Codec<BrTextureMesh> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("texture").forGetter(o -> o.texture),
            ExtraCodecs.VECTOR3F.fieldOf("position").forGetter(o -> o.position),
            ExtraCodecs.VECTOR3F.fieldOf("rotation").forGetter(o -> o.rotation),
            ExtraCodecs.VECTOR3F.fieldOf("local_pivot").forGetter(o -> o.local_pivot),
            ExtraCodecs.VECTOR3F.fieldOf("scale").forGetter(o -> o.scale)
    ).apply(ins, BrTextureMesh::new));
    String texture;
    Vector3f position;
    Vector3f rotation;
    Vector3f local_pivot;
    Vector3f scale;
}
