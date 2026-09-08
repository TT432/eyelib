package io.github.tt432.eyelib.importer.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.With;
import java.util.List;

/** @author TT432 */
@With
@SuppressWarnings("NullAway")
public record BBModel(
        Meta meta,
        String name,
        @SerializedName("model_identifier")
        String modelIdentifier,
        @SerializedName("visible_box")
        List<Double> visibleBox,
        Resolution resolution,
        List<Element> elements,
        List<Outliner.CubeOrOutliner> outliner,
        List<Texture> textures,
        List<Group> groups
) {
    private static final Codec<Outliner.CubeOrOutliner> ROOT_OUTLINER_CODEC = Codec.either(
            Outliner.CODEC,
            Codec.STRING
    ).xmap(
            entry -> entry.map(
                    outliner -> new Outliner.CubeOrOutliner(outliner, null),
                    uuid -> new Outliner.CubeOrOutliner(null, uuid)
            ),
            entry -> entry.outliner() != null
                    ? Either.left(entry.outliner())
                    : Either.right(entry.uuid())
    );

    public static final Codec<BBModel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Meta.CODEC.fieldOf("meta").forGetter(BBModel::meta),
            Codec.STRING.fieldOf("name").forGetter(BBModel::name),
            Codec.STRING.fieldOf("model_identifier").forGetter(BBModel::modelIdentifier),
            Codec.DOUBLE.listOf().fieldOf("visible_box").forGetter(BBModel::visibleBox),
            Resolution.CODEC.fieldOf("resolution").forGetter(BBModel::resolution),
            Element.CODEC.listOf().fieldOf("elements").forGetter(BBModel::elements),
            ROOT_OUTLINER_CODEC.listOf().fieldOf("outliner").forGetter(BBModel::outliner),
            Texture.CODEC.listOf().fieldOf("textures").forGetter(BBModel::textures),
            Group.CODEC.listOf().optionalFieldOf("groups", List.of()).forGetter(BBModel::groups)
    ).apply(ins, (meta, name, modelIdentifier, visibleBox, resolution, elements, outliner, textures, groups) ->
            new BBModel(meta, name, modelIdentifier, visibleBox, resolution, elements, outliner,
                    normalizeTextureUvSize(textures, resolution), groups)));

    /**
     * 对齐 Blockbench 语义：texture 未显式写 uv_width/uv_height 时，UV 空间回落到项目 resolution
     * （bbmodel.js 加载时 Project.texture_width/height = resolution；bedrock 格式无 per_texture_uv_size，
     * 其 texture 条目不写 uv_width/uv_height）。缺省为 0 会导致导入时所有面被当作非法 UV 丢弃。
     */
    private static List<Texture> normalizeTextureUvSize(List<Texture> textures, Resolution resolution) {
        return textures.stream()
                .map(t -> t.uvWidth() > 0 && t.uvHeight() > 0
                        ? t
                        : t.withUvWidth(t.uvWidth() > 0 ? t.uvWidth() : resolution.width())
                          .withUvHeight(t.uvHeight() > 0 ? t.uvHeight() : resolution.height()))
                .toList();
    }
}
