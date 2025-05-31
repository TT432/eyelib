package io.github.tt432.eyelib.common.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.common.behavior.component.group.ComponentGroup;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * @author TT432
 */
public record BehaviorEntity(
        ResourceLocation identifier,
        Map<String, ComponentGroup> component_groups
) {
    public static final Codec<BehaviorEntity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RecordCodecBuilder.<BehaviorEntity>create(instance2 -> instance2.group(
                    RecordCodecBuilder.<ResourceLocation>create(instance3 -> instance3.group(
                            ResourceLocation.CODEC.fieldOf("identifier").forGetter(r -> r)
                    ).apply(instance3, r -> r)).fieldOf("description").forGetter(BehaviorEntity::identifier),
                    Codec.unboundedMap(Codec.STRING, ComponentGroup.CODEC).fieldOf("component_groups").forGetter(BehaviorEntity::component_groups)
            ).apply(instance2, BehaviorEntity::new)).fieldOf("minecraft:entity").forGetter(e -> e)
    ).apply(instance, e -> e));
}
