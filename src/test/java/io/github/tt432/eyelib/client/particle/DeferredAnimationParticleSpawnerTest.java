package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.particle.api.ParticleSpawnApi;
import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeferredAnimationParticleSpawner 契约（Opt19）：
 * 队列按录入序回放、参数防御性拷贝、spawn 返回值与直通实现一致（true）、drain 幂等。
 *
 * @author TT432
 */
class DeferredAnimationParticleSpawnerTest {

    private static final class RecordingApi implements ParticleSpawnApi {
        final List<String> log = new ArrayList<>();

        @Override
        public void spawn(ParticleSpawnRequest request) {
            log.add("spawn:" + request.spawnId() + ":" + request.particleId()
                    + ":" + request.position().x + "," + request.position().y + "," + request.position().z);
        }

        @Override
        public void updatePose(String spawnId, org.joml.Matrix4fc pose) {
            log.add("pose:" + spawnId + ":" + pose.m00());
        }

        @Override
        public void remove(String spawnId) {
            log.add("remove:" + spawnId);
        }
    }

    @Test
    void drainReplaysOpsInRecordOrder() {
        RecordingApi api = new RecordingApi();
        DeferredAnimationParticleSpawner spawner = new DeferredAnimationParticleSpawner(api);

        assertTrue(spawner.spawn("s1", "effect_a", new Vector3f(1, 2, 3)));
        spawner.updatePose("s1", new Matrix4f().scale(2));
        spawner.remove("s0");
        spawner.spawn("s2", "effect_b", new Vector3f(4, 5, 6));

        // drain 前目标零调用
        assertTrue(api.log.isEmpty());

        spawner.drain();
        assertEquals(List.of(
                "spawn:s1:effect_a:1.0,2.0,3.0",
                "pose:s1:2.0",
                "remove:s0",
                "spawn:s2:effect_b:4.0,5.0,6.0"), api.log);

        // drain 后清空：再次 drain 幂等无操作
        spawner.drain();
        assertEquals(4, api.log.size());
    }

    @Test
    void enqueuedArgumentsAreDefensivelyCopied() {
        RecordingApi api = new RecordingApi();
        DeferredAnimationParticleSpawner spawner = new DeferredAnimationParticleSpawner(api);

        Vector3f pos = new Vector3f(1, 1, 1);
        spawner.spawn("s1", "e", pos);
        pos.set(99, 99, 99); // 入队后调用方复用实例——不得影响已入队参数

        Matrix4f pose = new Matrix4f().scale(3);
        spawner.updatePose("s1", pose);
        pose.scale(10);

        spawner.drain();
        assertEquals(List.of("spawn:s1:e:1.0,1.0,1.0", "pose:s1:3.0"), api.log);
    }

    @Test
    void isEmptyReflectsQueueState() {
        RecordingApi api = new RecordingApi();
        DeferredAnimationParticleSpawner spawner = new DeferredAnimationParticleSpawner(api);
        assertTrue(spawner.isEmpty());
        spawner.remove("x");
        assertTrue(!spawner.isEmpty());
        spawner.drain();
        assertTrue(spawner.isEmpty());
    }
}
