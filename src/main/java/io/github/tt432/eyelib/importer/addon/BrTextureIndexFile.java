package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrTextureIndexFile(
        BedrockResourceValue root
) {
    public static final Codec<BrTextureIndexFile> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ImporterCodecUtil.BEDROCK_RESOURCE_VALUE_CODEC.fieldOf("root").forGetter(BrTextureIndexFile::root)
    ).apply(ins, BrTextureIndexFile::new));
}
