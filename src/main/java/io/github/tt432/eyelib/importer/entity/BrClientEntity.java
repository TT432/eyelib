package io.github.tt432.eyelib.importer.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.addon.BedrockVersionValue;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;
import io.github.tt432.eyelib.molang.MolangValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
public record BrClientEntity(
        String identifier,
        Optional<BedrockVersionValue> min_engine_version,
        Map<String, String> materials,
        Map<String, String> textures,
        Map<String, String> geometry,
        Map<String, String> animations,
        List<Map<String, String>> animation_controllers,
        Map<String, String> particle_effects,
        Map<String, String> sound_effects,
        List<String> render_controllers,
        Map<String, MolangValue> renderControllerConditions,
        Optional<BrClientEntityScripts> scripts,
        Optional<BedrockResourceValue.ObjectValue> spawn_egg,
        Map<String, String> item,
        boolean enable_attachables
) {
    private static final Codec<BedrockResourceValue.ObjectValue> OBJECT_VALUE_CODEC = ImporterCodecUtil.JSON_ELEMENT_CODEC.comapFlatMap(
            jsonElement -> {
                BedrockResourceValue value = BedrockResourceValue.fromJsonElement(jsonElement);
                return value instanceof BedrockResourceValue.ObjectValue objectValue
                        ? DataResult.success(objectValue)
                        : DataResult.error(() -> "Expected object value");
            },
            BedrockResourceValue.ObjectValue::toJsonElement
    );

    /**
     * render_controller 引用：BE 规范中 render_controllers 数组的元素可以是纯名字符串，
     * 也可以是 {@code {名字: molang条件}} 对象（条件控制该 RC 是否启用）。
     */
    private record RenderControllerRef(String name, @Nullable String condition) {
    }

    private static final Codec<RenderControllerRef> RENDER_CONTROLLER_CODEC = ImporterCodecUtil.JSON_ELEMENT_CODEC.comapFlatMap(
            jsonElement -> {
                if (jsonElement.isJsonPrimitive()) {
                    return DataResult.success(new RenderControllerRef(jsonElement.getAsString(), null));
                }
                if (jsonElement.isJsonObject()) {
                    var obj = jsonElement.getAsJsonObject();
                    if (!obj.entrySet().isEmpty()) {
                        var entry = obj.entrySet().iterator().next();
                        String condition = entry.getValue().isJsonPrimitive()
                                ? entry.getValue().getAsString()
                                : null;
                        return DataResult.success(new RenderControllerRef(entry.getKey(), condition));
                    }
                }
                return DataResult.error(() -> "Expected string or object for render_controller");
            },
            ref -> {
                if (ref.condition() == null) {
                    return new JsonPrimitive(ref.name());
                }
                var obj = new JsonObject();
                obj.addProperty(ref.name(), ref.condition());
                return obj;
            }
    );

    /** 将内联条件并入条件表；显式 render_controller_conditions 字段的条目优先。 */
    private static List<String> splitNames(List<RenderControllerRef> refs) {
        return refs.stream().map(RenderControllerRef::name).toList();
    }

    private static Map<String, MolangValue> mergeConditions(List<RenderControllerRef> refs,
                                                            Map<String, MolangValue> explicit) {
        Map<String, MolangValue> result = new LinkedHashMap<>();
        for (RenderControllerRef ref : refs) {
            if (ref.condition() != null) {
                result.put(ref.name(), new MolangValue(ref.condition()));
            }
        }
        result.putAll(explicit);
        return result;
    }

    private static final Codec<Map<String, String>> ITEM_FIELD_CODEC = Codec.either(
            Codec.STRING,
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
    ).xmap(
            either -> {
                Map<String, String> result = new LinkedHashMap<>();
                either.ifLeft(s -> result.put(s, "1.0"));
                either.ifRight(result::putAll);
                return result;
            },
            map -> {
                if (map.size() == 1 && "1.0".equals(map.values().iterator().next())) {
                    return Either.left(map.keySet().iterator().next());
                }
                return Either.right(map);
            }
    );

    public static final Codec<BrClientEntity> CODEC = wrapDescription("minecraft:client_entity");
    public static final Codec<BrClientEntity> ATTACHABLE_CODEC = wrapDescription("minecraft:attachable");

    public BrClientEntity {
        min_engine_version = min_engine_version == null ? Optional.empty() : min_engine_version;
        materials = Map.copyOf(materials);
        textures = Map.copyOf(textures);
        geometry = Map.copyOf(geometry);
        animations = Map.copyOf(animations);
        animation_controllers = animation_controllers.stream().map(Map::copyOf).toList();
        particle_effects = Map.copyOf(particle_effects);
        sound_effects = Map.copyOf(sound_effects);
        render_controllers = List.copyOf(render_controllers);
        renderControllerConditions = renderControllerConditions == null ? Map.of() : Map.copyOf(renderControllerConditions);
        scripts = scripts == null ? Optional.empty() : scripts;
        spawn_egg = spawn_egg == null ? Optional.empty() : spawn_egg;
        item = item == null ? Map.of() : Map.copyOf(item);
    }

    public BrClientEntity(
            String identifier,
            Map<String, String> materials,
            Map<String, String> textures,
            Map<String, String> geometry,
            Map<String, String> animations,
            Map<String, String> particle_effects,
            Map<String, String> sound_effects,
            List<String> render_controllers,
            Optional<BrClientEntityScripts> scripts
    ) {
        this(identifier, Optional.empty(), materials, textures, geometry, animations, List.of(), particle_effects, sound_effects,
                render_controllers, Map.of(), scripts, Optional.empty(), Map.of(), false);
    }

    private static Codec<BrClientEntity> wrapDescription(String rootField) {
        return RecordCodecBuilder.create(ins -> ins.group(
                RecordCodecBuilder.<BrClientEntity>create(ins1 -> ins1.group(
                        RecordCodecBuilder.<BrClientEntity>create(ins2 -> ins2.group(
                                Codec.STRING.fieldOf("identifier").forGetter(BrClientEntity::identifier),
                                BedrockVersionValue.CODEC.optionalFieldOf("min_engine_version").forGetter(BrClientEntity::min_engine_version),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("materials", Map.of()).forGetter(BrClientEntity::materials),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING.xmap(s -> s + ".png", s -> s.substring(0, s.length() - ".png".length()))).optionalFieldOf("textures", Map.of()).forGetter(BrClientEntity::textures),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("geometry", Map.of()).forGetter(BrClientEntity::geometry),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("animations", Map.of()).forGetter(BrClientEntity::animations),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING).listOf().optionalFieldOf("animation_controllers", List.of()).forGetter(BrClientEntity::animation_controllers),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("particle_effects", Map.of()).forGetter(BrClientEntity::particle_effects),
                                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("sound_effects", Map.of()).forGetter(BrClientEntity::sound_effects),
                                RENDER_CONTROLLER_CODEC.listOf().optionalFieldOf("render_controllers", List.of()).forGetter(e -> e.render_controllers().stream().map(n -> new RenderControllerRef(n, null)).toList()),
                                Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).optionalFieldOf("render_controller_conditions", Map.of()).forGetter(BrClientEntity::renderControllerConditions),
                                BrClientEntityScripts.CODEC.optionalFieldOf("scripts").forGetter(BrClientEntity::scripts),
                                OBJECT_VALUE_CODEC.optionalFieldOf("spawn_egg").forGetter(BrClientEntity::spawn_egg),
                                ITEM_FIELD_CODEC.optionalFieldOf("item", Map.of()).forGetter(BrClientEntity::item),
                                Codec.BOOL.optionalFieldOf("enable_attachables", false).forGetter(BrClientEntity::enable_attachables)
                        ).apply(ins2, (identifier, minEngineVersion, materials, textures, geometry, animations,
                                       animationControllers, particleEffects, soundEffects, renderControllerRefs,
                                       explicitConditions, scripts, spawnEgg, item, enableAttachables) ->
                                new BrClientEntity(identifier, minEngineVersion, materials, textures, geometry,
                                        animations, animationControllers, particleEffects, soundEffects,
                                        splitNames(renderControllerRefs),
                                        mergeConditions(renderControllerRefs, explicitConditions),
                                        scripts, spawnEgg, item, enableAttachables))).fieldOf("description").forGetter(o -> o)
                ).apply(ins1, o -> o)).fieldOf(rootField).forGetter(o -> o)
        ).apply(ins, o -> o));
    }
}