package io.github.tt432.eyelib.client.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author TT432
 */
public record BrClientEntity(
        String identifier,
        Map<String, String> materials,
        Map<String, String> textures,
        Map<String, String> geometry,
        Map<String, String> animations,
        List<String> render_controllers,
        Optional<BrClientEntityScripts> scripts
) {
    public static final Codec<BrClientEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            RecordCodecBuilder.<BrClientEntity>create(ins1 -> ins1.group(
                    RecordCodecBuilder.<BrClientEntity>create(ins2 -> ins2.group(
                            Codec.STRING.fieldOf("identifier").forGetter(BrClientEntity::identifier),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("materials", Map.of()).forGetter(BrClientEntity::materials),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("textures", Map.of()).forGetter(BrClientEntity::textures),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("geometry", Map.of()).forGetter(BrClientEntity::geometry),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("animations", Map.of()).forGetter(BrClientEntity::animations),
                            Codec.STRING.listOf().optionalFieldOf("render_controllers", List.of()).forGetter(BrClientEntity::render_controllers),
                            BrClientEntityScripts.CODEC.optionalFieldOf("scripts").forGetter(BrClientEntity::scripts)
                    ).apply(ins2, BrClientEntity::new)).fieldOf("description").forGetter(o -> o)
            ).apply(ins1, o -> o)).fieldOf("minecraft:client_entity").forGetter(o -> o)
    ).apply(ins, o -> o));
}
