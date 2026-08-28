package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 采样优化契约（fused floor/higher 二分 + 常量关键帧短路）：
 * 优化路径必须与旧路径（逐次 floorEntry/higherEntry/lowerEntry + 逐轴 molang 求值）
 * 在所有关键帧布局与采样点上结果一致；非常量（this 引用）关键帧不得被短路。
 *
 * @author TT432
 */
class BrBoneAnimationSamplerOptTest {

    // ---------------------------------------------------------------------
    // ImmutableFloatTreeMap.floorHigherIndices：与逐次 floorEntry/higherEntry 等价
    // ---------------------------------------------------------------------

    @Test
    void floorHigherIndicesMatchSequentialLookups() {
        float[][] keySets = {
                {0F},
                {0F, 10F},
                {0F, 10F, 20F, 30F, 40F},
                {0.5F, 1.25F, 2F, 3.75F},
        };
        float[] ticks = {-1F, 0F, 0.25F, 0.5F, 1F, 1.25F, 5F, 9.9F, 10F, 15F, 19.9F, 20F, 25F, 40F, 41F, 100F};

        for (float[] keys : keySets) {
            Float2ObjectOpenHashMap<String> data = new Float2ObjectOpenHashMap<>();
            for (float k : keys) {
                data.put(k, "v" + k);
            }
            ImmutableFloatTreeMap<String> map = ImmutableFloatTreeMap.of(keys, data);

            for (float tick : ticks) {
                long span = map.floorHigherIndices(tick);
                int floorIndex = (int) (span >> 32);
                int higherIndex = (int) span;
                String fusedFloor = floorIndex >= 0 ? map.valueAt(floorIndex) : null;
                String fusedHigher = higherIndex >= 0 ? map.valueAt(higherIndex) : null;

                assertEquals(map.floorEntry(tick), fusedFloor,
                        "floor mismatch keys=" + java.util.Arrays.toString(keys) + " tick=" + tick);
                assertEquals(map.higherEntry(tick), fusedHigher,
                        "higher mismatch keys=" + java.util.Arrays.toString(keys) + " tick=" + tick);
            }
        }
    }

    // ---------------------------------------------------------------------
    // 常量关键帧：优化采样结果 == 旧算法参考实现
    // ---------------------------------------------------------------------

    @Test
    void constantLinearChannelMatchesLegacyReference() {
        float[] keys = {0F, 10F, 20F, 30F};
        ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames = frames(keys, BrBoneKeyFrame.LerpMode.LINEAR, i -> vector(i * 2F, i * -1F, 5F));

        for (float tick = -1F; tick <= 35F; tick += 0.5F) {
            Vector3f expected = legacyReference(frames, tick);
            Vector3f actual = BrBoneAnimationSampler.lerp(new MolangScope(), frames, tick, 0F, 0F, 0F);
            assertVectorEquals(expected, actual, "tick=" + tick);
        }
    }

    @Test
    void constantCatmullromChannelMatchesLegacyReference() {
        float[] keys = {0F, 10F, 20F, 30F, 40F};
        ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames = frames(keys, BrBoneKeyFrame.LerpMode.CATMULLROM, i -> vector(i * 3F, i, i * -2F));

        for (float tick = -1F; tick <= 45F; tick += 0.25F) {
            Vector3f expected = legacyReference(frames, tick);
            Vector3f actual = BrBoneAnimationSampler.lerp(new MolangScope(), frames, tick, 0F, 0F, 0F);
            assertVectorEquals(expected, actual, "tick=" + tick);
        }
    }

    @Test
    void mixedConstantAndDynamicChannelMatchesLegacyReference() {
        // 常量帧与 this 引用帧混合：动态帧必须走逐轴求值，常量帧被短路，整体结果不变
        Float2ObjectOpenHashMap<BrBoneKeyFrameDefinition> data = new Float2ObjectOpenHashMap<>();
        data.put(0F, frame(0F, BrBoneKeyFrame.LerpMode.LINEAR, vector(0F, 0F, 0F)));
        data.put(10F, frame(10F, BrBoneKeyFrame.LerpMode.LINEAR, dynamic("this + 10")));
        data.put(20F, frame(20F, BrBoneKeyFrame.LerpMode.LINEAR, vector(4F, 4F, 4F)));
        ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames = ImmutableFloatTreeMap.of(new float[]{0F, 10F, 20F}, data);

        for (float tick = 0F; tick <= 25F; tick += 0.5F) {
            Vector3f expected = legacyReference(frames, tick, 1F, 2F, 3F);
            Vector3f actual = BrBoneAnimationSampler.lerp(new MolangScope(), frames, tick, 1F, 2F, 3F);
            assertVectorEquals(expected, actual, "tick=" + tick);
        }
    }

