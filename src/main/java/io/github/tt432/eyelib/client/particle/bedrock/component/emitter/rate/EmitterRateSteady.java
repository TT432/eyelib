package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * 粒子以恒定的速率或 Molang 速率随时间发射
 *
 * @param spawnRate    粒子的发射频率，以粒子/秒为单位。每发射一个粒子时评估一次
 * @param maxParticles 该发射器同时激活的最大粒子数量。每次粒子发射器循环时评估一次
 * @author TT432
 */
@ParticleComponent(value = "emitter_rate_steady", type = "emitter_rate", target = ComponentTarget.EMITTER)
public record EmitterRateSteady(
        MolangValue spawnRate,
        MolangValue maxParticles
) {
    public static final Codec<EmitterRateSteady> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.fieldOf("spawn_rate").forGetter(o -> o.spawnRate),
            MolangValue.CODEC.fieldOf("max_particles").forGetter(o -> o.maxParticles)
    ).apply(ins, EmitterRateSteady::new));
}
