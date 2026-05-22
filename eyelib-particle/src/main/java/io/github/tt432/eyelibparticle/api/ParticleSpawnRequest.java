package io.github.tt432.eyelibparticle.api;

import org.joml.Vector3f;

import java.util.Objects;

/**
 * 由运行时适配器消费的字符串键控粒子生成请求。
 *
 * @param spawnId    已生成发射器实例的字符串标识符
 * @param particleId 字符串粒子定义标识符
 * @param position   生成位置，输入和输出时均进行防御性拷贝
 * @author TT432
 */
public record ParticleSpawnRequest(String spawnId, String particleId, Vector3f position) {
    /**
     * 使用非空字符串标识符和防御性拷贝的位置创建生成请求。
     */
    public ParticleSpawnRequest {
        spawnId = Objects.requireNonNull(spawnId, "spawnId");
        particleId = Objects.requireNonNull(particleId, "particleId");
        position = new Vector3f(Objects.requireNonNull(position, "position"));
    }

    @Override
    public Vector3f position() {
        return new Vector3f(position);
    }
}