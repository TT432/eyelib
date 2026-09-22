package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.AnimationEffect;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：条目 keyframe 触发的粒子登记在嵌套 {@link BrAnimationEntry.Data} 上，
 * 控制器级清理（模型切换/实体离场/资源重载的 drain）必须能触达它们，
 * 否则 looping 发射器永久失联（2026-09-21 换模型后粒子残留实证）。
 *
 * @author TT432
 */
class BrAnimationControllerParticleDrainTest {

    private static BrAcState state(String animationName) {
        return new BrAcState(
                Map.of("slot.main", MolangValue.ONE),
                MolangValue.ZERO,
                MolangValue.ZERO,
                List.of(),
                List.of(),
                Map.of(),
                0F,
                false
        );
    }

    private static BrAnimationEntry childEntry(String name) {
        return new BrAnimationEntry(name, BrLoopType.ONCE, 1F, false,
                MolangValue.ONE, MolangValue.ONE, null, null,
                AnimationEffect.empty(), AnimationEffect.empty(), AnimationEffect.empty(),
                new Int2ObjectOpenHashMap<>());
    }

    @Test
    void drainAllParticlesReachesNestedEntryRegistrations() {
        BrAnimationEntry child = childEntry("animation.test.child");
        BrAnimationController controller = new BrAnimationController(
                "controller.animation.test", state("slot.main"), Map.of("default", state("slot.main")));
        BrAnimationController.Data data = controller.createData();

        RuntimeParticlePlayData stateLevel = new RuntimeParticlePlayData("uuid-state", null, true, 0F);
        data.particles().add(stateLevel);

        Object nestedData = data.getData(child);
        RuntimeParticlePlayData entryLevel = new RuntimeParticlePlayData("uuid-entry", null, true, 0F);
        ((BrAnimationEntry.Data) nestedData).particles().add(entryLevel);

        List<RuntimeParticlePlayData> drained = data.drainAllParticles();

        assertEquals(2, drained.size());
        assertTrue(drained.contains(stateLevel));
        assertTrue(drained.contains(entryLevel));
        // drain 后源头清空：重复 drain 不再返回（避免重复 remove / 假孤儿）
        assertTrue(data.drainAllParticles().isEmpty());
    }
}
