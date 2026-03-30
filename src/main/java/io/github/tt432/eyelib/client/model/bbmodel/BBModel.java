package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.util.EntryStreams;
import io.github.tt432.eyelib.util.client.Textures;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Group> groups,

        List<Textures.ModelWithTexture> byTextureModels,
        Textures.ModelWithTexture mergedModel
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
    ).apply(ins, BBModel::from));

    public static BBModel from(Meta meta, String name, String modelIdentifier, List<Double> visibleBox, Resolution resolution, List<Element> elements, List<Outliner> outliner, List<Texture> textures, List<Group> groups) {
        List<Textures.ModelWithTexture> byTextureModels = makeByTextureModels(name, elements, outliner, textures, groups);
        return new BBModel(meta, name, modelIdentifier, visibleBox, resolution, elements, outliner, textures, groups,
                byTextureModels,
                Textures.repackModels(byTextureModels)
        );
    }

    private static List<Textures.ModelWithTexture> makeByTextureModels(
            String name, List<Element> elements, List<Outliner> outliner, List<Texture> textures, List<Group> groups
    ) {
        List<Textures.ModelWithTexture> result = new ArrayList<>();
        for (int i = 0; i < textures.size(); i++) {
            var groupMap = groups.stream().map(g -> Map.entry(g.uuid(), g)).collect(EntryStreams.collect());

            Int2ObjectMap<Model.Bone> allBones = new Int2ObjectOpenHashMap<>();
            initBones(elements, outliner, allBones, groupMap, i, textures);

            result.add(new Textures.ModelWithTexture(new Model(name, allBones), textures.get(i)));
        }
        return result;
    }

    private static void initBones(List<Element> elements, List<Outliner> outliner, Int2ObjectMap<Model.Bone> allBonesMap,
                                  Map<String, Group> groupMap, int textureIndex, List<Texture> textures) {
        Map<String, Element> elementMap = new HashMap<>();
        if (elements != null) {
            for (Element element : elements) {
                if (element.uuid() != null) {
                    elementMap.put(element.uuid(), element);
                }
            }
        }

        if (outliner != null && !outliner.isEmpty()) {
            for (var element : outliner) {
                processOutlinerEntry(element, null, elementMap, allBonesMap, groupMap, textureIndex, textures);
            }
        }
    }

    private static void processOutlinerEntry(Outliner entry, @Nullable BBBone parent, Map<String, Element> elementMap,
                                             Int2ObjectMap<Model.Bone> allBonesMap,
                                             Map<String, Group> groupMap, int textureIndex, List<Texture> textures) {
        String uuid = entry.uuid();
        Group group = groupMap.get(uuid);
        if (group == null && entry.group().isPresent()) {
            group = entry.group().get();
        }
        if (group == null) return;
        BBBone bone = new BBBone(GlobalBoneIdHandler.get(group.name()), parent == null ? -1 : parent.id());

        if (group.origin() != null) {
            bone.origin().set(group.origin()).div(16);
        }

        if (group.rotation() != null) {
            bone.rotation().set(group.rotation()).mul(EyeMath.DEGREES_TO_RADIANS);
        }

        List<Model.Cube> cubes = new ArrayList<>();

        for (String cubeId : entry.cubes()) {
            Element el = elementMap.get(cubeId);
            if (el != null) {
                var cube = el.createBbCube(textureIndex, textures);
                if (cube != null) cubes.add(cube);
            }
        }

        allBonesMap.put(bone.id(), bone.withCubes(cubes).createBone());

        for (Outliner child : entry.children()) {
            processOutlinerEntry(child, bone, elementMap, allBonesMap, groupMap, textureIndex, textures);
        }
    }
}
