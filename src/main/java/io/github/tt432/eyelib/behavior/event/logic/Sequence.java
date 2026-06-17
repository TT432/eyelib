package io.github.tt432.eyelib.behavior.event.logic;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;

import java.util.List;

/**
 * @author TT432
 */
public record Sequence(
        List<SequenceEntry> entries
) implements LogicNode {
    public static final Codec<Sequence> CODEC = SequenceEntry.CODEC.listOf().xmap(Sequence::new, Sequence::entries);

    @Override
    public void eval(EntityBehaviorData data) {
        for (SequenceEntry entry : entries) {
            entry.eval(data);
        }
    }
}