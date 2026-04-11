package io.github.tt432.eyelibimporter.model.bbmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Resolution(
        int width,
        int height
) {
    public static final Codec<Resolution> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("width").forGetter(Resolution::width),
            Codec.INT.fieldOf("height").forGetter(Resolution::height)
    ).apply(ins, Resolution::new));
}
