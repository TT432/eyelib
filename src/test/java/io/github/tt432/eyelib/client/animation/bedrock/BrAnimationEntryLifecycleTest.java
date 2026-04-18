package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;
import io.github.tt432.eyelibmolang.MolangValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrAnimationEntryLifecycleTest {
    @Test
    void onFinishResetsPlaybackFieldsAndClearsRuntimeParticles() {
        BrAnimationEntry entry = new BrAnimationEntry(
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
        BrAnimationEntry.Data data = entry.createData();

        data.owner().playbackState().tick(BrLoopType.LOOP, 2F, 5F, 5F);
        data.owner().syncStateFields();
        entry.onFinish(data);

        assertEquals(0, data.loopedTimes());
        assertEquals(0F, data.lastTicks(), 0.0001F);
        assertEquals(0F, data.animTime(), 0.0001F);
        assertEquals(0F, data.deltaTime(), 0.0001F);
        assertTrue(data.owner().particles().isEmpty());
        assertEquals(3, data.owner().effects().size());
    }
}
