package io.github.tt432.eyelibparticle.api;

/**
 * 粒子运行时适配器的字符串键控生成/移除请求端口。
 *
 * @author TT432
 */
public interface ParticleSpawnApi {
    /**
     * 应用一个粒子生成请求。
     *
     * @param request 字符串键控的粒子生成请求
     */
    void spawn(ParticleSpawnRequest request);

    /**
     * 通过字符串生成标识符移除一个已生成的粒子发射器。
     *
     * @param spawnId 字符串生成标识符
     */
    void remove(String spawnId);
}