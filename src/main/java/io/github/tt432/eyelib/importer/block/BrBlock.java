package io.github.tt432.eyelib.importer.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;
import org.jspecify.annotations.NullMarked;

/**
 * Bedrock 方块定义的数据结构。<br>
 * 对应 JSON 格式：
 * <pre>
 * {
 *   "format_version": "1.20.0",
 *   "minecraft:block": {
 *     "description": { "identifier": "my_mod:custom_block" },
 *     "components": { ... }
 *   }
 * }
 * </pre>
 * @author TT432
 */
@NullMarked
public record BrBlock(
        String formatVersion,
        String identifier,
        BedrockResourceValue.ObjectValue components
) {
    private static final Codec<BedrockResourceValue.ObjectValue> OBJECT_VALUE_CODEC = ImporterCodecUtil.OBJECT_VALUE_CODEC;

    /**
     * minecraft:block.description 内部结构。<br>
     * 当前仅含 identifier，后续可按需扩展。
     */
    private record Description(String identifier) {
        static final Codec<Description> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("identifier").forGetter(Description::identifier)
        ).apply(ins, Description::new));
    }

    /**
     * codec 解码规则：
     * <ul>
     *   <li>顶层 {@code format_version}</li>
     *   <li>{@code minecraft:block} 内嵌 {@code description} 和 {@code components}</li>
     * </ul>
     */
    public static final Codec<BrBlock> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrBlock::formatVersion),
            RecordCodecBuilder.<BrBlock>create(inner -> inner.group(
                    Description.CODEC.fieldOf("description")
                            .forGetter(o -> new Description(o.identifier())),
                    OBJECT_VALUE_CODEC.fieldOf("components").forGetter(BrBlock::components)
            ).apply(inner, (desc, comps) -> new BrBlock("", desc.identifier(), comps)))
                    .fieldOf("minecraft:block").forGetter(o -> o)
    ).apply(ins, (fmtVersion, block) ->
            new BrBlock(fmtVersion, block.identifier(), block.components())));
}
