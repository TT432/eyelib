package io.github.tt432.eyelib.importer.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * bbmodel 内嵌动画（Blockbench bedrock 模式）。坐标为 Blockbench 编辑器空间，
 * 转 bedrock 动画 schema 见 {@link BbModelAnimations}。
 * uuid、选中态、贝塞尔曲线柄等编辑器字段不解析。
 *
 * @author TT432
 */
public record BbModelAnimation(
        String name,
        String loop,
        float length,
        boolean override,
        @SerializedName("anim_time_update")
        String animTimeUpdate,
        @SerializedName("blend_weight")
        String blendWeight,
        @SerializedName("start_delay")
        String startDelay,
        @SerializedName("loop_delay")
        String loopDelay,
        Map<String, Animator> animators
) {
    public static final Codec<BbModelAnimation> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(BbModelAnimation::name),
            Codec.STRING.optionalFieldOf("loop", "once").forGetter(BbModelAnimation::loop),
            Codec.FLOAT.optionalFieldOf("length", 0F).forGetter(BbModelAnimation::length),
            Codec.BOOL.optionalFieldOf("override", false).forGetter(BbModelAnimation::override),
            Codec.STRING.optionalFieldOf("anim_time_update", "").forGetter(BbModelAnimation::animTimeUpdate),
            Codec.STRING.optionalFieldOf("blend_weight", "").forGetter(BbModelAnimation::blendWeight),
            Codec.STRING.optionalFieldOf("start_delay", "").forGetter(BbModelAnimation::startDelay),
            Codec.STRING.optionalFieldOf("loop_delay", "").forGetter(BbModelAnimation::loopDelay),
            Codec.unboundedMap(Codec.STRING, Animator.CODEC).optionalFieldOf("animators", Map.of()).forGetter(BbModelAnimation::animators)
    ).apply(ins, BbModelAnimation::new));

    /**
     * 骨骼动画器；map key 为骨骼 group uuid。
     */
    public record Animator(
            String name,
            List<Keyframe> keyframes
    ) {
        public static final Codec<Animator> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("name").forGetter(Animator::name),
                Keyframe.CODEC.listOf().optionalFieldOf("keyframes", List.of()).forGetter(Animator::keyframes)
        ).apply(ins, Animator::new));
    }

    public record Keyframe(
            String channel,
            float time,
            String interpolation,
            @SerializedName("data_points")
            List<DataPoint> dataPoints
    ) {
        public static final Codec<Keyframe> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("channel").forGetter(Keyframe::channel),
                Codec.FLOAT.optionalFieldOf("time", 0F).forGetter(Keyframe::time),
                Codec.STRING.optionalFieldOf("interpolation", "linear").forGetter(Keyframe::interpolation),
                DataPoint.CODEC.listOf().optionalFieldOf("data_points", List.of()).forGetter(Keyframe::dataPoints)
        ).apply(ins, Keyframe::new));
    }

    /**
     * 关键帧数据点。轴向值数字与 molang 表达式字符串均可；此处只保留原始文本，
     * 编译推迟到 {@link BbModelAnimations} 转换时——bbmodel 是美术资产，可能带
     * 本库绑定不了的表达式，解析期编译会让无关消费方（模型合并）一起崩溃。
     */
    public record DataPoint(
            String x,
            String y,
            String z
    ) {
        private static final Codec<String> SCALAR_STRING = Codec.either(Codec.STRING, Codec.either(Codec.FLOAT, Codec.BOOL))
                .xmap(either -> either.map(Function.identity(), scalar -> scalar.map(Object::toString, value -> value ? "1" : "0")), Either::left);

        public static final Codec<DataPoint> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                SCALAR_STRING.optionalFieldOf("x", "0").forGetter(DataPoint::x),
                SCALAR_STRING.optionalFieldOf("y", "0").forGetter(DataPoint::y),
                SCALAR_STRING.optionalFieldOf("z", "0").forGetter(DataPoint::z)
        ).apply(ins, DataPoint::new));
    }
}
