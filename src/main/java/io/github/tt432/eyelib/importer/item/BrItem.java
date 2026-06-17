package io.github.tt432.eyelib.importer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Bedrock 物品定义的数据结构。
 *
 * @author TT432
 */
public record BrItem(
        String formatVersion,
        Description description,
        BedrockResourceValue.ObjectValue components
) {
    private static final Codec<BedrockResourceValue.ObjectValue> OBJECT_VALUE_CODEC = ImporterCodecUtil.OBJECT_VALUE_CODEC;

    /**
     * 物品描述信息，包含 identifier 和可选的 menu_category。
     */
    public record Description(String identifier, @Nullable MenuCategory menuCategory) {
        public static final Codec<Description> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("identifier").forGetter(Description::identifier),
                MenuCategory.CODEC.optionalFieldOf("menu_category")
                        .forGetter(d -> Optional.ofNullable(d.menuCategory))
        ).apply(ins, (id, mc) -> new Description(id, mc.orElse(null))));
    }

    /**
     * 创造模式物品栏分类信息。
     */
    public record MenuCategory(String category, @Nullable String group) {
        public static final Codec<MenuCategory> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("category").forGetter(MenuCategory::category),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(mc -> Optional.ofNullable(mc.group))
        ).apply(ins, (cat, grp) -> new MenuCategory(cat, grp.orElse(null))));
    }

    public static final Codec<BrItem> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrItem::formatVersion),
            Description.CODEC.fieldOf("description").forGetter(BrItem::description),
            OBJECT_VALUE_CODEC.fieldOf("components").forGetter(BrItem::components)
    ).apply(ins, BrItem::new));
}
