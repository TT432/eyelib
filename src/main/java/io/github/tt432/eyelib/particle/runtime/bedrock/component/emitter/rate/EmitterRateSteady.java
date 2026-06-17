package io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;

/** @author TT432 */
public record EmitterRateSteady(
        MolangValue spawnRate,
        MolangValue maxParticles
) implements EmitterParticleComponent {
    public static final Codec<EmitterRateSteady> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.fieldOf("spawn_rate").forGetter(EmitterRateSteady::spawnRate),
            MolangValue.CODEC.fieldOf("max_particles").forGetter(EmitterRateSteady::maxParticles)
    ).apply(ins, EmitterRateSteady::new));

    private static final class Data {
        float emitTimestamp;
        float loopTime;
    }

    @Override
    public void onTick(EmitterAccess emitter) {
        Data data = emitter.blackboard().getOrCreate("emitter_rate_steady", Data.class, new Data());
        float age = emitter.age();

        if (data.emitTimestamp == 0 || age - data.emitTimestamp >= data.loopTime) {
            emitter.emit();
            data.loopTime = 1 / spawnRate.eval(emitter.molangScope());
            data.emitTimestamp = age;
        }
    }

    @Override
    public void onLoop(EmitterAccess emitter) {
        emitter.blackboard().getOrCreate("emitter_rate_steady", Data.class, new Data()).emitTimestamp = 0;
    }

    @Override
    public boolean canEmit(EmitterAccess emitter) {
        return emitter.emitCount() < maxParticles.eval(emitter.molangScope());
    }
}