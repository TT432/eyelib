package io.github.tt432.eyelib.particle.api;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * 将粒子定义扁平化到字符串键控的 {@link ParticleStore} 中的发布接缝。
 *
 * @param <T> 由消费方运行时适配器提供的粒子定义类型
 * @author TT432
 */
public final class ParticlePublisher<T> {
    private final ParticleStore<T> store;
    private final ParticleIdentifier<? super T> identifier;

    /**
     * 创建一个由指定的存储和标识符提取器支持的发布器。
     *
     * @param store      要发布到的粒子存储
     * @param identifier 提取每个粒子的规范字符串标识符
     */
    public ParticlePublisher(ParticleStore<T> store, ParticleIdentifier<? super T> identifier) {
        this.store = Objects.requireNonNull(store, "store");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * 在标识符提取器提供的标识符下发布单个粒子。
     *
     * @param particle 要发布的粒子定义
     */
    public void publishParticle(T particle) {
        T checkedParticle = Objects.requireNonNull(particle, "particle");
        store.put(identify(checkedParticle), checkedParticle);
    }

    /**
     * 使用标识符提取器提供的标识符替换所有粒子。
     * 迭代顺序通过 {@link LinkedHashMap} 保持，因此替换顺序保持稳定。
     *
     * @param particles 要发布的粒子定义
     */
    public void replaceParticles(Iterable<? extends T> particles) {
        Objects.requireNonNull(particles, "particles");
        LinkedHashMap<String, T> replacement = new LinkedHashMap<>();
        for (T particle : particles) {
            T checkedParticle = Objects.requireNonNull(particle, "particle");
            replacement.put(identify(checkedParticle), checkedParticle);
        }
        store.replaceAll(replacement);
    }

    private String identify(T particle) {
        return Objects.requireNonNull(identifier.identify(particle), "particle identifier");
    }
}