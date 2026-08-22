package io.github.tt432.eyelib.behavior.event.logic;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.event.filter.Filter;
import io.github.tt432.eyelib.behavior.event.filter.Subject;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    /**
     * trigger 递归深度上限：addon 可手写环形 trigger（A→B→A），不加保护会在 spawn 求值时
     * StackOverflowError。事件树求值始终在单线程内完成，用 ThreadLocal 计数即可。
     */
    private static final int MAX_TRIGGER_DEPTH = 32;
    private static final ThreadLocal<int[]> TRIGGER_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    @Override
    public void eval(EntityBehaviorData data) {
        // 检查 filter 门控
        if (filter != null && !filter.eval(data)) {
            return;
        }

        int[] depth = TRIGGER_DEPTH.get();
        if (depth[0] >= MAX_TRIGGER_DEPTH) {
            log.warn("Trigger event '{}' exceeded max recursion depth {}, possible cyclic trigger chain", event, MAX_TRIGGER_DEPTH);
            return;
        }

        // 仅实现 self target 情况，跨实体 target 交由后续阶段实现
        depth[0]++;
        try {
            data.getBehavior().ifPresent(b -> {
                LogicNode targetEvent = b.events().get(event);
                if (targetEvent != null) {
                    targetEvent.eval(data);
                }
            });
        } finally {
            depth[0]--;
        }
    }
}
