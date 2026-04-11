package io.github.tt432.eyelibimporter.model.bbmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record Group(
        String name,
        Vector3f origin,
        Vector3f rotation,
        String uuid,
        boolean export,
        boolean isOpen,
        boolean locked,
        boolean visibility,
        boolean mirror_uv,
        int color,
        int autouv,
        boolean shade
) {

    public static final MapCodec<Group> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Group::name),
            BbModelCodecs.FLOATS2VEC3F_CODEC.fieldOf("origin").forGetter(Group::origin),
            BbModelCodecs.FLOATS2VEC3F_CODEC.optionalFieldOf("rotation", new Vector3f()).forGetter(Group::rotation),
            Codec.STRING.fieldOf("uuid").forGetter(Group::uuid),
            Codec.BOOL.fieldOf("export").forGetter(Group::export),
            Codec.BOOL.fieldOf("isOpen").forGetter(Group::isOpen),
            Codec.BOOL.fieldOf("locked").forGetter(Group::locked),
            Codec.BOOL.fieldOf("visibility").forGetter(Group::visibility),
            Codec.BOOL.fieldOf("mirror_uv").forGetter(Group::mirror_uv),
            Codec.INT.fieldOf("color").forGetter(Group::color),
            Codec.INT.fieldOf("autouv").forGetter(Group::autouv),
            Codec.BOOL.optionalFieldOf("shade", false).forGetter(Group::shade)
    ).apply(ins, Group::new));

    public static final Codec<Group> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Group::name),
            BbModelCodecs.FLOATS2VEC3F_CODEC.fieldOf("origin").forGetter(Group::origin),
            BbModelCodecs.FLOATS2VEC3F_CODEC.fieldOf("rotation").forGetter(Group::rotation),
            Codec.STRING.fieldOf("uuid").forGetter(Group::uuid),
            Codec.BOOL.fieldOf("export").forGetter(Group::export),
            Codec.BOOL.fieldOf("isOpen").forGetter(Group::isOpen),
            Codec.BOOL.fieldOf("locked").forGetter(Group::locked),
            Codec.BOOL.fieldOf("visibility").forGetter(Group::visibility),
            Codec.BOOL.fieldOf("mirror_uv").forGetter(Group::mirror_uv),
            Codec.INT.fieldOf("color").forGetter(Group::color),
            Codec.INT.fieldOf("autouv").forGetter(Group::autouv),
            Codec.BOOL.fieldOf("shade").forGetter(Group::shade)
    ).apply(ins, Group::new));
}
