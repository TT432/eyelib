package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationEffect;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：ONCE 剪辑自然结束后，其 keyframe 登记的粒子发射器必须经
 * {@link AnimationParticleSpawner#remove} 回收（此前登记无人处理，
 * looping 发射器永久泄漏）。LOOP 与 HOLD_ON_LAST_FRAME 不得移除。
 *
 * @author TT432
 */
class BrClipOnceEndParticleCleanupTest {

    @Test
    void onceClipEndRemovesRegisteredEmitters() {
        RecordingSpawner spawner = new RecordingSpawner();
        BrAnimationEntry entry = entry(BrLoopType.ONCE, 1.0F, "2.0");
        BrAnimationEntry.Data data = entry.createData();
        data.owner().particles().add(new RuntimeParticlePlayData("uuid-a", null, false, 0));
        data.owner().particles().add(new RuntimeParticlePlayData("uuid-b", null, false, 0));

        tick(entry, data, spawner);

        assertEquals(List.of("uuid-a", "uuid-b"), spawner.removed);
        assertTrue(data.owner().particles().isEmpty());

        // 结束后每帧重复 tick 不得重复 remove（登记已清空）
        tick(entry, data, spawner);
        assertEquals(2, spawner.removed.size());
    }

    @Test
    void onceClipBeforeEndKeepsEmitters() {
        RecordingSpawner spawner = new RecordingSpawner();
        BrAnimationEntry entry = entry(BrLoopType.ONCE, 1.0F, "0.5");
        BrAnimationEntry.Data data = entry.createData();
        data.owner().particles().add(new RuntimeParticlePlayData("uuid-a", null, false, 0));

        tick(entry, data, spawner);

        assertTrue(spawner.removed.isEmpty());
        assertEquals(1, data.owner().particles().size());
    }

    @Test
    void loopAndHoldClipsKeepEmittersAfterLength() {
        for (BrLoopType loop : new BrLoopType[]{BrLoopType.LOOP, BrLoopType.HOLD_ON_LAST_FRAME}) {
            RecordingSpawner spawner = new RecordingSpawner();
            BrAnimationEntry entry = entry(loop, 1.0F, "5.0");
            BrAnimationEntry.Data data = entry.createData();
            data.owner().particles().add(new RuntimeParticlePlayData("uuid-a", null, false, 0));

            tick(entry, data, spawner);

            assertTrue(spawner.removed.isEmpty(), loop + " 不得移除发射器");
            assertEquals(1, data.owner().particles().size(), loop + " 登记必须保留");
        }
    }

    private static void tick(BrAnimationEntry entry, BrAnimationEntry.Data data,
                             RecordingSpawner spawner) {
        MolangScope scope = new MolangScope();
        scope.getHostContext().put(HostRoles.ANIMATION_PARTICLE_SPAWNER, spawner);
        BrClipExecutor.tick(entry, data, Map.of(), scope, 1F, 1F,
                new ModelRuntimeData(), new AnimationEffects(), () -> {});
    }

    private static BrAnimationEntry entry(BrLoopType loop, float length, String animTimeUpdateExpr) {
        return new BrAnimationEntry(
                "test",
                loop,
                length,
                false,
                new MolangValue(animTimeUpdateExpr),
                MolangValue.ONE,
                null,
                null,
                AnimationEffect.empty(),
                AnimationEffect.empty(),
                AnimationEffect.empty(),
                new Int2ObjectOpenHashMap<>()
        );
    }

    private static final class RecordingSpawner implements AnimationParticleSpawner {
        final List<String> removed = new ArrayList<>();

        @Override
        public boolean spawn(String spawnId, String effectId, Vector3f position) { return true; }

        @Override
        public void updatePose(String spawnId, Matrix4fc pose) { }

        @Override
        public void remove(String spawnId) { removed.add(spawnId); }
    }
}
