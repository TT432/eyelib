package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrTextureMetadataFile(
        BedrockResourceValue.ObjectValue root
) {
    public static final Codec<BrTextureMetadataFile> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ImporterCodecUtil.OBJECT_VALUE_CODEC.fieldOf("root").forGetter(BrTextureMetadataFile::root)
    ).apply(ins, BrTextureMetadataFile::new));
}
