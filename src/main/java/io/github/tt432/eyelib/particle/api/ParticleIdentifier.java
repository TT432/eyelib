package io.github.tt432.eyelib.particle.api;

/**
 * 提取粒子定义的稳定字符串标识符。
 *
 * @param <T> 由消费方运行时适配器提供的粒子定义类型
 * @author TT432
 */
@FunctionalInterface
public interface ParticleIdentifier<T> {
    /**
     * 返回用于发布粒子定义的字符串标识符。
     *
     * @param particle 粒子定义
     * @return 稳定的字符串粒子标识符
     */
    String identify(T particle);
}