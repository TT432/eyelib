package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationEffect;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;
import io.github.tt432.eyelibmolang.MolangValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BrAnimationEntryLifecycleTest {
    @Test
    void onFinishResetsPlaybackTimeFields() {
        BrAnimationEntry entry = createEntry();
        BrAnimationEntry.Data data = entry.createData();

        data.owner().playbackState().tick(BrLoopType.LOOP, 2F, 5F, 5F);
        data.owner().syncStateFields();
        entry.onFinish(data);

        assertEquals(0, data.loopedTimes());
        assertEquals(0F, data.lastTicks(), 0.0001F);
        assertEquals(0F, data.animTime(), 0.0001F);
        assertEquals(0F, data.deltaTime(), 0.0001F);
    }

    @Test
    void onFinishClearsRuntimeParticles() {
        BrAnimationEntry entry = createEntry();
        BrAnimationEntry.Data data = entry.createData();

        data.owner().playbackState().tick(BrLoopType.LOOP, 2F, 5F, 5F);
        data.owner().syncStateFields();
        entry.onFinish(data);

        assertTrue(data.owner().particles().isEmpty());
    }

    @Test
    void onFinishPreservesEffects() {
        BrAnimationEntry entry = createEntry();
        BrAnimationEntry.Data data = entry.createData();

        data.owner().playbackState().tick(BrLoopType.LOOP, 2F, 5F, 5F);
        data.owner().syncStateFields();
        entry.onFinish(data);

        assertEquals(3, data.owner().effects().size());
    }

    private static BrAnimationEntry createEntry() {
        return new BrAnimationEntry(
                "animation.test.lifecycle",
                BrLoopType.LOOP,
                2F,
                false,
                MolangValue.ZERO,
                MolangValue.ONE,
                MolangValue.ZERO,
                MolangValue.ZERO,
                AnimationEffect.empty(),
                AnimationEffect.empty(),
                AnimationEffect.empty(),
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()
        );
    }
}
