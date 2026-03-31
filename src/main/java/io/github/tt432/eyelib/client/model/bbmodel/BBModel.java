package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
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
        List<Outliner> outliner,
        List<Texture> textures,
        List<Group> groups
) {
    public static final Codec<BBModel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Meta.CODEC.fieldOf("meta").forGetter(BBModel::meta),
            Codec.STRING.fieldOf("name").forGetter(BBModel::name),
            Codec.STRING.fieldOf("model_identifier").forGetter(BBModel::modelIdentifier),
            Codec.DOUBLE.listOf().fieldOf("visible_box").forGetter(BBModel::visibleBox),
            Resolution.CODEC.fieldOf("resolution").forGetter(BBModel::resolution),
            Element.CODEC.listOf().fieldOf("elements").forGetter(BBModel::elements),
            Outliner.CODEC.listOf().fieldOf("outliner").forGetter(BBModel::outliner),
            Texture.CODEC.listOf().fieldOf("textures").forGetter(BBModel::textures),
            Group.CODEC.listOf().optionalFieldOf("groups", List.of()).forGetter(BBModel::groups)
    ).apply(ins, BBModel::new));
}
