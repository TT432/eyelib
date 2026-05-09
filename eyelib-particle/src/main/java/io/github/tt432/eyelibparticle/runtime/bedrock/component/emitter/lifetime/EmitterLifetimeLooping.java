package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;

public record EmitterLifetimeLooping(
        MolangValue activeTime,
        MolangValue sleepTime
) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeLooping> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("active_time", MolangValue.ONE).forGetter(EmitterLifetimeLooping::activeTime),
            MolangValue.CODEC.optionalFieldOf("sleep_time", MolangValue.ZERO).forGetter(EmitterLifetimeLooping::sleepTime)
    ).apply(ins, EmitterLifetimeLooping::new));

    private static final class Data {
        float activeTime;
        float sleepTime;
        float loopTime;
    }

    @Override
    public void onTick(EmitterAccess emitter) {
        Data data = emitter.blackboard().getOrCreate("lifetime_looping", Data.class, new Data());

        if (emitter.age() > data.loopTime) {
            emitter.setEnabled(true);
            emitter.onLoopStart();
            data.activeTime = activeTime.eval(emitter.molangScope());
            data.sleepTime = sleepTime.eval(emitter.molangScope());
            data.loopTime = data.activeTime + data.sleepTime;
        } else {
            emitter.setEnabled(emitter.age() < data.activeTime);
        }
    }
}
