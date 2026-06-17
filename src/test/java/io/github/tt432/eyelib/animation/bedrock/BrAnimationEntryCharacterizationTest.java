package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneAnimationSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.molang.MolangValue3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BrAnimationEntryCharacterizationTest {
    @Test
    void fromSchemaCompilesNamedBonesIntoRuntimeBoneIdsAndChannels() {
        BrAnimationEntry entry = BrAnimationEntry.fromSchema("animation.test.body", new BrAnimationEntrySchema(
                BrLoopType.LOOP,
                2F,
                false,
                io.github.tt432.eyelib.molang.MolangValue.ZERO,
                io.github.tt432.eyelib.molang.MolangValue.ONE,
                io.github.tt432.eyelib.molang.MolangValue.ZERO,
                io.github.tt432.eyelib.molang.MolangValue.ZERO,
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare),
                Map.of("Body", new BrBoneAnimationSchema(
                        keyframes(0F, MolangValue3.ZERO),
                        new TreeMap<>(Float::compare),
                        new TreeMap<>(Float::compare)
                ))
        ));

        int bodyId = GlobalBoneIdHandler.get("body");
        BrBoneAnimation bodyAnimation = entry.bones().get(bodyId);

        assertEquals("animation.test.body", entry.name());
        assertNotNull(bodyAnimation);
        assertTrue(bodyAnimation.channels().containsKey(BrBoneAnimation.ROTATION));
        assertNotNull(bodyAnimation.rotation().floorEntry(0F));
    }

    private static TreeMap<Float, BrBoneKeyFrameSchema> keyframes(float time, MolangValue3 value) {
        TreeMap<Float, BrBoneKeyFrameSchema> map = new TreeMap<>(Float::compare);
        map.put(time, new BrBoneKeyFrameSchema(List.of(value), BrBoneKeyFrameSchema.LerpMode.LINEAR));
        return map;
    }
}