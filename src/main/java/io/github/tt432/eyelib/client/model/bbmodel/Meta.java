package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Meta(
        @SerializedName("format_version")
        String formatVersion,
        @SerializedName("model_format")
        String modelFormat,
        @SerializedName("box_uv")
        boolean boxUv
) {
    public static final Codec<Meta> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(Meta::formatVersion),
            Codec.STRING.fieldOf("model_format").forGetter(Meta::modelFormat),
            Codec.BOOL.fieldOf("box_uv").forGetter(Meta::boxUv)
    ).apply(ins, Meta::new));
}
