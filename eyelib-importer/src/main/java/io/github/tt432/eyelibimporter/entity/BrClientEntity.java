package io.github.tt432.eyelibimporter.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.addon.BedrockVersionValue;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @param particle_effects 短名称 -> 全名
 * @author TT432
 */
@NullMarked
/** @author TT432 */
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
        Optional<BrClientEntityScripts> scripts,
        Optional<BedrockResourceValue.ObjectValue> spawn_egg
) {
    private static final Codec<BedrockResourceValue.ObjectValue> OBJECT_VALUE_CODEC = ImporterCodecUtil.JSON_ELEMENT_CODEC.comapFlatMap(
            jsonElement -> {
                BedrockResourceValue value = BedrockResourceValue.fromJsonElement(jsonElement);
                return value instanceof BedrockResourceValue.ObjectValue objectValue
                        ? DataResult.success(objectValue)
                        : DataResult.error(() -> "Expected object value");
            },
            value -> {
                throw new UnsupportedOperationException("Client entity object encoding is not supported");
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
        scripts = scripts == null ? Optional.empty() : scripts;
        spawn_egg = spawn_egg == null ? Optional.empty() : spawn_egg;
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
                render_controllers, scripts, Optional.empty());
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
                                Codec.STRING.listOf().optionalFieldOf("render_controllers", List.of()).forGetter(BrClientEntity::render_controllers),
                                BrClientEntityScripts.CODEC.optionalFieldOf("scripts").forGetter(BrClientEntity::scripts),
                                OBJECT_VALUE_CODEC.optionalFieldOf("spawn_egg").forGetter(BrClientEntity::spawn_egg)
                        ).apply(ins2, BrClientEntity::new)).fieldOf("description").forGetter(o -> o)
                ).apply(ins1, o -> o)).fieldOf(rootField).forGetter(o -> o)
        ).apply(ins, o -> o));
    }
}