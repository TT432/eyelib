package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;

import java.util.List;

/** @author TT432 */
public record ParticleMotionCollision(
        MolangValue enabled,
        float collisionDrag,
        float coefficientOfRestitution,
        float collisionRadius,
        boolean expireOnContact,
        List<Event> events
) implements ParticleParticleComponent {
    public static final Codec<ParticleMotionCollision> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("enabled", MolangValue.TRUE_VALUE).forGetter(ParticleMotionCollision::enabled),
            Codec.FLOAT.optionalFieldOf("collision_drag", 0F).forGetter(ParticleMotionCollision::collisionDrag),
            Codec.FLOAT.optionalFieldOf("coefficient_of_restitution", 0F).forGetter(ParticleMotionCollision::coefficientOfRestitution),
            Codec.FLOAT.optionalFieldOf("collision_radius", 0F).forGetter(ParticleMotionCollision::collisionRadius),
            Codec.BOOL.optionalFieldOf("expire_on_contact", false).forGetter(ParticleMotionCollision::expireOnContact),
            Event.CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(ParticleMotionCollision::events)
    ).apply(ins, ParticleMotionCollision::new));

    public record Event(
            String event,
            float minSpeed
    ) {
        public static final Codec<Event> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(Event::event),
                Codec.FLOAT.optionalFieldOf("min_speed", 2F).forGetter(Event::minSpeed)
        ).apply(ins, Event::new));
    }
}