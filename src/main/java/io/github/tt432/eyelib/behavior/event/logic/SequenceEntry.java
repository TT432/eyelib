package io.github.tt432.eyelibbehavior.event.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.EntityBehaviorData;
import io.github.tt432.eyelibbehavior.event.filter.Filter;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Sequence 的中间层记录，每个条目可附带可选的 filters 门控。
 * <p>
 * Bedrock 标准中 sequence 的每个条目是一个带可选 filters 的容器，
 * 而非裸 LogicNode。SequenceEntry 负责在 filters 通过时才执行内部的 node。
 *
 * @param filter 可选的过滤器门控，为 null 时无条件执行
 * @param node   要执行的逻辑节点
 * @author TT432
 */
public record SequenceEntry(
        @Nullable Filter filter,
        LogicNode node
) {
    public static final Codec<SequenceEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Filter.CODEC.optionalFieldOf("filters").forGetter(e -> Optional.ofNullable(e.filter)),
            LogicNode.CODEC.forGetter(e -> e.node)
    ).apply(ins, (f, n) -> new SequenceEntry(f.orElse(null), n)));

    /**
     * 执行当前条目，若 filter 通过或不存在则执行内部节点。
     *
     * @param data 实体行为数据
     */
    public void eval(EntityBehaviorData data) {
        if (filter == null || filter.eval(data)) {
            node.eval(data);
        }
    }
}
