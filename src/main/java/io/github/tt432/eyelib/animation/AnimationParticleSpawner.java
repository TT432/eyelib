package io.github.tt432.eyelib.animation;

import org.joml.Vector3f;
/**
 * 粒子生成接口，作为 animation 模块与 particle 模块的边界。
 * animation 侧通过此接口发射粒子，不接触 particle 内部类型。
 *
 * @author TT432
 */
public interface AnimationParticleSpawner {
    /**
     * 生成一个粒子效果。
     *
     * @param spawnId  唯一标识此次 spawn 的 ID（用于后续 remove）
     * @param effectId 粒子 shortname，由调用方从 client_entity 映射中获取
     * @param position 生成位置
     * @return true 表示请求已发送给 particle runtime（不一定代表成功 spawn）
     */
    boolean spawn(String spawnId, String effectId, Vector3f position);

    void remove(String spawnId);
}
