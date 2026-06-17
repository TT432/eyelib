package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.importer.addon.BrSpawnRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 旧 spawn rule 查询入口；真实运行时数据由服务端行为包注册表维护。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class SpawnRuleRegistry {
    public static void register(String identifier, BrSpawnRule rule) {
        io.github.tt432.eyelib.common.behavior.SpawnRuleRegistry.register(identifier, rule);
    }

    public static void registerAll(Map<String, BrSpawnRule> rules) {
        io.github.tt432.eyelib.common.behavior.SpawnRuleRegistry.registerAll(rules);
    }

    @Nullable
    public static BrSpawnRule get(String identifier) {
        return io.github.tt432.eyelib.common.behavior.SpawnRuleRegistry.get(identifier);
    }

    public static void clear() {
        io.github.tt432.eyelib.common.behavior.SpawnRuleRegistry.clear();
    }

    public static Map<String, BrSpawnRule> allRules() {
        return io.github.tt432.eyelib.common.behavior.SpawnRuleRegistry.allRules();
    }
}
