package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;

public record EmitterLifetimeOnce(MolangValue activeTime) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeOnce> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("active_time", MolangValue.getConstant(10))
                    .forGetter(EmitterLifetimeOnce::activeTime)
    ).apply(ins, EmitterLifetimeOnce::new));

    @Override
    public void onStart(EmitterAccess emitter) {
        emitter.blackboard().put("lifetime_once", activeTime.eval(emitter.molangScope()));
        emitter.setEnabled(true);
    }

    @Override
    public void onTick(EmitterAccess emitter) {
        if (!emitter.blackboard().getOrDefault("lifetime_once_emitted", Boolean.class, false)) {
            emitter.blackboard().put("lifetime_once_emitted", true);
            emitter.onLoopStart();
        }

        if (emitter.age() > emitter.blackboard().getOrDefault("lifetime_once", Float.class, 0F)) {
            emitter.remove();
        }
    }
}
