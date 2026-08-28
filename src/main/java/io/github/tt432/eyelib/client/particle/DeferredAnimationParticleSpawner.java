package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.particle.api.ParticleSpawnApi;
import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 延迟粒子生成器（Opt19 并行 stage）：worker 线程期间把 spawn/updatePose/remove 记入队列，
 * join 后由渲染线程 {@link #drain()} 按录入序回放到真实 adapter。
 * 同帧 drain——粒子在实体之后的粒子相渲染，时序差异不可观察。
 * 向量/矩阵入队时防御性拷贝（调用方可能复用该实例）。
 *
 * @author TT432
 */
public final class DeferredAnimationParticleSpawner implements AnimationParticleSpawner {
    private sealed interface Op {
    }

    private record Spawn(String spawnId, String effectId, Vector3f position) implements Op {
    }

    private record UpdatePose(String spawnId, Matrix4f pose) implements Op {
    }

    private record Remove(String spawnId) implements Op {
    }

    private final ParticleSpawnApi target;
    private final List<Op> ops = new ArrayList<>();

    public DeferredAnimationParticleSpawner(ParticleSpawnApi target) {
        this.target = target;
    }

    @Override
    public boolean spawn(String spawnId, String effectId, Vector3f position) {
        ops.add(new Spawn(spawnId, effectId, new Vector3f(position)));
        return true;
    }

    @Override
    public void updatePose(String spawnId, Matrix4fc pose) {
        ops.add(new UpdatePose(spawnId, new Matrix4f(pose)));
    }

    @Override
    public void remove(String spawnId) {
        ops.add(new Remove(spawnId));
    }

    /** 是否无待回放操作（渲染线程 drain 前的快路径判定）。 */
    public boolean isEmpty() {
        return ops.isEmpty();
    }

    /**
     * 渲染线程调用：按录入序把队列回放到目标 adapter，然后清空（可重复 drain，幂等）。
     */
    public void drain() {
        for (Op op : ops) {
            if (op instanceof Spawn s) {
                target.spawn(new ParticleSpawnRequest(s.spawnId(), s.effectId(), s.position()));
            } else if (op instanceof UpdatePose u) {
                target.updatePose(u.spawnId(), u.pose());
            } else if (op instanceof Remove r) {
                target.remove(r.spawnId());
            }
        }
        ops.clear();
    }
}
