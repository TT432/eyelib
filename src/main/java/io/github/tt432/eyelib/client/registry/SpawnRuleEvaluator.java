package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.addon.BrSpawnRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Spawn Rule 条件求值器（简单版）。
 * <p>
 * 当前仅支持 {@code minecraft:biome_filter} 条件，检查每个 filter 条目
 * 的 {@code test}、{@code operator}、{@code value} 字段是否与提供的生物群系信息匹配。
 * <p>
 * 支持的 test 类型：
 * <ul>
 *   <li>{@code has_biome_tag} — 检查生物群系是否包含指定标签</li>
 *   <li>{@code is_biome} — 检查生物群系 ID 是否匹配</li>
 * </ul>
 * <p>
 * 支持的 operator：{@code ==}（等于）、{@code !=}（不等于）。
 *
 * @author TT432
 */
public final class SpawnRuleEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnRuleEvaluator.class);

    private static final String BIOME_FILTER_KEY = "minecraft:biome_filter";

    private SpawnRuleEvaluator() {
    }

    /**
     * 判断给定的生成规则是否匹配当前生物群系。
     *
     * @param rule      生成规则
     * @param biomeId   当前生物群系的 identifier，例如 "minecraft:plains"
     * @param biomeTags 当前生物群系拥有的标签集合
     * @return 如果所有 biome_filter 条件都通过返回 {@code true}；
     *         若无 biome_filter 条件则默认返回 {@code true}（无约束视为匹配）
     */
    public static boolean matches(BrSpawnRule rule, String biomeId, Set<String> biomeTags) {
        Objects.requireNonNull(rule, "rule must not be null");
        Objects.requireNonNull(biomeId, "biomeId must not be null");
        Objects.requireNonNull(biomeTags, "biomeTags must not be null");

        for (var condition : rule.conditions()) {
            // 每个 condition 是一个 ObjectValue，形如：
            // { "minecraft:biome_filter": { ... } }
            BedrockResourceValue filterValue = condition.values().get(BIOME_FILTER_KEY);
            if (filterValue instanceof BedrockResourceValue.ObjectValue biomeFilter) {
                if (!evaluateBiomeFilter(biomeFilter, biomeId, biomeTags)) {
                    return false;
                }
            }
            // 忽略其他 condition 类型（weight、density_limit 等）
        }
        return true;
    }

    /**
     * 检查单个 minecraft:biome_filter ObjectValue 中的所有子条件是否全部通过。
     */
    private static boolean evaluateBiomeFilter(
            BedrockResourceValue.ObjectValue biomeFilter,
            String biomeId,
            Set<String> biomeTags
    ) {
        for (var entry : biomeFilter.values().entrySet()) {
            String testKey = entry.getKey();
            BedrockResourceValue testVal = entry.getValue();

            // 跳过非 ObjectValue 的条目
            if (!(testVal instanceof BedrockResourceValue.ObjectValue filterEntry)) {
                continue;
            }

            if (!evaluateSingleFilter(testKey, filterEntry, biomeId, biomeTags)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 求值单个 filter 条目。
     * <p>
     * filter 条目的结构：{ "test": "...", "operator": "...", "value": "..." }
     */
    private static boolean evaluateSingleFilter(
            String testKey,
            BedrockResourceValue.ObjectValue filterEntry,
            String biomeId,
            Set<String> biomeTags
    ) {
        String test = extractString(filterEntry, "test");
        String operator = extractString(filterEntry, "operator");
        String value = extractString(filterEntry, "value");

        if (test == null || value == null) {
            LOGGER.debug("Skipping invalid filter entry (missing test/value) in condition {}", testKey);
            return true; // 跳过格式不完整条目
        }

        // 默认 operator 为 "=="
        boolean isEquality = operator == null || "==".equals(operator);
        boolean isInequality = "!=".equals(operator);

        return switch (test) {
            case "has_biome_tag" -> evaluateHasBiomeTag(isEquality, isInequality, value, biomeTags);
            case "is_biome" -> evaluateIsBiome(isEquality, isInequality, value, biomeId);
            default -> {
                // 未知 test 类型，跳过
                LOGGER.debug("Unknown biome filter test type: {}", test);
                yield true;
            }
        };
    }

    private static boolean evaluateHasBiomeTag(
            boolean isEquality, boolean isInequality,
            String tag, Set<String> biomeTags
    ) {
        boolean hasTag = biomeTags.contains(tag);
        if (isEquality) {
            return hasTag;
        } else if (isInequality) {
            return !hasTag;
        }
        // 未知 operator，跳过
        return true;
    }

    private static boolean evaluateIsBiome(
            boolean isEquality, boolean isInequality,
            String targetBiomeId, String currentBiomeId
    ) {
        boolean match = targetBiomeId.equals(currentBiomeId);
        if (isEquality) {
            return match;
        } else if (isInequality) {
            return !match;
        }
        return true;
    }

    @org.jspecify.annotations.Nullable
    private static String extractString(BedrockResourceValue.ObjectValue obj, String key) {
        BedrockResourceValue val = obj.values().get(key);
        if (val instanceof BedrockResourceValue.StringValue sv) {
            return sv.value();
        }
        return null;
    }
}
