package io.github.tt432.eyelib.client.particle.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * todo
 *
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_lifetime_events", target = ComponentTarget.PARTICLE)
public record ParticleLifetimeEvents(
        List<String> creationEvent,
        List<String> expirationEvent,
        TreeMap<Float, List<String>> timeline
) implements ParticleParticleComponent {
    public static final Codec<ParticleLifetimeEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            EyelibCodec.singleOrList(Codec.STRING).optionalFieldOf("creation_event", List.of())
                    .forGetter(o -> o.creationEvent),
            EyelibCodec.singleOrList(Codec.STRING).optionalFieldOf("expiration_event", List.of())
                    .forGetter(o -> o.expirationEvent),
            EyelibCodec.treeMap(Codec.STRING.xmap(Float::parseFloat, String::valueOf),
                            EyelibCodec.singleOrList(Codec.STRING),
                            Comparator.comparingDouble(k -> k))
                    .optionalFieldOf("timeline", new TreeMap<>()).forGetter(o -> o.timeline)
    ).apply(ins, ParticleLifetimeEvents::new));
}
