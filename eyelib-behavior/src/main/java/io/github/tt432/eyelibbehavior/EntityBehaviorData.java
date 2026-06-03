package io.github.tt432.eyelibbehavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.BehaviorEntity;
import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import io.github.tt432.eyelibutil.streamcodec.EyelibStreamCodecs;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 实体行为数据，管理实体行为描述与组件。
 *
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
        components.clear();
        // 1. 从 component_groups 收集
        for (ComponentGroup componentGroup : componentGroups) {
            for (Map<String, Component> value : componentGroup.components().values()) {
                value.values().forEach(component -> components.put(component.getClass(), component));
            }
        }
        // 2. 从 behavior 的顶层 components 收集（不覆盖，component_groups 优先级更高）
        behavior.ifPresent(b -> {
            for (Component component : b.components().components().values()) {
                components.putIfAbsent(component.getClass(), component);
            }
        });
    }

    public EntityBehaviorData() {
        this(Optional.empty(), new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Component> T component(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    public <T extends Component> void component(Class<T> componentClass, T component) {
        components.put(componentClass, component);
    }
}