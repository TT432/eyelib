package io.github.tt432.eyelibimporter.model.bbmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.With;

@With
public record Faces(
        FaceData north,
        FaceData east,
        FaceData south,
        FaceData west,
        FaceData up,
        FaceData down
) {
    public static final Codec<Faces> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            FaceData.CODEC.fieldOf("north").forGetter(Faces::north),
            FaceData.CODEC.fieldOf("east").forGetter(Faces::east),
            FaceData.CODEC.fieldOf("south").forGetter(Faces::south),
            FaceData.CODEC.fieldOf("west").forGetter(Faces::west),
            FaceData.CODEC.fieldOf("up").forGetter(Faces::up),
            FaceData.CODEC.fieldOf("down").forGetter(Faces::down)
    ).apply(ins, Faces::new));
}
