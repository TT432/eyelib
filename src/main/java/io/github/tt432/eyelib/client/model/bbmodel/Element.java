package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import lombok.With;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

@With
public record Element(
        String name,

        @SerializedName("box_uv")
        boolean boxUv,

        @SerializedName("render_order")
        String renderOrder,

        boolean locked,

        @SerializedName("allow_mirror_modeling")
        boolean allowMirrorModeling,

        Vector3f from,
        Vector3f to,
        int autouv,
        int color,
        Vector3f origin,

        @SerializedName("uv_offset")
        Vector2f uvOffset,

        double inflate,

        Faces faces,
        String type,
        String uuid,
        Vector3f rotation
) {
    public static final Codec<Element> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Element::name),
            Codec.BOOL.fieldOf("box_uv").forGetter(Element::boxUv),
            Codec.STRING.fieldOf("render_order").forGetter(Element::renderOrder),
            Codec.BOOL.fieldOf("locked").forGetter(Element::locked),
            Codec.BOOL.fieldOf("allow_mirror_modeling").forGetter(Element::allowMirrorModeling),
            EyelibCodec.FLOATS2VEC3F_CODEC.fieldOf("from").forGetter(Element::from),
            EyelibCodec.FLOATS2VEC3F_CODEC.fieldOf("to").forGetter(Element::to),
            Codec.INT.fieldOf("autouv").forGetter(Element::autouv),
            Codec.INT.fieldOf("color").forGetter(Element::color),
            EyelibCodec.FLOATS2VEC3F_CODEC.fieldOf("origin").forGetter(Element::origin),
            EyelibCodec.FLOATS2VEC2F_CODEC.optionalFieldOf("uv_offset", new Vector2f()).forGetter(Element::uvOffset),
            Codec.DOUBLE.optionalFieldOf("inflate", 0D).forGetter(Element::inflate),
            Faces.CODEC.fieldOf("faces").forGetter(Element::faces),
            Codec.STRING.fieldOf("type").forGetter(Element::type),
            Codec.STRING.fieldOf("uuid").forGetter(Element::uuid),
            EyelibCodec.FLOATS2VEC3F_CODEC.optionalFieldOf("rotation", new Vector3f()).forGetter(Element::rotation)
    ).apply(ins, Element::new));
}
