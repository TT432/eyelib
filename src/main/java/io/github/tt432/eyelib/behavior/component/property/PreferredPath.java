package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

import java.util.List;

/**
 * minecraft:preferred_path
 *
 * @param max_fall_blocks      默认 3
 * @param jump_cost            默认 0
 * @param default_block_cost   默认 0.0f
 * @param preferred_path_blocks 路径块列表
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record PreferredPath(
        int max_fall_blocks,
        int jump_cost,
        float default_block_cost,
        List<PathBlock> preferred_path_blocks
) implements Component {
    public static final Codec<PreferredPath> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("max_fall_blocks", 3).forGetter(PreferredPath::max_fall_blocks),
            Codec.INT.optionalFieldOf("jump_cost", 0).forGetter(PreferredPath::jump_cost),
            Codec.FLOAT.optionalFieldOf("default_block_cost", 0.0f).forGetter(PreferredPath::default_block_cost),
            PathBlock.CODEC.listOf().optionalFieldOf("preferred_path_blocks", List.of()).forGetter(PreferredPath::preferred_path_blocks)
    ).apply(ins, PreferredPath::new));

    @Override
    public String id() {
        return "preferred_path";
    }

    public record PathBlock(
            List<String> blocks,
            float cost
    ) {
        public static final Codec<PathBlock> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.listOf().fieldOf("blocks").forGetter(PathBlock::blocks),
                Codec.FLOAT.optionalFieldOf("cost", 0.0f).forGetter(PathBlock::cost)
        ).apply(ins, PathBlock::new));
    }
}
