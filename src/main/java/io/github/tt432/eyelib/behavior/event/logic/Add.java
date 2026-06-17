package io.github.tt432.eyelibbehavior.event.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.EntityBehaviorData;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;

import java.util.List;

/**
 * @author TT432
 */
public record Add(
        List<String> component_groups
) implements LogicNode {
    public static final Codec<Add> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().fieldOf("component_groups").forGetter(Add::component_groups)
    ).apply(ins, Add::new));

    @Override
    public void eval(EntityBehaviorData data) {
        if (data.getBehavior().isPresent()) {
            data.getComponentGroups().addAll(component_groups().stream().map(s -> data.getBehavior().map(b -> b.component_groups().get(s)).orElse(ComponentGroup.EMPTY)).toList());
        }
    }
}