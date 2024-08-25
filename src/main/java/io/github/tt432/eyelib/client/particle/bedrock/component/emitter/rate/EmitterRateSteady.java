package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.Blackboard;
import lombok.extern.slf4j.Slf4j;

/**
 * 粒子以恒定的速率或 Molang 速率随时间发射
 *
 * @param spawnRate    粒子的发射频率，以粒子/秒为单位。每发射一个粒子时评估一次
 * @param maxParticles 该发射器同时激活的最大粒子数量。每次粒子发射器循环时评估一次
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_rate_steady", type = "emitter_rate", target = ComponentTarget.EMITTER)
@Slf4j
public record EmitterRateSteady(
        MolangValue spawnRate,
        MolangValue maxParticles
) implements EmitterParticleComponent {
    public static final Codec<EmitterRateSteady> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.fieldOf("spawn_rate").forGetter(o -> o.spawnRate),
            MolangValue.CODEC.fieldOf("max_particles").forGetter(o -> o.maxParticles)
    ).apply(ins, EmitterRateSteady::new));

    @lombok.Data
    private static final class Data {
        float emitTimestamp;
        float loopTime;
    }

    @Override
    public void onTick(BrParticleEmitter emitter) {
        Blackboard blackboard = emitter.blackboard;
        var data = blackboard.getOrCreate("emitter_rate_steady", new Data());
        float age = emitter.getAge();

        if (data.emitTimestamp == 0 || age - data.loopTime >= data.emitTimestamp) {
            emitter.emit();
            data.loopTime = 1 / spawnRate.eval(emitter.molangScope);
            data.emitTimestamp = age;
        }
    }

    @Override
    public void onLoop(BrParticleEmitter emitter) {
        emitter.blackboard.getOrCreate("emitter_rate_steady", new Data()).emitTimestamp = 0;
    }

    @Override
    public boolean canEmit(BrParticleEmitter emitter) {
        return emitter.getEmitCount() < maxParticles().eval(emitter.molangScope);
    }
}
