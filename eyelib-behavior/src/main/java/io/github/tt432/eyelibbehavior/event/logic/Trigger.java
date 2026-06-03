package io.github.tt432.eyelibbehavior.event.logic;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.EntityBehaviorData;
import io.github.tt432.eyelibbehavior.event.filter.Filter;
import io.github.tt432.eyelibbehavior.event.filter.Subject;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * trigger 事件节点，触发指定名称的事件。
 * <p>
 * Bedrock 标准支持两种形式：
 * <ul>
 *   <li>简化形式: {@code "trigger": "event_name"} — 触发自身的另一事件</li>
 *   <li>完整形式: {@code "trigger": { "event": "...", "target": "self", "filters": {...} }}</li>
 * </ul>
 *
 * @param filter 可选的过滤器门控
 * @param event  要触发的事件名称
 * @param target 目标主体（self / other 等），默认 self
 * @author TT432
 */
public record Trigger(
        @Nullable Filter filter,
        String event,
        Subject target
) implements LogicNode {
    public static final Codec<Trigger> CODEC = Codec.either(
            // 简化形式: "trigger": "event_name"
            Codec.STRING,
            // 完整形式: "trigger": { "event": "...", "target": "...", "filters": {...} }
            RecordCodecBuilder.<Trigger>create(ins -> ins.group(
                    Filter.CODEC.optionalFieldOf("filters").forGetter(t -> Optional.ofNullable(t.filter)),
                    Codec.STRING.fieldOf("event").forGetter(Trigger::event),
                    Subject.CODEC.optionalFieldOf("target", Subject.self).forGetter(Trigger::target)
            ).apply(ins, (f, e, t) -> new Trigger(f.orElse(null), e, t)))
    ).xmap(
            either -> either.map(s -> new Trigger(null, s, Subject.self), Function.identity()),
            t -> t.filter == null && t.target == Subject.self ? Either.left(t.event) : Either.right(t)
    );

    @Override
    public void eval(EntityBehaviorData data) {
        // 检查 filter 门控
        if (filter != null && !filter.eval(data)) {
            return;
        }

        // 仅实现 self target 情况，跨实体 target 交由后续阶段实现
        data.getBehavior().ifPresent(b -> {
            LogicNode targetEvent = b.events().get(event);
            if (targetEvent != null) {
                targetEvent.eval(data);
            }
        });
    }
}
