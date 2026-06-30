package io.github.tt432.eyelib.util.repository;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 字符串键控的通用仓储端口。
 * 所有模块中基于字符串标识符的资源/定义存储都应实现此接口，以统一访问模式。
 *
 * @param <T> 值类型
 * @author TT432
 */
public interface Repository<T> {
    /**
     * 通过字符串标识符查找条目。
     *
     * @param id 字符串标识符
     * @return 对应条目，如果未注册则返回 {@code null}
     */
    @Nullable
    T get(String id);

    /**
     * 返回所有当前注册的条目。
     *
     * @return 以字符串为键的条目映射
     */
    Map<String, T> all();

    /**
     * 返回所有已注册的字符串标识符集合。
     */
    default Collection<String> names() {
        return all().keySet();
    }

    /**
     * 注册或替换一个条目。
     *
     * @param id    字符串标识符
     * @param value 条目值
     */
    void put(String id, T value);

    /**
     * 用提供的映射替换整个存储内容。
     *
     * @param replacement 以字符串为键的替换映射
     */
    void replaceAll(Map<String, ? extends T> replacement);

    /**
     * 批量注册或替换条目（合并语义：覆盖同 key，加入新 key）。
     * 默认逐条 {@link #put}；实现可覆盖以做单次 copy-on-write 合并与单次事件发布。
     *
     * @param entries 以字符串为键的条目映射
     */
    default void putAll(Map<String, ? extends T> entries) {
        entries.forEach(this::put);
    }

    /**
     * 清除所有条目。
     */
    void clear();
}
