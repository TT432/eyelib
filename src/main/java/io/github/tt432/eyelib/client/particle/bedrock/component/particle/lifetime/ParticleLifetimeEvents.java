package io.github.tt432.eyelib.client.particle.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.chin.codec.ChinExtraCodecs;
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
    public static final Codec<ParticleLifetimeEvents> CODEC = RecordCodecBuilder.create(ins -> {
        final Codec<List<String>> elementCodec = ChinExtraCodecs.singleOrList(Codec.STRING);
        Comparator<Float> comparator = Comparator.comparingDouble(k -> k);
        return ins.group(
                ChinExtraCodecs.singleOrList(Codec.STRING).optionalFieldOf("creation_event", List.of())
                        .forGetter(o -> o.creationEvent),
                ChinExtraCodecs.singleOrList(Codec.STRING).optionalFieldOf("expiration_event", List.of())
                        .forGetter(o -> o.expirationEvent),
                ChinExtraCodecs.treeMap(EyelibCodec.STR_FLOAT_CODEC, elementCodec, comparator)
                        .optionalFieldOf("timeline", new TreeMap<>()).forGetter(o -> o.timeline)
        ).apply(ins, ParticleLifetimeEvents::new);
    });
}
