package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelibimporter.addon.BrSpawnRule;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawn Rule 运行时注册表，按 entity identifier 索引已解析的 BrSpawnRule。
 * <p>
 * 在 addon loader 流程中由 {@code BedrockAddonRuntimeBridge} 填充，
 * 供运行时查询某个实体的生成规则。
 *
 * @author TT432
 */
@NullMarked
public final class SpawnRuleRegistry {
    private static final Map<String, BrSpawnRule> RULES = new ConcurrentHashMap<>();

    private SpawnRuleRegistry() {
    }

    /**
     * 注册一条生成规则。
     *
     * @param identifier 实体 identifier，例如 "minecraft:zombie"
     * @param rule       已解析的生成规则
     */
    public static void register(String identifier, BrSpawnRule rule) {
        RULES.put(identifier, rule);
    }

    /**
     * 批量注册生成规则。
     *
     * @param rules 实体 identifier → BrSpawnRule 映射
     */
    public static void registerAll(Map<String, BrSpawnRule> rules) {
        RULES.putAll(rules);
    }

    /**
     * 获取指定实体的生成规则。
     *
     * @param identifier 实体 identifier
     * @return 生成规则，若不存在返回 {@code null}
     */
    @Nullable
    public static BrSpawnRule get(String identifier) {
        return RULES.get(identifier);
    }

    /**
     * 清空注册表，通常在资源重载时调用。
     */
    public static void clear() {
        RULES.clear();
    }

    /**
     * 返回当前注册表中所有规则的快照。
     *
     * @return 不可修改的映射视图
     */
    public static Map<String, BrSpawnRule> allRules() {
        return Map.copyOf(RULES);
    }
}
