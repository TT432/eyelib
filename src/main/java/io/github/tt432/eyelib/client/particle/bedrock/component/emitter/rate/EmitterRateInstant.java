package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * 一次性发射全部粒子，除非循环否则没有更多
 *
 * @param numParticles 粒子发射数量。每次发射器循环评估一次
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_rate_instant", type = "emitter_rate", target = ComponentTarget.EMITTER)
public record EmitterRateInstant(
        MolangValue numParticles
) implements EmitterParticleComponent {
    public static final Codec<EmitterRateInstant> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("num_particles", MolangValue.getConstant(10))
                    .forGetter(o -> o.numParticles)
    ).apply(ins, EmitterRateInstant::new));

    @Override
    public void onLoop(BrParticleEmitter emitter) {
        for (int i = 0; i < ((int) numParticles.eval(emitter.molangScope)); i++) {
            emitter.emit();
        }
    }
}
