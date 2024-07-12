package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.data.Blackboard;

/**
 * 发射器会循环工作直到被移除。
 *
 * @param activeTime 每次循环发射器发射粒子的时间。每次粒子发射器循环时评估一次
 * @param sleepTime  每次循环发射器暂停发射粒子的时间。每次粒子发射器循环时评估一次
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_lifetime_looping", type = "emitter_lifetime", target = ComponentTarget.EMITTER)
public record EmitterLifetimeLooping(
        MolangValue activeTime,
        MolangValue sleepTime
) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeLooping> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("active_time", MolangValue.ONE).forGetter(o -> o.activeTime),
            MolangValue.CODEC.optionalFieldOf("sleep_time", MolangValue.ZERO).forGetter(o -> o.sleepTime)
    ).apply(ins, EmitterLifetimeLooping::new));

    @lombok.Data
    private static class Data {
        private float activeTime;
        private float sleepTime;
        private float loopTime;
    }

    @Override
    public void onTick(BrParticleEmitter emitter) {
        Blackboard blackboard = emitter.blackboard;
        MolangScope molangScope = emitter.molangScope;

        final String dataKey = "lifetime_looping";
        Data data = blackboard.getOrCreate(dataKey, new Data());

        if (emitter.getAge() > data.loopTime) {
            emitter.setEnabled(true);
            emitter.onLoopStart();
            data.activeTime = activeTime.eval(molangScope);
            data.sleepTime = sleepTime.eval(molangScope);
            data.loopTime = data.activeTime + data.sleepTime;
        } else {
            emitter.setEnabled(emitter.getAge() < data.activeTime);
        }
    }
}
