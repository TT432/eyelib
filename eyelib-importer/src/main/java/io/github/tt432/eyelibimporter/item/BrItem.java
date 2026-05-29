package io.github.tt432.eyelibimporter.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;
import org.jspecify.annotations.NullMarked;

/** Bedrock 物品定义的数据结构。
 * @author TT432 */
@NullMarked
public record BrItem(
        String formatVersion,
        String identifier,
        BedrockResourceValue.ObjectValue components
) {
    private static final Codec<BedrockResourceValue.ObjectValue> OBJECT_VALUE_CODEC = ImporterCodecUtil.OBJECT_VALUE_CODEC;

    public static final Codec<BrItem> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrItem::formatVersion),
            RecordCodecBuilder.<BrItem>create(inner -> inner.group(
                    Codec.STRING.fieldOf("identifier").forGetter(BrItem::identifier),
                    OBJECT_VALUE_CODEC.fieldOf("components").forGetter(BrItem::components)
            ).apply(inner, BrItem::fromDescriptionAndComponents))
                    .fieldOf("minecraft:item").forGetter(o -> o)
    ).apply(ins, BrItem::fromFormatAndItem));

    private static BrItem fromDescriptionAndComponents(String identifier, BedrockResourceValue.ObjectValue components) {
        return new BrItem("", identifier, components);
    }

    private static BrItem fromFormatAndItem(String formatVersion, BrItem item) {
        return new BrItem(formatVersion, item.identifier(), item.components());
    }
}
