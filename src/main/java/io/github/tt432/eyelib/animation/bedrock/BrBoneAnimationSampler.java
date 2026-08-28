package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 骨骼关键帧采样器，对定义数据进行线性或 Catmull-Rom 插值。
 *
 * @author TT432
 */
public final class BrBoneAnimationSampler {
    /**
     * 诊断开关（benchmark A/B 用）：-Deyelib.anim.sampleOpt=false 回退
     * 逐次二分 + 逐轴 molang 求值旧路径。
     */
    static final boolean SAMPLE_OPT =
            Boolean.parseBoolean(System.getProperty("eyelib.anim.sampleOpt", "true"));

    private BrBoneAnimationSampler() {
    }

    @Nullable
    public static Vector3f sample(BrBoneAnimationDefinition definition, String channelName, MolangScope scope, float currentTick,
                                  float thisX, float thisY, float thisZ) {
        return lerp(scope, definition.channel(channelName).keyFrames(), currentTick, thisX, thisY, thisZ);
    }
    /** 经已解析通道采样：跳过 channel(name) 的字符串键查找（热路径，见 BrBoneAnimation 预解析字段）。 */
    @Nullable
    public static Vector3f sample(BrAnimationChannel<BrBoneKeyFrameDefinition> channel, MolangScope scope, float currentTick,
                                  float thisX, float thisY, float thisZ) {
        return lerp(scope, channel.keyFrames(), currentTick, thisX, thisY, thisZ);
    }

    @Nullable
    public static Vector3f lerp(MolangScope scope,
                                ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames,
                                float currentTick,
                                float thisX, float thisY, float thisZ) {
        if (!SAMPLE_OPT) {
            return lerpLegacy(scope, frames, currentTick, thisX, thisY, thisZ);
        }

        // 单次二分同时定位 floor/higher（与 floorEntry+higherEntry 逐次调用语义等价，
        // 见 ImmutableFloatTreeMap.floorHigherIndices），索引直取免除哈希查找
        long span = frames.floorHigherIndices(currentTick);
        int floorIndex = (int) (span >> 32);
        int higherIndex = (int) span;
        BrBoneKeyFrameDefinition before = floorIndex >= 0 ? frames.valueAt(floorIndex) : null;
        BrBoneKeyFrameDefinition after = higherIndex >= 0 ? frames.valueAt(higherIndex) : null;

        if (before != null && after != null) {
            var weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                // 构造不变量：before.timestamp()==sortedKeys[floorIndex]、after.timestamp()==sortedKeys[higherIndex]
                // （lowerEntry(命中键)=索引-1，higherEntry(命中键)=索引+1），邻接点直接索引取值；
                // 不变量破缺时回退逐次查找（防御分支，正常不可达）
                float[] keys = frames.getSortedKeys();
                BrBoneKeyFrameDefinition beforePlus;
                BrBoneKeyFrameDefinition afterPlus;
                if (before.timestamp() == keys[floorIndex] && after.timestamp() == keys[higherIndex]) {
                    beforePlus = floorIndex > 0 ? frames.valueAt(floorIndex - 1) : null;
                    afterPlus = higherIndex + 1 < keys.length ? frames.valueAt(higherIndex + 1) : null;
                } else {
                    beforePlus = frames.lowerEntry(before.timestamp());
                    afterPlus = frames.higherEntry(after.timestamp());
                }

                if (beforePlus == null || afterPlus == null) {
                    return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
                }
                return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight, thisX, thisY, thisZ);
            }
        } else if (before != null) {
            return evalSingleSide(scope, before, before.timestamp() >= currentTick, thisX, thisY, thisZ);
        } else if (after != null) {
            return evalSingleSide(scope, after, after.timestamp() >= currentTick, thisX, thisY, thisZ);
        }

        return null;
    }

    /**
     * 单侧分支：常量关键帧直接返回预计算向量拷贝（常量不读 scope/this），
     * 跳过逐轴 clearTempVariables+dispatch 链；setThis 调用保留以维持 this 残态语义。
     */
    private static Vector3f evalSingleSide(MolangScope scope, BrBoneKeyFrameDefinition frame, boolean pre,
                                           float thisX, float thisY, float thisZ) {
        MolangValue3 value = BrBoneKeyFrame.getValue(frame, pre);
        if (value.allAxesConstant()) {
            scope.setThis(thisX);
            scope.setThis(thisY);
            scope.setThis(thisZ);
            return new Vector3f(value.constantAxis(0), value.constantAxis(1), value.constantAxis(2));
        }
        return value.evalWithThis(scope, thisX, thisY, thisZ);
    }

    /** 旧路径：逐次 floorEntry/higherEntry/lowerEntry 查找 + 逐轴 molang 求值。 */
    @Nullable
    private static Vector3f lerpLegacy(MolangScope scope,
                                       ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames,
                                       float currentTick,
                                       float thisX, float thisY, float thisZ) {
        BrBoneKeyFrameDefinition before = frames.floorEntry(currentTick);
        BrBoneKeyFrameDefinition after = frames.higherEntry(currentTick);

        if (before != null && after != null) {
            var weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(before.timestamp());
                var afterPlus = frames.higherEntry(after.timestamp());

                if (beforePlus == null || afterPlus == null) {
                    return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
                }
                return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight, thisX, thisY, thisZ);
            }
        } else if (before != null) {
            return BrBoneKeyFrame.getValue(before, before.timestamp() >= currentTick).evalWithThis(scope, thisX, thisY, thisZ);
        } else if (after != null) {
            return BrBoneKeyFrame.getValue(after, after.timestamp() >= currentTick).evalWithThis(scope, thisX, thisY, thisZ);
        }

        return null;
    }
}
