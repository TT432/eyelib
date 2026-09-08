package io.github.tt432.eyelib.importer.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Optional;

/** BBModel 大纲元素。cube 之外的类型（locator / null_object / mesh 等）不写 from/to/faces 等字段，
 * 因此 cube 专有字段全部可选；消费方必须先按 {@link #type} 分派再读取对应字段。
 * @author TT432 */
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

        @Nullable Vector3f from,
        @Nullable Vector3f to,
        int autouv,
        int color,
        @Nullable Vector3f origin,

        @SerializedName("uv_offset")
        Vector2f uvOffset,

        double inflate,

        @Nullable Faces faces,
        String type,
        String uuid,
        Vector3f rotation,

        /** locator / null_object 的位置；缺省时回落到 from（Blockbench Locator.extend 兼容旧键）。 */
        @Nullable Vector3f position,

        @SerializedName("ignore_inherited_scale")
        boolean ignoreInheritedScale,

        @SerializedName("mirror_uv")
        boolean mirrorUv
) {
    private record Part1(
            String name,
            boolean boxUv,
            String renderOrder,
            boolean locked,
            boolean allowMirrorModeling,
            int autouv,
            int color,
            String type,
            String uuid,
            Vector3f rotation
    ) {
        static final MapCodec<Part1> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.STRING.fieldOf("name").forGetter(Part1::name),
                Codec.BOOL.optionalFieldOf("box_uv", false).forGetter(Part1::boxUv),
                Codec.STRING.optionalFieldOf("render_order", "default").forGetter(Part1::renderOrder),
                Codec.BOOL.optionalFieldOf("locked", false).forGetter(Part1::locked),
                Codec.BOOL.optionalFieldOf("allow_mirror_modeling", true).forGetter(Part1::allowMirrorModeling),
                Codec.INT.optionalFieldOf("autouv", 0).forGetter(Part1::autouv),
                Codec.INT.optionalFieldOf("color", 0).forGetter(Part1::color),
                Codec.STRING.fieldOf("type").forGetter(Part1::type),
                Codec.STRING.fieldOf("uuid").forGetter(Part1::uuid),
                BbModelCodecs.FLOATS2VEC3F_CODEC.optionalFieldOf("rotation", new Vector3f()).forGetter(Part1::rotation)
        ).apply(ins, Part1::new));
    }

    private record Part2(
            Optional<Vector3f> from,
            Optional<Vector3f> to,
            Optional<Vector3f> origin,
            Vector2f uvOffset,
            double inflate,
            Optional<Faces> faces,
            Optional<Vector3f> position,
            boolean ignoreInheritedScale,
            boolean mirrorUv
    ) {
        static final MapCodec<Part2> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                BbModelCodecs.FLOATS2VEC3F_CODEC.optionalFieldOf("from").forGetter(Part2::from),
                BbModelCodecs.FLOATS2VEC3F_CODEC.optionalFieldOf("to").forGetter(Part2::to),
                BbModelCodecs.FLOATS2VEC3F_CODEC.optionalFieldOf("origin").forGetter(Part2::origin),
                BbModelCodecs.FLOATS2VEC2F_CODEC.optionalFieldOf("uv_offset", new Vector2f()).forGetter(Part2::uvOffset),
                Codec.DOUBLE.optionalFieldOf("inflate", 0D).forGetter(Part2::inflate),
                Faces.CODEC.optionalFieldOf("faces").forGetter(Part2::faces),
                BbModelCodecs.FLOATS2VEC3F_CODEC.optionalFieldOf("position").forGetter(Part2::position),
                Codec.BOOL.optionalFieldOf("ignore_inherited_scale", false).forGetter(Part2::ignoreInheritedScale),
                Codec.BOOL.optionalFieldOf("mirror_uv", false).forGetter(Part2::mirrorUv)
        ).apply(ins, Part2::new));
    }

    public static final Codec<Element> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Part1.MAP_CODEC.forGetter(e -> new Part1(e.name, e.boxUv, e.renderOrder, e.locked, e.allowMirrorModeling,
                    e.autouv, e.color, e.type, e.uuid, e.rotation)),
            Part2.MAP_CODEC.forGetter(e -> new Part2(Optional.ofNullable(e.from), Optional.ofNullable(e.to), Optional.ofNullable(e.origin), e.uvOffset, e.inflate, Optional.ofNullable(e.faces),
                    Optional.ofNullable(e.position), e.ignoreInheritedScale, e.mirrorUv))
    ).apply(ins, (p1, p2) -> new Element(
            p1.name, p1.boxUv, p1.renderOrder, p1.locked, p1.allowMirrorModeling,
            p2.from.orElse(null), p2.to.orElse(null), p1.autouv, p1.color, p2.origin.orElse(null),
            p2.uvOffset, p2.inflate, p2.faces.orElse(null), p1.type, p1.uuid, p1.rotation,
            p2.position.orElse(null), p2.ignoreInheritedScale, p2.mirrorUv)));
}