    // ---------------------------------------------------------------------
    // this 引用关键帧：非常量，不得被常量短路
    // ---------------------------------------------------------------------

    @Test
    void thisReferencingKeyframeUsesRuntimeThisValue() {
        ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames =
                frames(new float[]{0F}, BrBoneKeyFrame.LerpMode.LINEAR, i -> dynamic("this + 10"));

        Vector3f sampled = BrBoneAnimationSampler.lerp(new MolangScope(), frames, 0F, 1F, 2F, 3F);
        assertNotNull(sampled);
        assertEquals(11F, sampled.x, 0.0001F);
        assertEquals(12F, sampled.y, 0.0001F);
        assertEquals(13F, sampled.z, 0.0001F);
    }

    @Test
    void singleSideConstantReturnsIndependentCopy() {
        ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames =
                frames(new float[]{0F}, BrBoneKeyFrame.LerpMode.LINEAR, i -> vector(7F, 8F, 9F));

        Vector3f first = BrBoneAnimationSampler.lerp(new MolangScope(), frames, 5F, 0F, 0F, 0F);
        assertNotNull(first);
        first.x = -999F;

        Vector3f second = BrBoneAnimationSampler.lerp(new MolangScope(), frames, 6F, 0F, 0F, 0F);
        assertNotNull(second);
        assertNotSame(first, second);
        assertEquals(7F, second.x, 0.0001F);
    }

    @Test
    void emptyFramesReturnNull() {
        assertNull(BrBoneAnimationSampler.lerp(new MolangScope(), ImmutableFloatTreeMap.empty(), 0F, 0F, 0F, 0F));
    }

    // ---------------------------------------------------------------------
    // 参考实现：复刻旧路径（逐次查找 + BrBoneKeyFrame 逐轴求值），作为优化路径的 oracle
    // ---------------------------------------------------------------------

    private static Vector3f legacyReference(ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames, float tick) {
        return legacyReference(frames, tick, 0F, 0F, 0F);
    }

    private static Vector3f legacyReference(ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames, float tick,
                                            float thisX, float thisY, float thisZ) {
        MolangScope scope = new MolangScope();
        BrBoneKeyFrameDefinition before = frames.floorEntry(tick);
        BrBoneKeyFrameDefinition after = frames.higherEntry(tick);

        if (before != null && after != null) {
            float weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), tick);
            boolean linear = before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR
                    && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR;
            if (linear) {
                return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
            }
            BrBoneKeyFrameDefinition beforePlus = frames.lowerEntry(before.timestamp());
            BrBoneKeyFrameDefinition afterPlus = frames.higherEntry(after.timestamp());
            if (beforePlus == null || afterPlus == null) {
                return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
            }
            return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight, thisX, thisY, thisZ);
        } else if (before != null) {
            return BrBoneKeyFrame.getValue(before, before.timestamp() >= tick).evalWithThis(scope, thisX, thisY, thisZ);
        } else if (after != null) {
            return BrBoneKeyFrame.getValue(after, after.timestamp() >= tick).evalWithThis(scope, thisX, thisY, thisZ);
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // 构造工具
    // ---------------------------------------------------------------------

    private interface FrameFactory {
        MolangValue3 at(int index);
    }

    private static ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames(float[] keys, BrBoneKeyFrame.LerpMode mode,
                                                                          FrameFactory factory) {
        Float2ObjectOpenHashMap<BrBoneKeyFrameDefinition> data = new Float2ObjectOpenHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            data.put(keys[i], frame(keys[i], mode, factory.at(i)));
        }
        return ImmutableFloatTreeMap.of(keys, data);
    }

    private static BrBoneKeyFrameDefinition frame(float timestamp, BrBoneKeyFrame.LerpMode mode, MolangValue3 value) {
        return new BrBoneKeyFrameDefinition(timestamp, List.of(value), mode);
    }

    private static MolangValue3 vector(float x, float y, float z) {
        return new MolangValue3(MolangValue.getConstant(x), MolangValue.getConstant(y), MolangValue.getConstant(z));
    }

    private static MolangValue3 dynamic(String expression) {
        MolangValue value = new MolangValue(expression);
        return new MolangValue3(value, value, value);
    }

    private static void assertVectorEquals(Vector3f expected, Vector3f actual, String message) {
        if (expected == null) {
            assertNull(actual, message);
            return;
        }
        assertNotNull(actual, message);
        assertEquals(expected.x, actual.x, 0.0001F, message + " x");
        assertEquals(expected.y, actual.y, 0.0001F, message + " y");
        assertEquals(expected.z, actual.z, 0.0001F, message + " z");
    }
}
