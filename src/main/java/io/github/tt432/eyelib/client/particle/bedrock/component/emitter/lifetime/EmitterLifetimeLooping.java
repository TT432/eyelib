package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * 发射器会循环工作直到被移除。
 *
 * @param activeTime 每次循环发射器发射粒子的时间。每次粒子发射器循环时评估一次
 * @param sleepTime  每次循环发射器暂停发射粒子的时间。每次粒子发射器循环时评估一次
 * @author TT432
 */
@ParticleComponent(value = "emitter_lifetime_looping", type = "emitter_lifetime", target = ComponentTarget.EMITTER)
public record EmitterLifetimeLooping(
        MolangValue activeTime,
        MolangValue sleepTime
) {
    public static final Codec<EmitterLifetimeLooping> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.fieldOf("active_time").forGetter(o -> o.activeTime),
            MolangValue.CODEC.fieldOf("sleep_time").forGetter(o -> o.sleepTime)
    ).apply(ins, EmitterLifetimeLooping::new));
}
