package io.github.tt432.eyelibimporter.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/** @author TT432 */
@NullMarked
public record BrRenderControllerEntry(
        MolangValue geometry,
        List<MolangValue> textures,
        Map<String, Map<String, List<String>>> arrays,
        Map<String, MolangValue> materials,
        Map<String, MolangValue> partVisibility
) {
    public static final Codec<BrRenderControllerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MolangValue.CODEC.optionalFieldOf("geometry", MolangValue.ZERO).forGetter(BrRenderControllerEntry::geometry),
            MolangValue.CODEC.listOf().optionalFieldOf("textures", List.of()).forGetter(BrRenderControllerEntry::textures),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()))
                    .optionalFieldOf("arrays", Map.of())
                    .forGetter(BrRenderControllerEntry::arrays),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    BrRenderControllerEntry::flattenMolangMaps,
                    List::of
            ).optionalFieldOf("materials", Map.of()).forGetter(BrRenderControllerEntry::materials),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    BrRenderControllerEntry::flattenMolangMaps,
                    List::of
            ).optionalFieldOf("part_visibility", Map.of()).forGetter(BrRenderControllerEntry::partVisibility)
    ).apply(instance, BrRenderControllerEntry::new));

    private static Map<String, MolangValue> flattenMolangMaps(List<Map<String, MolangValue>> values) {
        Map<String, MolangValue> result = new Object2ObjectOpenHashMap<>();
        for (Map<String, MolangValue> value : values) {
            result.putAll(value);
        }
        return result;
    }
}
