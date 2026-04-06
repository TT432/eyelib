package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.With;

import java.util.List;

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
    ).apply(ins, BBModel::new));
}
