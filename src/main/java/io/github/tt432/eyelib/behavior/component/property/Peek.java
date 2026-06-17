package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:peek
 *
 * @param on_open        事件触发配置
 * @param on_close       事件触发配置
 * @param on_target_open 事件触发配置
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Peek(
        EventConfig on_open,
        EventConfig on_close,
        EventConfig on_target_open
) implements Component {
    public static final Codec<Peek> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            EventConfig.CODEC.fieldOf("on_open").forGetter(Peek::on_open),
            EventConfig.CODEC.fieldOf("on_close").forGetter(Peek::on_close),
            EventConfig.CODEC.fieldOf("on_target_open").forGetter(Peek::on_target_open)
    ).apply(ins, Peek::new));

    @Override
    public String id() {
        return "peek";
    }

    public record EventConfig(
            String event,
            String target
    ) {
        public static final Codec<EventConfig> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(EventConfig::event),
                Codec.STRING.optionalFieldOf("target", "self").forGetter(EventConfig::target)
        ).apply(ins, EventConfig::new));
    }
}
