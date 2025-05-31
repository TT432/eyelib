package io.github.tt432.eyelib.common.behavior.event.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.EntityBehaviorData;
import io.github.tt432.eyelib.common.behavior.component.group.ComponentGroup;

import java.util.List;

/**
 * @author TT432
 */
public record Remove(
        List<String> component_groups
) implements LogicNode {
    public static final Codec<Remove> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().fieldOf("component_groups").forGetter(Remove::component_groups)
    ).apply(ins, Remove::new));

    @Override
    public void eval(EntityBehaviorData data) {
        if (data.getBehavior().isPresent()) {
            data.getComponentGroups().removeAll(component_groups().stream().map(s -> data.getBehavior().map(b -> b.component_groups().get(s)).orElse(ComponentGroup.EMPTY)).toList());
        }
    }
}
