package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.common.behavior.BehaviorEntity;
import io.github.tt432.eyelib.common.behavior.component.Component;
import io.github.tt432.eyelib.common.behavior.component.group.ComponentGroup;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import lombok.Getter;

import java.util.*;

/**
 * @author TT432
 */
public class EntityBehaviorData {
    public static final Codec<EntityBehaviorData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BehaviorEntity.CODEC.optionalFieldOf("behavior").forGetter(o -> o.behavior),
            ComponentGroup.CODEC.listOf().fieldOf("componentGroups").forGetter(o -> o.componentGroups)
    ).apply(ins, EntityBehaviorData::new));

    public static final StreamCodec<EntityBehaviorData> STREAM_CODEC = EyelibStreamCodecs.fromCodec(CODEC);

    @Getter
    private Optional<BehaviorEntity> behavior;
    @Getter
    private List<ComponentGroup> componentGroups;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public EntityBehaviorData(Optional<BehaviorEntity> behavior, List<ComponentGroup> componentGroups) {
        this.behavior = behavior;
        this.componentGroups = componentGroups;

        setup();
    }

    public void setup() {
        // todo 临时使用
        for (ComponentGroup componentGroup : componentGroups) {
            for (Map<String, Component> value : componentGroup.components().values()) {
                value.values().forEach(component -> components.put(component.getClass(), component));
            }
        }
    }

    public EntityBehaviorData() {
        this(Optional.empty(), new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T component(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    public <T extends Component> void component(Class<T> componentClass, T component) {
        components.put(componentClass, component);
    }
}
