package io.github.tt432.eyelib.bridge.molang.adapter;

import io.github.tt432.eyelib.util.PortResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * {@code query.is_item_name_any} 的名字匹配纯逻辑（不触 MC 类型，可离线单测）。
 *
 * <p>语义按 Bedrock 规范：候选名为 {@code namespace:path} 形式（省略命名空间补
 * {@code minecraft:}、大小写不敏感），空串候选 {@code ''} 只匹配空槽。
 *
 * @author TT432
 */
public final class ItemNameMatcher {
    private ItemNameMatcher() {
    }

    /**
     * 单个候选名匹配。
     *
     * @param key       槽位物品的注册名（{@code namespace:path}）；{@code null} 表示空槽
     * @param candidate molang 传入的候选名
     */
    public static boolean matches(@Nullable String key, String candidate) {
        if (key == null) {
            // BE: '' 用于检查空槽；空槽不会命中任何具体名字（命名空间省略形也不会）
            return candidate.isEmpty();
        }
        if (candidate.isEmpty()) {
            return false;
        }
        return key.equals(normalize(candidate));
    }

    /** 任一候选名匹配（{@code query.is_item_name_any} 的多名字形）。 */
    public static boolean matchesAny(@Nullable String key, String... candidates) {
        for (String candidate : candidates) {
            if (matches(key, candidate)) {
                return true;
            }
        }
        return false;
    }

    /** 归一化为注册名形式：小写 + 缺省命名空间补 {@code minecraft:}。 */
    public static String normalize(String candidate) {
        return PortResourceLocation.parse(candidate.toLowerCase(Locale.ROOT)).toString();
    }
}
