package io.github.tt432.eyelibparticle.api;

/**
 * 粒子存储的窄生命周期/重置端口。
 *
 * @author TT432
 */
public interface ParticleLifecycle {
    /**
     * 清除所有当前已发布的粒子条目。
     */
    void clear();
}