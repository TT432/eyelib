package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * 只在被游戏告知应当发射时发射。多被用在旧版粒子效果
 *
 * @param maxParticles 每次粒子发射后评估
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_rate_manual", type = "emitter_rate", target = ComponentTarget.EMITTER)
public record EmitterRateManual(
        MolangValue maxParticles
) implements EmitterParticleComponent {
    public static final Codec<EmitterRateManual> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("max_particles", MolangValue.getConstant(50))
                    .forGetter(o -> o.maxParticles)
    ).apply(ins, EmitterRateManual::new));

    @Override
    public boolean canEmit(BrParticleEmitter emitter) {
        return emitter.getEmitCount() < maxParticles().eval(emitter.molangScope);
    }
}
