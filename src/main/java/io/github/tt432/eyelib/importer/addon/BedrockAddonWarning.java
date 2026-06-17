package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** @author TT432 */
@NullMarked
public record BedrockAddonWarning(
        BedrockAddonWarningSeverity severity,
        BedrockAddonWarningCode code,
        String packSource,
        @Nullable String relativePath,
        String message
) {
    public static final Codec<BedrockAddonWarning> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BedrockAddonWarningSeverity.CODEC.fieldOf("severity").forGetter(BedrockAddonWarning::severity),
            BedrockAddonWarningCode.CODEC.fieldOf("code").forGetter(BedrockAddonWarning::code),
            Codec.STRING.fieldOf("pack_source").forGetter(BedrockAddonWarning::packSource),
            Codec.STRING.optionalFieldOf("relative_path", null).forGetter(BedrockAddonWarning::relativePath),
            Codec.STRING.fieldOf("message").forGetter(BedrockAddonWarning::message)
    ).apply(ins, BedrockAddonWarning::new));
}
