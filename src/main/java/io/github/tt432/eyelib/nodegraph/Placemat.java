package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * 画布分组（注释框）：标题 + 颜色 + 成员节点。包围盒由编辑器按成员自动计算，不持久化。
 *
 * @param uid     分组 uid
 * @param title   标题
 * @param color   颜色（#RRGGBB 或 #AARRGGBB）
 * @param nodeUids 成员节点 uid 列表
 */
public record Placemat(
        String uid,
        String title,
        String color,
        List<String> nodeUids
) {
    public static final Codec<Placemat> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("uid").forGetter(Placemat::uid),
            Codec.STRING.optionalFieldOf("title", "").forGetter(Placemat::title),
            Codec.STRING.optionalFieldOf("color", "#808080").forGetter(Placemat::color),
            Codec.STRING.listOf().optionalFieldOf("nodes", List.of()).forGetter(Placemat::nodeUids)
    ).apply(ins, Placemat::new));
}
