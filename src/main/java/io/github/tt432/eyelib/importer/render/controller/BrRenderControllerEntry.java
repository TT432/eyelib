package io.github.tt432.eyelib.importer.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangMapEntry;
import io.github.tt432.eyelib.molang.MolangValue;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/** @author TT432 */
@NullMarked
public record BrRenderControllerEntry(
        MolangValue geometry,
        List<MolangValue> textures,
        Map<String, Map<String, List<String>>> arrays,
        List<MolangMapEntry> materials,
        Map<String, MolangValue> partVisibility,
        boolean ignoreLighting,
        Optional<BrRcColor> color,
        Optional<BrRcColor> isHurtColor,
        Optional<BrRcColor> onFireColor,
        Optional<BrRcColor> overlayColor
) {
    public static final Codec<BrRenderControllerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MolangValue.CODEC.optionalFieldOf("geometry", MolangValue.ZERO).forGetter(BrRenderControllerEntry::geometry),
            MolangValue.CODEC.listOf().optionalFieldOf("textures", List.of()).forGetter(BrRenderControllerEntry::textures),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()))
                    .optionalFieldOf("arrays", Map.of())
                    .forGetter(BrRenderControllerEntry::arrays),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    BrRenderControllerEntry::toMolangMapEntries,
                    BrRenderControllerEntry::fromMolangMapEntries
            ).optionalFieldOf("materials", List.of()).forGetter(BrRenderControllerEntry::materials),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    BrRenderControllerEntry::flattenMolangMaps,
                    List::of
            ).optionalFieldOf("part_visibility", Map.of()).forGetter(BrRenderControllerEntry::partVisibility),
            Codec.BOOL.optionalFieldOf("ignore_lighting", false).forGetter(BrRenderControllerEntry::ignoreLighting),
            BrRcColor.CODEC.optionalFieldOf("color").forGetter(BrRenderControllerEntry::color),
            BrRcColor.CODEC.optionalFieldOf("is_hurt_color").forGetter(BrRenderControllerEntry::isHurtColor),
            BrRcColor.CODEC.optionalFieldOf("on_fire_color").forGetter(BrRenderControllerEntry::onFireColor),
            BrRcColor.CODEC.optionalFieldOf("overlay_color").forGetter(BrRenderControllerEntry::overlayColor)
    ).apply(instance, BrRenderControllerEntry::new));

    private static List<MolangMapEntry> toMolangMapEntries(List<Map<String, MolangValue>> list) {
        List<MolangMapEntry> result = new ArrayList<>();
        for (Map<String, MolangValue> map : list) {
            for (var entry : map.entrySet()) {
                result.add(new MolangMapEntry(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private static List<Map<String, MolangValue>> fromMolangMapEntries(List<MolangMapEntry> entries) {
        return entries.stream()
                .map(e -> Map.of(e.key(), e.value()))
                .toList();
    }

    private static Map<String, MolangValue> flattenMolangMaps(List<Map<String, MolangValue>> values) {
        Map<String, MolangValue> result = new LinkedHashMap<>();
        for (Map<String, MolangValue> value : values) {
            result.putAll(value);
        }
        return result;
    }
}
