package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.common.behavior.BehaviorEntity;
import io.github.tt432.eyelib.common.behavior.component.Component;
import io.github.tt432.eyelib.common.behavior.component.group.ComponentGroup;
import lombok.Getter;

import java.util.*;

/**
 * @author TT432
 */
public class EntityBehaviorData {
    public static final Codec<EntityBehaviorData> CODEC = Codec.unit(EntityBehaviorData::new);
    @Getter
    private Optional<BehaviorEntity> behavior = Optional.empty();
    @Getter
    private final List<ComponentGroup> componentGroups = new ArrayList<>();
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Component> T component(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }
}
