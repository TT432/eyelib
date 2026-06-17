package io.github.tt432.eyelib.common.behavior;

import io.github.tt432.eyelib.importer.addon.BrSpawnRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端行为包生成规则注册表。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class SpawnRuleRegistry {
    private static final Map<String, BrSpawnRule> RULES = new ConcurrentHashMap<>();

    public static void register(String identifier, BrSpawnRule rule) {
        RULES.put(identifier, rule);
    }

    public static void registerAll(Map<String, BrSpawnRule> rules) {
        RULES.putAll(rules);
    }

    @Nullable
    public static BrSpawnRule get(String identifier) {
        return RULES.get(identifier);
    }

    public static void replaceAll(Map<String, BrSpawnRule> rules) {
        RULES.clear();
        RULES.putAll(rules);
    }

    public static void clear() {
        RULES.clear();
    }

    public static Map<String, BrSpawnRule> allRules() {
        return Map.copyOf(new LinkedHashMap<>(RULES));
    }
}
