package io.github.tt432.eyelib.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.group.ComponentGroup;
import io.github.tt432.eyelib.behavior.event.logic.LogicNode;
import io.github.tt432.eyelib.util.PortResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * @author TT432
 */
public record BehaviorEntity(
        PortResourceLocation identifier,
        Map<String, ComponentGroup> component_groups,
        BehaviorComponents components,
        Map<String, LogicNode> events
) {
    public static final Codec<BehaviorEntity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RecordCodecBuilder.<BehaviorEntity>create(instance2 -> instance2.group(
                    RecordCodecBuilder.<PortResourceLocation>create(instance3 -> instance3.group(
                            Codec.STRING.xmap(PortResourceLocation::parse, PortResourceLocation::toString).fieldOf("identifier").forGetter(r -> r)
                    ).apply(instance3, r -> r)).fieldOf("description").forGetter(BehaviorEntity::identifier),
                    Codec.unboundedMap(Codec.STRING, ComponentGroup.CODEC).optionalFieldOf("component_groups", java.util.Collections.emptyMap()).forGetter(BehaviorEntity::component_groups),
                    BehaviorComponents.CODEC.optionalFieldOf("components", BehaviorComponents.EMPTY).forGetter(BehaviorEntity::components),
                    Codec.unboundedMap(Codec.STRING, LogicNode.CODEC.codec()).optionalFieldOf("events", Collections.emptyMap()).forGetter(BehaviorEntity::events)
            ).apply(instance2, BehaviorEntity::new)).fieldOf("minecraft:entity").forGetter(e -> e)
    ).apply(instance, e -> e));
}