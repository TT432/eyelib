package io.github.tt432.eyelibbehavior.event.logic;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.EntityBehaviorData;

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
        for (LogicNode node : nodes) {
            node.eval(data);
        }
    }
}