package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public enum BedrockAddonWarningSeverity {
    INFO,
    WARNING,
    ERROR;

    public static final Codec<BedrockAddonWarningSeverity> CODEC = Codec.STRING.xmap(
            BedrockAddonWarningSeverity::valueOf,
            BedrockAddonWarningSeverity::name
    );
}
