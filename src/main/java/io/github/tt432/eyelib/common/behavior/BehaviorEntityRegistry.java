package io.github.tt432.eyelib.common.behavior;

import io.github.tt432.eyelib.behavior.BehaviorEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端行为实体定义的运行时注册表。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class BehaviorEntityRegistry {
    private static final Map<String, BehaviorEntity> ENTITIES = new ConcurrentHashMap<>();

    public static void replaceAll(Map<String, BehaviorEntity> entities) {
        ENTITIES.clear();
        ENTITIES.putAll(entities);
    }

    public static void put(String identifier, BehaviorEntity entity) {
        ENTITIES.put(identifier, entity);
    }

    @Nullable
    public static BehaviorEntity get(String identifier) {
        return ENTITIES.get(identifier);
    }

    public static Map<String, BehaviorEntity> all() {
        return Map.copyOf(new LinkedHashMap<>(ENTITIES));
    }

    public static void clear() {
        ENTITIES.clear();
    }
}
