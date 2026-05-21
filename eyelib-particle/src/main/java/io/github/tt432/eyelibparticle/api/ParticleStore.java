package io.github.tt432.eyelibparticle.api;

import java.util.Map;

/**
 * 字符串键控的可变粒子存储端口。
 *
 * @param <T> 由消费方运行时适配器提供的粒子定义类型
 * @author TT432
 */
/** @author TT432 */
public interface ParticleStore<T> extends ParticleLookupApi<T>, ParticleLifecycle {
    /**
     * 在其字符串标识符下发布或替换单个粒子定义。
     *
     * @param id       字符串粒子标识符
     * @param particle 粒子定义
     */
    void put(String id, T particle);

    /**
     * 使用提供的以字符串标识符为键的粒子定义替换整个存储。
     *
     * @param replacement 以字符串为键的替换粒子定义
     */
    void replaceAll(Map<String, ? extends T> replacement);
}