package io.github.tt432.eyelib.common.behavior.event.filter.base;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.common.behavior.event.filter.Operator;
import io.github.tt432.eyelib.common.behavior.event.filter.Subject;

/**
 * @author TT432
 */
public final class ActorHealth extends BaseFilter<Integer> {
    public static final MapCodec<ActorHealth> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.INT.fieldOf("value").forGetter(o -> o.value),
            Subject.CODEC.optionalFieldOf("subject", Subject.self).forGetter(o -> o.subject),
            Operator.CODEC.optionalFieldOf("operator", Operator.EQUALS).forGetter(o -> o.operator),
            Codec.STRING.fieldOf("domain").forGetter(o -> o.domain)
    ).apply(ins, ActorHealth::new));

    public ActorHealth(Integer value, Subject subject, Operator operator, String domain) {
        super(value, subject, operator, domain);
    }
}
