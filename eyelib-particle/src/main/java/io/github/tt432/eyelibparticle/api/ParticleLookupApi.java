package io.github.tt432.eyelibparticle.api;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 粒子模块边界拥有的字符串键控粒子定义读取端口。
 *
 * @param <T> 由消费方运行时适配器提供的粒子定义类型
 * @author TT432
 */
public interface ParticleLookupApi<T> {
    /**
     * 通过字符串标识符查找粒子定义。
     *
     * @param id 字符串粒子标识符
     * @return 粒子定义，如果 {@code id} 没有注册条目则返回 {@code null}
     */
    @Nullable
    T get(String id);

    /**
     * 返回所有当前注册的粒子定义，以字符串标识符为键。
     *
     * @return 以字符串为键的粒子定义映射
     */
    Map<String, T> all();

    /**
     * 返回已注册的字符串标识符集合。
     *
     * @return 已注册的粒子标识符集合
     */
    default Collection<String> names() {
        return all().keySet();
    }
}