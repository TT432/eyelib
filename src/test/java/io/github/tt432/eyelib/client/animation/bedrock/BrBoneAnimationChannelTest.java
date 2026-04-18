package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationDefinition;
import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneAnimationSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrBoneAnimationChannelTest {
    @Test
    void fromSchemaBuildsNamedChannelsAndKeepsLegacyAccessors() {
        BrBoneAnimation animation = BrBoneAnimation.fromSchema(new BrBoneAnimationSchema(
                keyframes(0F, MolangValue3.ZERO),
                keyframes(1F, MolangValue3.AXIS_X),
                keyframes(2F, MolangValue3.AXIS_Y)
        ));

        assertTrue(animation.channels().containsKey(BrBoneAnimation.ROTATION));
        assertTrue(animation.channels().containsKey(BrBoneAnimation.POSITION));
        assertTrue(animation.channels().containsKey(BrBoneAnimation.SCALE));
        assertEquals(animation.rotation(), animation.channel(BrBoneAnimation.ROTATION).keyFrames());
        assertEquals(animation.position(), animation.channel(BrBoneAnimation.POSITION).keyFrames());
        assertEquals(animation.scale(), animation.channel(BrBoneAnimation.SCALE).keyFrames());
    }

    @Test
    void sampleUsesChannelBackedKeyframesWithoutChangingLinearInterpolation() {
        BrBoneAnimation animation = BrBoneAnimation.fromSchema(new BrBoneAnimationSchema(
                keyframes(0F, vector(0F, 0F, 0F), 10F, vector(10F, 0F, 0F)),
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare)
        ));

        Vector3f sampled = animation.sample(BrBoneAnimation.ROTATION, new MolangScope(), 5F);

        assertNotNull(sampled);
        assertEquals(5F, sampled.x, 0.0001F);
        assertEquals(0F, sampled.y, 0.0001F);
        assertEquals(0F, sampled.z, 0.0001F);
    }

    @Test
    void unknownChannelFallsBackToEmptyDefinitionAndReturnsNull() {
        BrBoneAnimation animation = BrBoneAnimation.fromSchema(new BrBoneAnimationSchema(
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare)
        ));

        AnimationDefinition<BrBoneKeyFrameDefinition, BrAnimationChannel<BrBoneKeyFrameDefinition>> definition = animation.definition();

        assertTrue(definition.channel("missing").keyFrames().getData().isEmpty());
        assertNull(animation.sample("missing", new MolangScope(), 1F));
    }

    @Test
    void samplerKeepsSingleKeyframeBehaviorAtAndAfterTimestamp() {
        BrBoneAnimation animation = BrBoneAnimation.fromSchema(new BrBoneAnimationSchema(
                keyframes(2F, vector(2F, 3F, 4F)),
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare)
        ));

        Vector3f atTick = BrBoneAnimationSampler.sample(animation.definition(), BrBoneAnimation.ROTATION, new MolangScope(), 2F);
        Vector3f afterTick = BrBoneAnimationSampler.sample(animation.definition(), BrBoneAnimation.ROTATION, new MolangScope(), 5F);

        assertNotNull(atTick);
        assertNotNull(afterTick);
        assertEquals(2F, atTick.x, 0.0001F);
        assertEquals(2F, afterTick.x, 0.0001F);
    }

    @Test
    void samplerFallsBackToLinearWhenCatmullromNeighborsAreMissing() {
        TreeMap<Float, BrBoneKeyFrameSchema> rotation = new TreeMap<>(Float::compare);
        rotation.put(0F, new BrBoneKeyFrameSchema(List.of(vector(0F, 0F, 0F)), BrBoneKeyFrameSchema.LerpMode.CATMULLROM));
        rotation.put(10F, new BrBoneKeyFrameSchema(List.of(vector(10F, 0F, 0F)), BrBoneKeyFrameSchema.LerpMode.CATMULLROM));
        BrBoneAnimation animation = BrBoneAnimation.fromSchema(new BrBoneAnimationSchema(
                rotation,
                new TreeMap<>(Float::compare),
                new TreeMap<>(Float::compare)
        ));

        Vector3f sampled = BrBoneAnimationSampler.sample(animation.definition(), BrBoneAnimation.ROTATION, new MolangScope(), 5F);

        assertNotNull(sampled);
        assertEquals(5F, sampled.x, 0.0001F);
    }

    private static TreeMap<Float, BrBoneKeyFrameSchema> keyframes(float time, MolangValue3 value) {
        return keyframes(time, value, null, null);
    }

    private static TreeMap<Float, BrBoneKeyFrameSchema> keyframes(float firstTime, MolangValue3 firstValue,
                                                                  Float secondTime, MolangValue3 secondValue) {
        TreeMap<Float, BrBoneKeyFrameSchema> map = new TreeMap<>(Float::compare);
        map.put(firstTime, new BrBoneKeyFrameSchema(List.of(firstValue), BrBoneKeyFrameSchema.LerpMode.LINEAR));
        if (secondTime != null && secondValue != null) {
            map.put(secondTime, new BrBoneKeyFrameSchema(List.of(secondValue), BrBoneKeyFrameSchema.LerpMode.LINEAR));
        }
        return map;
    }

    private static MolangValue3 vector(float x, float y, float z) {
        return new MolangValue3(MolangValue.getConstant(x), MolangValue.getConstant(y), MolangValue.getConstant(z));
    }
}
