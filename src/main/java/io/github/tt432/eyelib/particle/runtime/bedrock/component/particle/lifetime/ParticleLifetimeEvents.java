package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** @author TT432 */
public record ParticleLifetimeEvents(
        List<String> creationEvent,
        List<String> expirationEvent,
        TreeMap<Float, List<String>> timeline
) implements ParticleParticleComponent {
    public static final Codec<ParticleLifetimeEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().optionalFieldOf("creation_event", List.of()).forGetter(ParticleLifetimeEvents::creationEvent),
            Codec.STRING.listOf().optionalFieldOf("expiration_event", List.of()).forGetter(ParticleLifetimeEvents::expirationEvent),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).xmap(ParticleLifetimeEvents::decodeTimeline, ParticleLifetimeEvents::encodeTimeline)
                    .optionalFieldOf("timeline", new TreeMap<>()).forGetter(ParticleLifetimeEvents::timeline)
    ).apply(ins, ParticleLifetimeEvents::new));

    private static TreeMap<Float, List<String>> decodeTimeline(Map<String, List<String>> values) {
        TreeMap<Float, List<String>> result = new TreeMap<>(Comparator.comparingDouble(k -> k));
        values.forEach((key, value) -> result.put(Float.parseFloat(key), List.copyOf(value)));
        return result;
    }

    private static Map<String, List<String>> encodeTimeline(TreeMap<Float, List<String>> values) {
        TreeMap<String, List<String>> result = new TreeMap<>();
        values.forEach((key, value) -> result.put(Float.toString(key), List.copyOf(value)));
        return result;
    }
}