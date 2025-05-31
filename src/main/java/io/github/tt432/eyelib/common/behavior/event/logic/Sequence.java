package io.github.tt432.eyelib.common.behavior.event.logic;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.capability.EntityBehaviorData;

import java.util.List;

/**
 * @author TT432
 */
public record Sequence(
        List<LogicNode> nodes
) implements LogicNode {
    public static final Codec<Sequence> CODEC = LogicNode.CODEC.codec().listOf().xmap(Sequence::new, Sequence::nodes);

    @Override
    public void eval(EntityBehaviorData data) {
        // 遍历 nodes 列表，依次执行每个逻辑节点的 eval 方法
        for (LogicNode node : nodes) {
            node.eval(data);
        }
    }
}
